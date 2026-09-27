"""
User tools: get_user_addresses, get_user_profile.
Cho phép AI Agent tra cứu địa chỉ đã lưu của tài khoản để xác nhận với người dùng.
"""

from typing import Any, Dict
from app.clients.auth_service_client import AuthServiceClient
from app.config import settings
from app.session.models import SessionState

GET_USER_ADDRESSES_SCHEMA = {
    "name": "get_user_addresses",
    "description": "Lấy danh sách các địa chỉ giao hàng đã lưu trong tài khoản của người dùng (nếu đã đăng nhập).",
    "input_schema": {
        "type": "object",
        "properties": {},
    },
}

async def handle_get_user_addresses(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    auth_client = AuthServiceClient(base_url=settings.auth_service_url, jwt=state.user_jwt)
    addresses = await auth_client.get_user_addresses()
    formatted = []
    for a in addresses:
        formatted.append({
            "id": a.get("id"),
            "address": a.get("addressLine") or a.get("fullAddress") or "",
            "is_default": a.get("isDefault", False),
        })
    return {"addresses": formatted, "count": len(formatted)}
