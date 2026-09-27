"""
test_tools.py — Unit test cho các tool handler (dùng mock clients).
"""

import uuid
import pytest
import pytest_asyncio

from app.session.models import CartItem, CartSnapshot, SessionState


def make_state(**kwargs) -> SessionState:
    return SessionState(
        session_id=str(uuid.uuid4()),
        area_code="KCN-A1",
        user_type="customer",
        **kwargs,
    )


@pytest.mark.asyncio
async def test_handle_view_cart_empty():
    from app.agent.tools.cart_tools import handle_view_cart
    state = make_state()
    result = await handle_view_cart({"session_id": state.session_id}, state)
    assert result["items"] == []
    assert result["total"] == 0


@pytest.mark.asyncio
async def test_handle_add_to_cart_and_view():
    from app.agent.tools.cart_tools import handle_add_to_cart, handle_view_cart
    state = make_state()

    add_result = await handle_add_to_cart(
        {
            "session_id": state.session_id,
            "item_id": "item-ct-001",
            "quantity": 2,
        },
        state,
    )
    assert add_result["success"] is True
    assert len(state.cart.items) == 1

    view_result = await handle_view_cart({"session_id": state.session_id}, state)
    assert len(view_result["items"]) == 1
    assert view_result["items"][0]["quantity"] == 2


@pytest.mark.asyncio
async def test_handle_add_to_cart_resets_confirmed():
    from app.agent.tools.cart_tools import handle_add_to_cart
    state = make_state()
    state.order_confirmed = True

    await handle_add_to_cart(
        {"session_id": state.session_id, "item_id": "item-ct-001", "quantity": 1},
        state,
    )
    assert state.order_confirmed is False


@pytest.mark.asyncio
async def test_handle_remove_from_cart():
    from app.agent.tools.cart_tools import handle_add_to_cart, handle_remove_from_cart
    state = make_state()

    await handle_add_to_cart(
        {"session_id": state.session_id, "item_id": "item-ct-001", "quantity": 1},
        state,
    )
    cart_item_id = state.cart.items[0].cart_item_id

    result = await handle_remove_from_cart(
        {"session_id": state.session_id, "cart_item_id": cart_item_id},
        state,
    )
    assert result["success"] is True
    assert len(state.cart.items) == 0


@pytest.mark.asyncio
async def test_handle_update_cart_item_zero_removes():
    from app.agent.tools.cart_tools import handle_add_to_cart, handle_update_cart_item
    state = make_state()

    await handle_add_to_cart(
        {"session_id": state.session_id, "item_id": "item-ct-001", "quantity": 3},
        state,
    )
    cart_item_id = state.cart.items[0].cart_item_id

    result = await handle_update_cart_item(
        {"session_id": state.session_id, "cart_item_id": cart_item_id, "quantity": 0},
        state,
    )
    assert result["success"] is True
    assert len(state.cart.items) == 0


@pytest.mark.asyncio
async def test_handle_search_shops_returns_list():
    from app.agent.tools.search_tools import handle_search_shops
    state = make_state()
    result = await handle_search_shops({"area_code": "KCN-A1"}, state)
    assert "shops" in result
    assert isinstance(result["shops"], list)


@pytest.mark.asyncio
async def test_handle_search_shops_filter_open():
    from app.agent.tools.search_tools import handle_search_shops
    state = make_state()
    result = await handle_search_shops({"area_code": "KCN-A1", "is_open_now": True}, state)
    for shop in result["shops"]:
        assert shop["is_open"] is True


@pytest.mark.asyncio
async def test_handle_get_menu():
    from app.agent.tools.search_tools import handle_get_menu
    state = make_state()
    result = await handle_get_menu({"shop_id": "shop-001"}, state)
    assert "items" in result
    assert len(result["items"]) > 0
    # Mỗi item phải có topping list với topping_id
    for item in result["items"]:
        for topping in item.get("toppings", []):
            assert "topping_id" in topping


@pytest.mark.asyncio
async def test_handle_get_order_status():
    from app.agent.tools.order_tools import handle_get_order_status
    state = make_state()
    result = await handle_get_order_status({"order_id": "ORD-TEST123"}, state)
    assert "status" in result
    assert "order_id" in result


@pytest.mark.asyncio
async def test_handle_create_order_empty_cart_fails():
    from app.agent.tools.order_tools import handle_create_order
    state = make_state()
    state.order_confirmed = True  # bypass guardrail, test handler trực tiếp
    result = await handle_create_order(
        {"session_id": state.session_id, "delivery_address": {"raw_text": "Toà A"}, "idempotency_key": state.idempotency_key},
        state,
    )
    assert result.get("success") is False


@pytest.mark.asyncio
async def test_handle_create_order_success_clears_cart():
    from app.agent.tools.cart_tools import handle_add_to_cart
    from app.agent.tools.order_tools import handle_create_order
    state = make_state()
    state.order_confirmed = True

    await handle_add_to_cart(
        {"session_id": state.session_id, "item_id": "item-ct-001", "quantity": 1},
        state,
    )
    assert len(state.cart.items) == 1

    result = await handle_create_order(
        {
            "session_id": state.session_id,
            "delivery_address": {"raw_text": "Toà A, phòng 101"},
            "idempotency_key": state.idempotency_key,
        },
        state,
    )
    assert result.get("order_id")
    assert len(state.cart.items) == 0  # giỏ phải được xoá sau khi đặt


@pytest.mark.asyncio
async def test_handle_initiate_payment_cod():
    from app.agent.tools.payment_tools import handle_initiate_payment
    state = make_state()
    result = await handle_initiate_payment(
        {"order_id": "ORD-MOCK001", "method": "cod"},
        state,
    )
    assert result["method"] == "cod"
    assert "instruction" in result
