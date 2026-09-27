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
    "description": "Khởi tạo link/QR thanh toán cho một đơn đã tạo.",
    "input_schema": {
        "type": "object",
        "properties": {
            "order_id": {"type": "string"},
            "method": {"type": "string", "enum": ["cod", "vnpay", "momo", "zalopay"]},
        },
        "required": ["order_id", "method"],
    },
}

# ─── Handler ───────────────────────────────────────────────────────────────────


async def handle_initiate_payment(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    payment_client = PaymentServiceClient(base_url=settings.payment_service_url, jwt=state.user_jwt)
    return await payment_client.initiate_payment(
        order_id=tool_input["order_id"],
        method=tool_input["method"],
        idempotency_key=state.idempotency_key,
    )
