"""
test_guardrails.py — Bắt buộc pass trước khi qua Phase 2.

Test cases:
1. Agent KHÔNG được gọi create_order khi turn gần nhất KHÔNG chứa xác nhận.
2. Agent ĐƯỢC gọi create_order sau khi người dùng xác nhận rõ.
3. Người dùng đổi ý (huỷ giỏ) → state.order_confirmed bị reset → không thể tự đặt.
4. idempotency_key KHÔNG lấy từ LLM, luôn từ SessionState.
5. Giỏ hàng trống → create_order bị block.
"""

import uuid
import pytest

from app.agent.guardrails import (
    GuardrailViolation,
    detect_user_confirmation,
    inject_system_fields,
    validate_tool_call,
)
from app.session.models import CartItem, CartSnapshot, SessionState


# ─── Fixture ──────────────────────────────────────────────────────────────────

def make_state(confirmed: bool = False, with_items: bool = True) -> SessionState:
    cart = CartSnapshot()
    if with_items:
        cart.items.append(
            CartItem(
                item_id="item-ct-001",
                quantity=2,
                unit_price=45000,
                item_name="Cơm Tấm Sườn Bì Chả",
            )
        )
    return SessionState(
        session_id=str(uuid.uuid4()),
        area_code="KCN-A1",
        user_type="customer",
        cart=cart,
        order_confirmed=confirmed,
    )


# ─── detect_user_confirmation ─────────────────────────────────────────────────

@pytest.mark.parametrize("text,expected", [
    ("xác nhận", True),
    ("Đặt ngay đi", True),
    ("OK thôi", True),
    ("đồng ý", True),
    ("chờ tôi nghĩ thêm", False),
    ("hủy đi", False),
    ("thêm 1 phần nữa", False),
    ("", False),
])
def test_detect_user_confirmation(text, expected):
    assert detect_user_confirmation(text) == expected


# ─── validate_tool_call ───────────────────────────────────────────────────────

def test_create_order_blocked_when_not_confirmed():
    """Không cho tạo đơn nếu chưa xác nhận."""
    state = make_state(confirmed=False, with_items=True)
    with pytest.raises(GuardrailViolation) as exc_info:
        validate_tool_call(
            "create_order",
            {"delivery_address": {"raw_text": "Toà A, phòng 101"}},
            state,
        )
    assert "chưa xác nhận" in str(exc_info.value).lower() or "guardrail" in str(exc_info.value).lower()


def test_create_order_allowed_when_confirmed():
    """Cho tạo đơn khi đã xác nhận và giỏ có hàng."""
    state = make_state(confirmed=True, with_items=True)
    # Không raise
    validate_tool_call(
        "create_order",
        {"delivery_address": {"raw_text": "Toà A, phòng 101"}},
        state,
    )


def test_create_order_blocked_when_cart_empty():
    """Giỏ trống → không cho tạo đơn dù đã confirmed."""
    state = make_state(confirmed=True, with_items=False)
    with pytest.raises(GuardrailViolation):
        validate_tool_call(
            "create_order",
            {"delivery_address": {"raw_text": "Toà A, phòng 101"}},
            state,
        )


def test_order_confirmed_reset_after_cart_change():
    """
    Sau khi người dùng đã confirm nhưng thay đổi giỏ hàng (rotate_idempotency_key),
    order_confirmed phải bị reset → create_order bị block lại.
    """
    state = make_state(confirmed=True, with_items=True)
    assert state.order_confirmed is True

    # Giả lập người dùng thay đổi giỏ hàng
    state.rotate_idempotency_key()
    assert state.order_confirmed is False

    with pytest.raises(GuardrailViolation):
        validate_tool_call(
            "create_order",
            {"delivery_address": {"raw_text": "Toà A, phòng 101"}},
            state,
        )


def test_initiate_payment_blocked_without_order_id():
    """initiate_payment cần order_id."""
    state = make_state(confirmed=True)
    with pytest.raises(GuardrailViolation):
        validate_tool_call("initiate_payment", {}, state)


def test_initiate_payment_allowed_with_order_id():
    state = make_state(confirmed=True)
    # Không raise
    validate_tool_call("initiate_payment", {"order_id": "ORD-123", "method": "cod"}, state)


# ─── inject_system_fields ─────────────────────────────────────────────────────

def test_idempotency_key_always_from_session():
    """LLM không được tự bịa idempotency_key — phải bị override bởi session value."""
    state = make_state(confirmed=True, with_items=True)
    fake_key_from_llm = "fake-llm-generated-key-should-be-ignored"
    tool_input = {
        "session_id": "should-be-overridden",
        "delivery_address": {"raw_text": "Toà A, phòng 101"},
        "idempotency_key": fake_key_from_llm,
    }
    result = inject_system_fields("create_order", tool_input, state)

    assert result["idempotency_key"] == state.idempotency_key
    assert result["idempotency_key"] != fake_key_from_llm
    assert result["session_id"] == state.session_id


def test_area_code_injected_for_search_shops():
    state = make_state()
    tool_input = {"keyword": "phở", "area_code": "FAKE-FROM-LLM"}
    result = inject_system_fields("search_shops", tool_input, state)
    assert result["area_code"] == state.area_code


def test_user_type_injected_for_apply_promotion():
    state = make_state()
    state.user_type = "customer"
    tool_input = {"promo_code": "RESIDENT20", "user_type": "guest"}  # LLM bịa user_type=guest
    result = inject_system_fields("apply_promotion", tool_input, state)
    assert result["user_type"] == "customer"  # phải là từ session


# ─── Non-financial tools không cần guardrail ──────────────────────────────────

def test_search_shops_no_guardrail_needed():
    state = make_state(confirmed=False)
    # Không raise với tool không tài chính
    validate_tool_call("search_shops", {"area_code": "KCN-A1", "keyword": "phở"}, state)


def test_get_menu_no_guardrail_needed():
    state = make_state(confirmed=False)
    validate_tool_call("get_menu", {"shop_id": "shop-001"}, state)
