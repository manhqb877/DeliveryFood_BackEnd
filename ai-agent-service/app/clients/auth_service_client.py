"""
Auth Service HTTP client — Gọi auth-service để lấy địa chỉ người dùng.
GET /auth/addresses  (headers: Authorization: Bearer <jwt>)
GET /auth/me         (headers: Authorization: Bearer <jwt>)
"""

import logging
from typing import Any, Dict, List, Optional
import httpx

from app.config import settings

logger = logging.getLogger(__name__)
_TIMEOUT = httpx.Timeout(10.0)


def _headers(jwt: Optional[str] = None) -> Dict[str, str]:
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if jwt:
        h["Authorization"] = f"Bearer {jwt}"
    return h


class AuthServiceClient:
    def __init__(self, base_url: str = settings.auth_service_url, jwt: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.jwt = jwt

    async def get_user_addresses(self) -> List[Dict[str, Any]]:
        """
        Lấy danh sách địa chỉ đã lưu của user.
        GET /auth/addresses
        Trả về danh sách UserAddressDto [{id, addressLine, latitude, longitude, isDefault, ...}]
        """
        if not self.jwt:
            return []

        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/auth/addresses",
                    headers=_headers(self.jwt),
                )
                if resp.status_code == 200:
                    data = resp.json()
                    # ApiResponse: {status, message, data: [...]}
                    return data.get("data") or []
                logger.warning(f"[AuthClient] GET /auth/addresses returned {resp.status_code}")
                return []
            except Exception as e:
                logger.error(f"[AuthClient] get_user_addresses error: {e}")
                return []

    async def get_user_profile(self) -> Optional[Dict[str, Any]]:
        """
        Lấy thông tin profile user: GET /auth/me
        """
        if not self.jwt:
            return None

        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/auth/me",
                    headers=_headers(self.jwt),
                )
                if resp.status_code == 200:
                    data = resp.json()
                    return data.get("data") if "data" in data else data
                return None
            except Exception as e:
                logger.error(f"[AuthClient] get_user_profile error: {e}")
                return None
