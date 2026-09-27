"""
Search tools: search_shops, get_menu.
Mỗi tool có schema JSON (dùng cho Anthropic function calling) và handler coroutine.
Handler chỉ gọi qua client, không dùng httpx/requests trực tiếp.
"""

from typing import Any, Dict

from app.clients.core_service_client import CoreServiceClient
from app.clients.analytics_service_client import AnalyticsServiceClient
from app.config import settings
from app.session.models import SessionState

# ─── JSON Schemas ──────────────────────────────────────────────────────────────

SEARCH_SHOPS_SCHEMA = {
    "name": "search_shops",
    "description": "Tìm và lọc quán ăn/món ăn trong khu vực hiện tại theo tên, khoảng giá, đánh giá, bestseller, giờ mở cửa.",
    "input_schema": {
        "type": "object",
        "properties": {
            "keyword": {"type": "string", "description": "Từ khoá món/loại món, để trống nếu không có"},
            "price_min": {"type": "integer"},
            "price_max": {"type": "integer"},
            "min_rating": {"type": "number", "description": "Ví dụ 4.0"},
            "bestseller_only": {"type": "boolean", "default": False},
            "is_open_now": {"type": "boolean", "default": True, "description": "Chỉ lấy quán đang mở cửa"},
            "sort_by": {
                "type": "string",
                "enum": ["relevance", "price_asc", "price_desc", "rating_desc", "bestseller"],
                "default": "relevance",
            },
            "area_code": {"type": "string", "description": "🔒 system-injected"},
        },
        "required": ["area_code"],
    },
}

SEARCH_ITEMS_SCHEMA = {
    "name": "search_items",
    "description": "Tìm kiếm các món ăn cụ thể theo tên món hoặc tìm món ngon giá rẻ trong hệ thống.",
    "input_schema": {
        "type": "object",
        "properties": {
            "keyword": {"type": "string", "description": "Tên món hoặc loại món (ví dụ: bún, cơm, trà sữa, gà)"},
            "max_price": {"type": "number", "description": "Mức giá tối đa mong muốn (VNĐ)"},
        },
    },
}

GET_MENU_SCHEMA = {
    "name": "get_menu",
    "description": "Lấy menu chi tiết của một quán, bao gồm danh sách topping/tuỳ chọn kèm theo từng món (topping_id, name, price) để dùng khi gọi add_to_cart.",
    "input_schema": {
        "type": "object",
        "properties": {"shop_id": {"type": "string"}},
        "required": ["shop_id"],
    },
}

# ─── Handlers ─────────────────────────────────────────────────────────────────


async def handle_search_shops(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    """
    orchestrator đã inject area_code vào tool_input trước khi gọi hàm này.
    """
    core = CoreServiceClient(base_url=settings.core_service_url, jwt=state.user_jwt)
    analytics = AnalyticsServiceClient(base_url=settings.analytics_service_url, jwt=state.user_jwt)

    shops = await core.search_shops(
        area_code=tool_input.get("area_code", state.area_code),
        keyword=tool_input.get("keyword", ""),
        price_min=tool_input.get("price_min"),
        price_max=tool_input.get("price_max"),
        min_rating=tool_input.get("min_rating"),
        bestseller_only=tool_input.get("bestseller_only", False),
        is_open_now=tool_input.get("is_open_now", True),
        sort_by=tool_input.get("sort_by", "relevance"),
    )

    if tool_input.get("bestseller_only") or tool_input.get("sort_by") == "bestseller":
        bestsellers = await analytics.get_bestsellers(area_id=state.area_code)
        for shop in shops:
            shop["has_bestseller"] = any(True for b in bestsellers if b.get("shop_id") == shop["shop_id"])

    return {"shops": shops, "count": len(shops)}


async def handle_search_items(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    core = CoreServiceClient(base_url=settings.core_service_url, jwt=state.user_jwt)
    items = await core.search_items(
        keyword=tool_input.get("keyword", ""),
        max_price=tool_input.get("max_price"),
    )
    return {"items": items, "count": len(items)}


async def handle_get_menu(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    core = CoreServiceClient(base_url=settings.core_service_url, jwt=state.user_jwt)
    return await core.get_menu(shop_id=tool_input["shop_id"])

