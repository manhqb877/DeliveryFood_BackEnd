"""
Tool registry: ánh xạ tên tool → (schema JSON, hàm thực thi Python).
Import tất cả tools từ các module con, expose qua TOOL_REGISTRY và TOOL_SCHEMAS.
"""

from app.agent.tools.search_tools import (
    SEARCH_SHOPS_SCHEMA,
    SEARCH_ITEMS_SCHEMA,
    GET_MENU_SCHEMA,
    handle_search_shops,
    handle_search_items,
    handle_get_menu,
)
from app.agent.tools.cart_tools import (
    ADD_TO_CART_SCHEMA,
    UPDATE_CART_ITEM_SCHEMA,
    REMOVE_FROM_CART_SCHEMA,
    VIEW_CART_SCHEMA,
    APPLY_PROMOTION_SCHEMA,
    GET_PROMOTIONS_SCHEMA,
    handle_add_to_cart,
    handle_update_cart_item,
    handle_remove_from_cart,
    handle_view_cart,
    handle_apply_promotion,
    handle_get_promotions,
)
from app.agent.tools.order_tools import (
    CREATE_ORDER_SCHEMA,
    GET_ORDER_STATUS_SCHEMA,
    handle_create_order,
    handle_get_order_status,
)
from app.agent.tools.payment_tools import (
    INITIATE_PAYMENT_SCHEMA,
    handle_initiate_payment,
)
from app.agent.tools.user_tools import (
    GET_USER_ADDRESSES_SCHEMA,
    handle_get_user_addresses,
)

# Danh sách schema gửi cho LLM
TOOL_SCHEMAS = [
    SEARCH_SHOPS_SCHEMA,
    SEARCH_ITEMS_SCHEMA,
    GET_MENU_SCHEMA,
    GET_USER_ADDRESSES_SCHEMA,
    ADD_TO_CART_SCHEMA,
    UPDATE_CART_ITEM_SCHEMA,
    REMOVE_FROM_CART_SCHEMA,
    VIEW_CART_SCHEMA,
    APPLY_PROMOTION_SCHEMA,
    GET_PROMOTIONS_SCHEMA,
    CREATE_ORDER_SCHEMA,
    GET_ORDER_STATUS_SCHEMA,
    INITIATE_PAYMENT_SCHEMA,
]

# Ánh xạ tên tool → handler coroutine
TOOL_REGISTRY = {
    "search_shops": handle_search_shops,
    "search_items": handle_search_items,
    "get_menu": handle_get_menu,
    "get_user_addresses": handle_get_user_addresses,
    "add_to_cart": handle_add_to_cart,
    "update_cart_item": handle_update_cart_item,
    "remove_from_cart": handle_remove_from_cart,
    "view_cart": handle_view_cart,
    "apply_promotion": handle_apply_promotion,
    "get_promotions": handle_get_promotions,
    "create_order": handle_create_order,
    "get_order_status": handle_get_order_status,
    "initiate_payment": handle_initiate_payment,
}
