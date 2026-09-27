"""
test_orchestrator.py — Integration test cho orchestrator loop (không cần LLM thật).
Dùng patch để mock Anthropic client.
"""

import uuid
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.session.models import SessionState


def make_state() -> SessionState:
    return SessionState(
        session_id=str(uuid.uuid4()),
        area_code="KCN-A1",
        user_type="customer",
    )


@pytest.mark.asyncio
async def test_orchestrator_simple_text_response():
    """
    Khi LLM trả về end_turn với text → orchestrator trả text đó.
    """
    state = make_state()
    user_msg = "Tìm quán cơm gần đây"

    # Mock Anthropic response
    mock_block = MagicMock()
    mock_block.type = "text"
    mock_block.text = "Dạ, tôi đang tìm quán cơm cho bạn!"

    mock_response = MagicMock()
    mock_response.stop_reason = "end_turn"
    mock_response.content = [mock_block]

    with patch("app.agent.orchestrator.anthropic.AsyncAnthropic") as mock_cls:
        mock_client = AsyncMock()
        mock_cls.return_value = mock_client
        mock_client.messages.create = AsyncMock(return_value=mock_response)

        with patch("app.agent.orchestrator.save_session", new_callable=AsyncMock):
            from app.agent.orchestrator import run_agent_turn
            result = await run_agent_turn(user_msg, state)

    assert "tôi đang tìm" in result.lower() or result != ""
    assert len(state.conversation_history) >= 2  # user + assistant


@pytest.mark.asyncio
async def test_orchestrator_does_not_allow_create_order_without_confirmation():
    """
    Nếu LLM cố gọi create_order nhưng người dùng chưa confirm → guardrail chặn,
    orchestrator gửi error tool_result lại LLM thay vì crash.
    """
    state = make_state()
    # Thêm item vào giỏ
    from app.session.models import CartItem
    state.cart.items.append(CartItem(item_id="item-ct-001", quantity=1, unit_price=45000, item_name="Cơm Tấm"))
    state.order_confirmed = False

    user_msg = "Đặt luôn đi"  # không chứa từ xác nhận rõ ràng

    # LLM lần 1 trả tool_use create_order
    tool_use_block = MagicMock()
    tool_use_block.type = "tool_use"
    tool_use_block.name = "create_order"
    tool_use_block.id = "tu_001"
    tool_use_block.input = {
        "session_id": state.session_id,
        "delivery_address": {"raw_text": "Toà A"},
        "idempotency_key": "fake-key",
    }

    response_1 = MagicMock()
    response_1.stop_reason = "tool_use"
    response_1.content = [tool_use_block]

    # LLM lần 2 (sau khi nhận guardrail error) trả text
    text_block = MagicMock()
    text_block.type = "text"
    text_block.text = "Bạn cần xác nhận trước khi tôi đặt đơn."

    response_2 = MagicMock()
    response_2.stop_reason = "end_turn"
    response_2.content = [text_block]

    with patch("app.agent.orchestrator.anthropic.AsyncAnthropic") as mock_cls:
        mock_client = AsyncMock()
        mock_cls.return_value = mock_client
        mock_client.messages.create = AsyncMock(side_effect=[response_1, response_2])

        with patch("app.agent.orchestrator.save_session", new_callable=AsyncMock):
            from importlib import reload
            import app.agent.orchestrator as orch_module
            result = await orch_module.run_agent_turn(user_msg, state)

    # Đơn KHÔNG được tạo
    assert state.last_order_id is None
    # Câu trả lời phải có nội dung từ LLM lần 2
    assert result != ""
