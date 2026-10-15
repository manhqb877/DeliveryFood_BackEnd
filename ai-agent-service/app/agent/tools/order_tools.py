"""
Order tools: create_order, get_order_status.
create_order — CHỈ được gọi sau khi guardrail xác nhận state.order_confirmed == True.
Hỗ trợ cả 2 hình thức: COD (tiền mặt khi nhận hàng) và SEPAY/VIETQR (chuyển khoản QR ngân hàng).
"""

from typing import Any, Dict
import logging

from app.clients.order_service_client import OrderServiceClient
from app.clients.payment_service_client import PaymentServiceClient
from app.config import settings
from app.session.models import CartSnapshot, SessionState

logger = logging.getLogger(__name__)

# ─── JSON Schemas ──────────────────────────────────────────────────────────────

CREATE_ORDER_SCHEMA = {
    "name": "create_order",
    "description": "Tạo đơn hàng chính thức từ giỏ hàng hiện tại (bao gồm cả mã khuyến mãi đã áp nếu có). Chọn payment_method là 'COD' (tiền mặt) hoặc 'SEPAY'/'VIETQR' (chuyển khoản QR ngân hàng). CHỈ gọi sau khi người dùng đã đồng ý.",
    "input_schema": {
        "type": "object",
        "properties": {
            "session_id": {"type": "string", "description": "🔒 system-injected"},
            "delivery_address": {
                "description": "Địa điểm nhận hàng (chuỗi địa chỉ hoặc object {raw_text: ...})",
            },
            "payment_method": {
                "type": "string",
                "enum": ["COD", "SEPAY", "VIETQR", "ONLINE"],
                "description": "Phương thức thanh toán: COD (tiền mặt khi nhận hàng) hoặc SEPAY/VIETQR (quét mã QR ngân hàng)",
            },
            "order_note": {"type": "string", "description": "Ghi chú đơn hàng (nếu có)"},
            "idempotency_key": {"type": "string", "description": "🔒 system-injected — UUID sinh bởi agent"},
        },
        "required": ["session_id", "delivery_address", "idempotency_key"],
    },
}

GET_ORDER_STATUS_SCHEMA = {
    "name": "get_order_status",
    "description": "Tra cứu trạng thái hiện tại của một đơn hàng (đã đặt/đang nấu/đang giao/đã giao...).",
    "input_schema": {
        "type": "object",
        "properties": {"order_id": {"type": "string"}},
        "required": ["order_id"],
    },
}

# ─── Handlers ─────────────────────────────────────────────────────────────────


async def handle_create_order(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    """
    Tạo đơn hàng từ giỏ hàng.
    Hỗ trợ thanh toán COD và VietQR SePay.
    """
    if not state.cart.items:
        return {"success": False, "error": "Giỏ hàng trống, không thể tạo đơn"}

    order_client = OrderServiceClient(base_url=settings.order_service_url, jwt=state.user_jwt)
    payment_client = PaymentServiceClient(base_url=settings.payment_service_url, jwt=state.user_jwt)

    items_payload = [
        {
            "item_id": i.item_id,
            "item_name": i.item_name,
            "quantity": i.quantity,
            "topping_ids": i.topping_ids,
            "note": i.note,
            "unit_price": i.unit_price,
        }
        for i in state.cart.items
    ]

    # Lấy shop_id từ cart hoặc item đầu tiên
    shop_id_int = None
    if state.cart.shop_id and str(state.cart.shop_id).isdigit():
        shop_id_int = int(state.cart.shop_id)

    delivery_addr = tool_input.get("delivery_address") or "Địa chỉ giao hàng"
    raw_pm = str(tool_input.get("payment_method") or "COD").upper()
    is_online_qr = raw_pm in ["SEPAY", "VIETQR", "ONLINE", "QR", "CHUYEN_KHOAN"]
    actual_payment_method = "ONLINE" if is_online_qr else "COD"

    result = await order_client.create_order(
        items=items_payload,
        delivery_address=delivery_addr,
        promo_code=state.cart.promo_code,
        idempotency_key=state.idempotency_key,  # luôn dùng key từ session, không từ LLM
        payment_method=actual_payment_method,
        shop_id=shop_id_int,
        user_id=state.user_id,
        guest_session_id=state.guest_session_id,
        discount_amount=state.cart.discount_amount,
    )

    if result.get("order_id"):
        oid = result["order_id"]
        ocode = result.get("order_code") or f"ORD{oid}"
        total_amt = float(result.get("total_amount") or state.cart.total or 0.0)

        order_info = {
            "order_id": oid,
            "order_code": ocode,
            "total_amount": total_amt,
            "payment_method": "ONLINE" if is_online_qr else "COD",
            "delivery_address": delivery_addr if isinstance(delivery_addr, str) else delivery_addr.get("fullAddress", ""),
            "items_count": len(state.cart.items),
        }
        state.last_order_id = str(oid)
        state.last_order_info = order_info
        state.cart_updated = True

        # Nếu là thanh toán VietQR / SePay, tạo mã QR ngay lập tức
        if is_online_qr:
            try:
                qr_res = await payment_client.generate_sepay_qr(
                    order_id=int(oid) if str(oid).isdigit() else 0,
                    order_code=ocode,
                    amount=total_amt,
                    user_id=state.user_id,
                )
                if qr_res:
                    result["payment_qr"] = qr_res
                    state.last_payment_qr = qr_res
                    order_info["payment_qr"] = qr_res
            except Exception as ex:
                logger.error(f"[OrderTools] Error generating SePay QR: {ex}")

        # Xoá giỏ hàng sau khi đặt thành công
        state.cart = CartSnapshot()
        state.order_confirmed = False

    return result


async def handle_get_order_status(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    order_client = OrderServiceClient(base_url=settings.order_service_url, jwt=state.user_jwt)
    return await order_client.get_order_status(order_id=tool_input["order_id"])
