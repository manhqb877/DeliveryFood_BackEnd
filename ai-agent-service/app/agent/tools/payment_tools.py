"""
Payment tools: initiate_payment.
"""

from typing import Any, Dict

from app.clients.payment_service_client import PaymentServiceClient
from app.config import settings
from app.session.models import SessionState

# ─── JSON Schema ───────────────────────────────────────────────────────────────

INITIATE_PAYMENT_SCHEMA = {
    "name": "initiate_payment",
    "description": "Khởi tạo link/QR thanh toán cho một đơn đã tạo. Phương thức có thể là 'sepay' (quét mã VietQR chuyển khoản MBBank), 'cod' (tiền mặt), 'vnpay', 'momo'.",
    "input_schema": {
        "type": "object",
        "properties": {
            "order_id": {"type": "string"},
            "method": {
                "type": "string",
                "enum": ["sepay", "vietqr", "cod", "vnpay", "momo", "zalopay"],
                "description": "Phương thức thanh toán. Mặc định ưu tiên 'sepay' (VietQR) hoặc 'cod'.",
            },
        },
        "required": ["order_id", "method"],
    },
}

# ─── Handler ───────────────────────────────────────────────────────────────────


async def handle_initiate_payment(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    payment_client = PaymentServiceClient(base_url=settings.payment_service_url, jwt=state.user_jwt)
    oid = tool_input["order_id"]
    order_info = state.last_order_info or {}
    total_amt = float(order_info.get("total_amount") or 0.0)
    order_code = order_info.get("order_code") or f"ORD{oid}"

    res = await payment_client.initiate_payment(
        order_id=oid,
        method=tool_input["method"],
        idempotency_key=state.idempotency_key,
        order_code=order_code,
        amount=total_amt,
        user_id=state.user_id,
    )

    if res.get("payment_qr"):
        state.last_payment_qr = res["payment_qr"]
        if state.last_order_info:
            state.last_order_info["payment_qr"] = res["payment_qr"]

    return res
