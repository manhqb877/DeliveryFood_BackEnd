"""
Order Service HTTP client — REAL implementation (Phase 2).
POST /orders   → placeOrder (nhận cartId, deliveryAddress, paymentMethod, promotionCode)
GET  /orders/{id} → getOrderById
"""

import logging
from typing import Any, Dict, List, Optional

import httpx

from app.config import settings

logger = logging.getLogger(__name__)

_TIMEOUT = httpx.Timeout(15.0)


def _headers(jwt: Optional[str] = None, idempotency_key: Optional[str] = None) -> Dict[str, str]:
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if jwt:
        h["Authorization"] = f"Bearer {jwt}"
    if idempotency_key:
        h["Idempotency-Key"] = idempotency_key
    return h


class OrderServiceClient:
    def __init__(self, base_url: str = settings.order_service_url, jwt: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.jwt = jwt

    async def sync_cart_to_order_service(
        self,
        shop_id: int,
        items: List[Dict[str, Any]],
        user_id: Optional[int] = None,
        guest_session_id: Optional[int] = None,
    ) -> Optional[int]:
        """
        Đồng bộ các món trong giỏ của AI Agent vào order-service qua POST /carts/items
        để tạo một Cart ID hợp lệ dùng cho đặt hàng.
        """
        cart_id = None
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            hdrs = _headers(self.jwt)
            if user_id:
                hdrs["X-User-Id"] = str(user_id)
            if guest_session_id:
                hdrs["X-Guest-Session-Id"] = str(guest_session_id)

            for it in items:
                try:
                    name_val = it.get("item_name") or it.get("name") or ""
                    if not name_val:
                        try:
                            from app.clients.core_service_client import CoreServiceClient
                            core = CoreServiceClient(base_url=settings.core_service_url, jwt=self.jwt)
                            detail = await core.get_item(str(it.get("item_id")))
                            if detail and detail.get("name"):
                                name_val = detail["name"]
                        except Exception:
                            pass

                    payload = {
                        "shopId": shop_id,
                        "itemId": int(it.get("item_id")),
                        "itemName": name_val or f"Món ăn #{it.get('item_id')}",
                        "unitPrice": float(it.get("unit_price") or it.get("price") or 0.0),
                        "quantity": int(it.get("quantity") or 1),
                        "selectedOptions": [],
                        "itemNote": it.get("note") or "",
                    }
                    if user_id:
                        payload["userId"] = user_id
                    if guest_session_id:
                        payload["guestSessionId"] = guest_session_id

                    resp = await client.post(
                        f"{self.base_url}/carts/items",
                        json=payload,
                        headers=hdrs,
                    )
                    if resp.status_code == 200:
                        res_data = resp.json()
                        cart_data = res_data.get("data")
                        if isinstance(cart_data, dict) and cart_data.get("id"):
                            cart_id = cart_data.get("id")
                except Exception as ex:
                    logger.warning(f"[OrderClient] Error syncing item {it} to cart: {ex}")
        return cart_id

    async def create_order(
        self,
        items: List[Dict[str, Any]],
        delivery_address: Any,
        promo_code: Optional[str],
        idempotency_key: str,
        payment_method: str = "COD",
        shop_id: Optional[int] = None,
        cart_id: Optional[int] = None,
        user_id: Optional[int] = None,
        guest_session_id: Optional[int] = None,
        discount_amount: float = 0.0,
    ) -> Dict[str, Any]:
        """
        POST /orders
        OrderRequest: { cartId, deliveryAddress (Map), paymentMethod, orderNote,
                        idempotencyKey, promotionCode, discountAmount }
        Headers: X-User-Id, X-Guest-Session-Id, Idempotency-Key
        """
        # 1. Đảm bảo deliveryAddress là một Map/Dict (order-service yêu cầu Map, không nhận raw String)
        if isinstance(delivery_address, str):
            addr_map = {
                "fullAddress": delivery_address.strip(),
                "addressLine": delivery_address.strip(),
                "raw_text": delivery_address.strip(),
            }
        elif isinstance(delivery_address, dict):
            addr_str = delivery_address.get("raw_text") or delivery_address.get("fullAddress") or delivery_address.get("addressLine") or ""
            if not addr_str:
                parts = [
                    delivery_address.get("room_or_unit"),
                    delivery_address.get("building_or_zone"),
                    delivery_address.get("district"),
                    delivery_address.get("city"),
                ]
                addr_str = ", ".join(p for p in parts if p)
            addr_map = {
                **delivery_address,
                "fullAddress": addr_str or "Địa chỉ giao hàng",
                "addressLine": addr_str or "Địa chỉ giao hàng",
                "raw_text": addr_str or "Địa chỉ giao hàng",
            }
        else:
            addr_map = {"fullAddress": str(delivery_address)}

        # Tự động bổ sung thông tin người nhận (recipientName, recipientPhone) từ Auth Service nếu còn thiếu
        if not addr_map.get("recipientName") or not addr_map.get("recipientPhone"):
            try:
                from app.clients.auth_service_client import AuthServiceClient
                auth_client = AuthServiceClient(base_url=settings.auth_service_url, jwt=self.jwt)
                user_prof = await auth_client.get_user_profile()
                if user_prof:
                    if not addr_map.get("recipientName"):
                        addr_map["recipientName"] = user_prof.get("fullName") or user_prof.get("name") or "Khách hàng"
                    if not addr_map.get("recipientPhone"):
                        addr_map["recipientPhone"] = user_prof.get("phone") or user_prof.get("phoneNumber") or ""
            except Exception as e:
                logger.warning(f"[OrderClient] Could not fetch user profile for recipient info: {e}")

        # 2. Nếu chưa có cart_id từ order-service, tự động sync các món vào order-service để lấy cartId
        if not cart_id and items and shop_id:
            cart_id = await self.sync_cart_to_order_service(
                shop_id=shop_id,
                items=items,
                user_id=user_id,
                guest_session_id=guest_session_id,
            )

        payload: Dict[str, Any] = {
            "deliveryAddress": addr_map,
            "paymentMethod": payment_method.upper(),
            "idempotencyKey": idempotency_key,
        }
        if cart_id is not None:
            payload["cartId"] = cart_id
        if promo_code:
            payload["promotionCode"] = promo_code
        if discount_amount > 0:
            payload["discountAmount"] = discount_amount

        hdrs = _headers(self.jwt, idempotency_key)
        if user_id:
            hdrs["X-User-Id"] = str(user_id)
        if guest_session_id:
            hdrs["X-Guest-Session-Id"] = str(guest_session_id)

        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.post(
                    f"{self.base_url}/orders",
                    json=payload,
                    headers=hdrs,
                )
                resp.raise_for_status()
                data = resp.json()
                order_data = data.get("data") if "data" in data else data
                return {
                    "order_id": str(order_data.get("id") or order_data.get("orderId") or ""),
                    "order_code": order_data.get("orderCode") or "",
                    "status": order_data.get("status") or "PENDING",
                    "total_amount": float(order_data.get("totalAmount") or order_data.get("finalAmount") or 0),
                    "raw": order_data,
                }
            except httpx.HTTPStatusError as e:
                body = {}
                try:
                    body = e.response.json()
                except Exception:
                    pass
                logger.error(f"[OrderClient] POST /orders → {e.response.status_code}: {body}")
                return {
                    "success": False,
                    "error": body.get("message") or f"Lỗi tạo đơn (HTTP {e.response.status_code})",
                }
            except Exception as e:
                logger.error(f"[OrderClient] create_order error: {e}")
                return {"success": False, "error": str(e)}

    async def get_order_status(self, order_id: str) -> Dict[str, Any]:
        """GET /orders/{id}"""
        status_labels = {
            "PENDING": "⏳ Đang chờ xác nhận",
            "CONFIRMED": "✅ Quán đã xác nhận",
            "PREPARING": "👨‍🍳 Đang chuẩn bị",
            "READY": "📦 Sẵn sàng giao",
            "DELIVERING": "🚴 Đang giao hàng",
            "DELIVERED": "🎉 Đã giao thành công",
            "CANCELLED": "❌ Đã huỷ",
            "FAILED": "⚠️ Thất bại",
        }

        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/orders/{order_id}",
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                data = resp.json()
                status = data.get("status") or "UNKNOWN"
                return {
                    "order_id": order_id,
                    "order_code": data.get("orderCode") or "",
                    "status": status,
                    "status_description": status_labels.get(status, status),
                    "total_amount": float(data.get("totalAmount") or data.get("finalAmount") or 0),
                }
            except httpx.HTTPStatusError as e:
                logger.error(f"[OrderClient] GET /orders/{order_id} → {e.response.status_code}")
                return {"order_id": order_id, "error": f"Không tìm thấy đơn (HTTP {e.response.status_code})"}
            except Exception as e:
                logger.error(f"[OrderClient] get_order_status error: {e}")
                return {"order_id": order_id, "error": str(e)}
