"""
Order tools: create_order, get_order_status.
create_order — CHỈ được gọi sau khi guardrail xác nhận state.order_confirmed == True.
"""

from typing import Any, Dict

from app.clients.order_service_client import OrderServiceClient
from app.config import settings
from app.session.models import SessionState

# ─── JSON Schemas ──────────────────────────────────────────────────────────────

CREATE_ORDER_SCHEMA = {
    "name": "create_order",
    "description": "Tạo đơn hàng chính thức từ giỏ hàng hiện tại (bao gồm cả mã khuyến mãi đã áp nếu có). CHỈ gọi sau khi người dùng đã xác nhận rõ ràng bằng lời trong lượt gần nhất.",
    "input_schema": {
        "type": "object",
        "properties": {
            "session_id": {"type": "string", "description": "🔒 system-injected"},
            "delivery_address": {
                "description": "Địa điểm nhận hàng (chuỗi địa chỉ hoặc object {raw_text: ...})",
            },
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
    Guardrail đã kiểm tra state.order_confirmed trước khi gọi hàm này (trong orchestrator/guardrails).
    Ở đây chỉ cần validate giỏ không trống.
    """
    if not state.cart.items:
        return {"success": False, "error": "Giỏ hàng trống, không thể tạo đơn"}

    order_client = OrderServiceClient(base_url=settings.order_service_url, jwt=state.user_jwt)

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

    result = await order_client.create_order(
        items=items_payload,
        delivery_address=delivery_addr,
        promo_code=state.cart.promo_code,
        idempotency_key=state.idempotency_key,  # luôn dùng key từ session, không từ LLM
        shop_id=shop_id_int,
        user_id=state.user_id,
        discount_amount=state.cart.discount_amount,
    )

    if result.get("order_id"):
        state.last_order_id = result["order_id"]
        # Xoá giỏ hàng sau khi đặt thành công
        from app.session.models import CartSnapshot
        state.cart = CartSnapshot()
        state.order_confirmed = False

    return result


async def handle_get_order_status(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    order_client = OrderServiceClient(base_url=settings.order_service_url, jwt=state.user_jwt)
    return await order_client.get_order_status(order_id=tool_input["order_id"])
