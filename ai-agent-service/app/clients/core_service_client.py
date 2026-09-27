"""
Core Service HTTP client — REAL implementation (Phase 2).
Gọi API thật qua httpx, forward JWT, không mock.
Endpoints thực tế:
  GET  /core/shops                     → danh sách tất cả shop
  GET  /core/shops/{shopId}/details    → chi tiết shop + categories + items + options
  GET  /core/items/search?keyword=     → tìm item
  GET  /core/items/{itemId}/options    → options của item
  POST /promotions/validate            → validate promo code
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


class CoreServiceClient:
    def __init__(self, base_url: str = settings.core_service_url, jwt: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.jwt = jwt

    # ──────────────────────────────────────────────────────────────────────────
    # SHOPS
    # ──────────────────────────────────────────────────────────────────────────

    async def get_item(self, item_id: str) -> Optional[Dict[str, Any]]:
        """
        Lấy thông tin chi tiết món ăn qua GET /core/items/{itemId}.
        Trả về dict {item_id, name, price, shop_id, shop_name} hoặc None nếu lỗi.
        """
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/core/items/{item_id.strip()}",
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                it = resp.json()
                return {
                    "item_id": str(it.get("id")),
                    "name": it.get("name") or it.get("itemName") or "",
                    "price": float(it.get("basePrice") or it.get("price") or 0),
                    "shop_id": str(it.get("shopId") or ""),
                    "shop_name": it.get("shopName") or "",
                }
            except Exception as e:
                logger.warning(f"[CoreClient] get_item({item_id}): {e}")
                return None

    async def search_items(self, keyword: str = "", max_price: Optional[float] = None) -> List[Dict[str, Any]]:
        """
        Tìm kiếm món ăn từ Core Service qua GET /core/items/search?keyword=...
        """
        kw = keyword.strip() if keyword else "a"
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                import urllib.parse
                resp = await client.get(
                    f"{self.base_url}/core/items/search?keyword={urllib.parse.quote(kw)}",
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                items: List[Dict[str, Any]] = resp.json()
            except Exception as e:
                logger.error(f"[CoreClient] search_items error: {e}")
                return []

        if max_price is not None:
            items = [it for it in items if float(it.get("basePrice") or 0) <= max_price]

        results = []
        for it in items[:15]:
            results.append({
                "item_id": str(it.get("id")),
                "name": it.get("name"),
                "price": float(it.get("basePrice") or 0),
                "shop_id": str(it.get("shopId")),
                "shop_name": it.get("shopName") or "",
                "description": it.get("description") or "",
                "rating": float(it.get("avgRating") or 0),
            })
        return results

    async def search_shops(
        self,
        area_code: str,
        keyword: str = "",
        price_min: Optional[int] = None,
        price_max: Optional[int] = None,
        min_rating: Optional[float] = None,
        bestseller_only: bool = False,
        is_open_now: bool = True,
        sort_by: str = "relevance",
    ) -> List[Dict[str, Any]]:
        """
        Gọi GET /core/shops → lấy danh sách shop, hỗ trợ tìm theo tên quán hoặc món ăn trong quán.
        """
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/core/shops",
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                shops: List[Dict[str, Any]] = resp.json()
            except httpx.HTTPStatusError as e:
                logger.error(f"[CoreClient] GET /core/shops → {e.response.status_code}")
                return []
            except Exception as e:
                logger.error(f"[CoreClient] GET /core/shops error: {e}")
                return []

        # 1. Lọc shop đang active
        result = [s for s in shops if s.get("isActive", True)]

        # 2. Lọc is_open_now: chỉ lọc khắt khe nếu có quán đang mở, fallback tất cả quán active nếu hệ thống test chưa bật isOpen
        open_shops = [s for s in result if s.get("isOpen") or s.get("isAcceptingOrders")]
        if is_open_now and open_shops:
            result = open_shops

        # 3. Lọc theo từ khóa
        if keyword:
            kw = keyword.strip().lower()
            generic_queries = {"tất cả", "danh sách", "quán nào", "khu vực", "ở đây", "gần đây", "món ngon", "ngon", "giá rẻ"}
            if kw not in generic_queries:
                matched = [
                    s for s in result
                    if kw in (s.get("shopName") or "").lower()
                    or kw in (s.get("locationDetail") or "").lower()
                ]
                # Nếu không tìm thấy theo tên quán, tìm xem có quán nào bán món chứa từ khoá này không
                if not matched:
                    item_matches = await self.search_items(keyword=kw)
                    matched_shop_ids = {str(it.get("shop_id")) for it in item_matches}
                    matched = [s for s in result if str(s.get("id")) in matched_shop_ids]
                if matched:
                    result = matched

        if min_rating is not None:
            result = [
                s for s in result
                if float(s.get("avgRating") or s.get("avg_rating") or 0) >= min_rating
            ]

        # Chuẩn hóa output
        normalized = []
        for s in result:
            normalized.append({
                "shop_id": str(s.get("id") or s.get("shopId") or ""),
                "name": s.get("shopName") or s.get("shop_name") or "",
                "area_id": s.get("areaId") or s.get("area_id"),
                "rating": float(s.get("avgRating") or s.get("avg_rating") or 0),
                "is_open": s.get("isOpen") or s.get("isAcceptingOrders") or False,
                "location": s.get("locationDetail") or s.get("location_detail") or "",
                "logo_url": s.get("logoUrl") or s.get("logo_url") or "",
            })

        return normalized

    async def get_menu(self, shop_id: str) -> Dict[str, Any]:
        """
        Gọi GET /core/shops/{shopId}/details → trả shop + categories + items + options.
        Tự động giải quyết slug/tên quán thành shop_id số nếu cần.
        """
        sid = str(shop_id).strip()
        if not sid.isdigit():
            found_shops = await self.search_shops(area_code="", keyword=sid, is_open_now=False)
            if found_shops:
                sid = str(found_shops[0]["shop_id"])
            else:
                return {"error": f"Không tìm thấy quán '{shop_id}'", "shop_id": shop_id, "items": []}

        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/core/shops/{sid}/details",
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                data: Dict[str, Any] = resp.json()
            except httpx.HTTPStatusError as e:
                logger.error(f"[CoreClient] GET /core/shops/{sid}/details → {e.response.status_code}")
                return {"error": f"Quán không tồn tại hoặc đã đóng cửa (HTTP {e.response.status_code})", "shop_id": sid, "items": []}
            except Exception as e:
                logger.error(f"[CoreClient] get_menu error: {e}")
                return {"error": str(e), "shop_id": sid, "items": []}

        # Normalize: data.categories[].items[]
        categories = data.get("categories") or []
        items_flat = []
        shop_name = data.get("shopName") or ""
        for cat in categories:
            cat_name = cat.get("categoryName") or cat.get("name") or ""
            for item in cat.get("items") or []:
                items_flat.append({
                    "item_id": str(item.get("id") or item.get("itemId") or ""),
                    "name": item.get("itemName") or item.get("name") or "",
                    "price": float(item.get("basePrice") or item.get("price") or 0),
                    "category": cat_name,
                    "shop_id": sid,
                    "shop_name": shop_name,
                })
                if len(items_flat) >= 25:
                    break
            if len(items_flat) >= 25:
                break

        return {
            "shop_id": sid,
            "shop_name": shop_name,
            "avg_rating": float(data.get("avgRating") or 0),
            "total_reviews": data.get("totalReviews") or 0,
            "items": items_flat,
        }

    async def get_promotions(self, shop_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """
        Lấy danh sách mã giảm giá hoạt động của shop và của toàn sàn.
        """
        promos = []
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            # 1. Platform promotions
            try:
                p_resp = await client.get(f"{self.base_url}/promotions/platform", headers=_headers(self.jwt))
                if p_resp.status_code == 200:
                    promos.extend(p_resp.json())
            except Exception as e:
                logger.warning(f"[CoreClient] GET /promotions/platform error: {e}")

            # 2. Shop promotions
            if shop_id:
                sid = str(shop_id).strip()
                if not sid.isdigit():
                    found_shops = await self.search_shops(area_code="", keyword=sid, is_open_now=False)
                    if found_shops:
                        sid = str(found_shops[0]["shop_id"])
                if sid.isdigit():
                    try:
                        s_resp = await client.get(f"{self.base_url}/promotions/shop/{sid}/active", headers=_headers(self.jwt))
                        if s_resp.status_code == 200:
                            promos.extend(s_resp.json())
                    except Exception as e:
                        logger.warning(f"[CoreClient] GET /promotions/shop/{sid}/active error: {e}")

        # Chuẩn hóa
        normalized = []
        seen_codes = set()
        for p in promos:
            code = p.get("code")
            if not code or code in seen_codes:
                continue
            if not p.get("isActive", True):
                continue
            seen_codes.add(code)
            disc_val = p.get("discountValue") or 0
            ptype = p.get("promoType") or ""
            desc = f"Giảm {disc_val:,.0f}{'%' if ptype == 'PERCENT' else 'đ'} cho đơn từ {p.get('minOrderValue', 0):,.0f}đ"
            normalized.append({
                "promo_id": p.get("id"),
                "code": code,
                "promo_type": ptype,
                "discount_value": disc_val,
                "min_order_value": float(p.get("minOrderValue") or 0),
                "max_discount_amount": float(p.get("maxDiscountAmount") or 0) if p.get("maxDiscountAmount") else None,
                "description": desc,
                "scope": p.get("scope"),
            })
        return normalized

    # ──────────────────────────────────────────────────────────────────────────
    # ITEMS
    # ──────────────────────────────────────────────────────────────────────────

    async def get_item_options(self, item_id: str) -> List[Dict[str, Any]]:
        """GET /core/items/{itemId}/options"""
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.get(
                    f"{self.base_url}/core/items/{item_id}/options",
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                return resp.json()
            except Exception as e:
                logger.error(f"[CoreClient] get_item_options({item_id}): {e}")
                return []

    # ──────────────────────────────────────────────────────────────────────────
    # PROMOTIONS
    # ──────────────────────────────────────────────────────────────────────────

    async def apply_promotion(
        self,
        promo_code: str,
        user_type: str,
        area_code: str,
        cart_subtotal: float,
        shop_id: Optional[str] = None,
        user_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        POST /promotions/validate
        Body: { code, shopId, userId, orderAmount }
        """
        payload: Dict[str, Any] = {
            "code": promo_code,
            "orderAmount": cart_subtotal,
        }
        if shop_id:
            try:
                payload["shopId"] = int(shop_id)
            except Exception:
                pass
        if user_id:
            payload["userId"] = user_id

        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                resp = await client.post(
                    f"{self.base_url}/promotions/validate",
                    json=payload,
                    headers=_headers(self.jwt),
                )
                resp.raise_for_status()
                data = resp.json()
                # PromotionValidationResponse: { valid, discountAmount, message }
                return {
                    "valid": data.get("valid", False),
                    "discount_amount": float(data.get("discountAmount") or 0),
                    "reason_if_invalid": data.get("message") or "Mã không hợp lệ",
                }
            except httpx.HTTPStatusError as e:
                body = {}
                try:
                    body = e.response.json()
                except Exception:
                    pass
                return {
                    "valid": False,
                    "discount_amount": 0,
                    "reason_if_invalid": body.get("message") or f"Lỗi validate mã (HTTP {e.response.status_code})",
                }
            except Exception as e:
                logger.error(f"[CoreClient] apply_promotion error: {e}")
                return {"valid": False, "discount_amount": 0, "reason_if_invalid": str(e)}
