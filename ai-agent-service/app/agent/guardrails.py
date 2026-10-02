"""
Guardrails — validate tool_input trước khi dispatch.

Quy tắc bắt buộc:
1. create_order và initiate_payment chỉ được gọi khi state.order_confirmed == True
   (với create_order) hoặc state.last_order_id tồn tại (với initiate_payment).
2. Các field system-injected (session_id, area_code, idempotency_key, user_type)
   luôn bị ghi đè bởi orchestrator từ SessionState — guardrail không cần validate chúng.
"""

from typing import Any, Dict, Tuple

from app.session.models import SessionState

# Tên các tool tài chính cần có cờ xác nhận
FINANCIAL_TOOLS = {"create_order", "initiate_payment"}

# Từ khoá người dùng dùng để xác nhận (case-insensitive)
CONFIRM_KEYWORDS = [
    "xác nhận", "xác nhân", "đồng ý", "ok", "được", "đặt đi", "đặt thôi",
<<<<<<< HEAD
    "yes", "có", "chắc chắn", "confirm", "đặt ngay", "đặt luôn",
    "thanh toán", "thanh toan", "thanh toán luôn", "thanh toán nhé", "thanh toán đi",
    "chốt", "chốt đơn", "đặt hàng", "đặt đơn", "mua", "mua luôn", "mua ngay",
    "quét qr", "chuyển khoản",
=======
    "yes", "có", "chắc chắn", "confirm", "đặt ngay",
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
]


def detect_user_confirmation(text: str) -> bool:
    """
    Kiểm tra xem lượt nói gần nhất của người dùng có chứa từ xác nhận không.
    Chỉ dùng khi cần set state.order_confirmed = True từ phía orchestrator.
    """
    normalized = text.lower().strip()
    return any(kw in normalized for kw in CONFIRM_KEYWORDS)


class GuardrailViolation(Exception):
    """Raised khi guardrail block một hành động."""
    pass


def validate_tool_call(
    tool_name: str,
    tool_input: Dict[str, Any],
    state: SessionState,
) -> None:
    """
    Raise GuardrailViolation nếu tool_call vi phạm quy tắc.
    orchestrator gọi hàm này trước khi dispatch.
    """
    if tool_name == "create_order":
        if not state.order_confirmed:
            raise GuardrailViolation(
                "Người dùng chưa xác nhận đơn hàng trong lượt hội thoại gần nhất. "
                "Hãy hỏi lại: 'Bạn xác nhận đặt đơn này chứ?' trước khi gọi create_order."
            )
        if not state.cart.items:
            raise GuardrailViolation("Giỏ hàng trống, không thể tạo đơn.")

    if tool_name == "initiate_payment":
        order_id = tool_input.get("order_id")
        if not order_id:
            raise GuardrailViolation("initiate_payment cần order_id hợp lệ.")


def inject_system_fields(
    tool_name: str,
    tool_input: Dict[str, Any],
    state: SessionState,
) -> Dict[str, Any]:
    """
    Ghi đè các field system-injected từ SessionState vào tool_input.
    LLM có thể tự bịa giá trị cho các field này — orchestrator luôn override.
    """
    overrides: Dict[str, Any] = {}

    # Các tool cần session_id
    if "session_id" in tool_input or tool_name in {
        "add_to_cart", "update_cart_item", "remove_from_cart",
        "view_cart", "apply_promotion", "create_order",
    }:
        overrides["session_id"] = state.session_id

    # Tool search cần area_code
    if tool_name == "search_shops":
        overrides["area_code"] = state.area_code

    # apply_promotion cần user_type
    if tool_name == "apply_promotion":
        overrides["user_type"] = state.user_type

    # create_order cần idempotency_key
    if tool_name == "create_order":
        overrides["idempotency_key"] = state.idempotency_key

    return {**tool_input, **overrides}
