"""
Analytics Service HTTP client.
Phase 1: mock data.
Phase 3: httpx calls tới ANALYTICS_SERVICE_URL.
Endpoint đích: GET /analytics/read-models/orders/bestsellers?areaId=&shopId=&limit=
"""

from typing import Any, Dict, List, Optional


class AnalyticsServiceClient:
    def __init__(self, base_url: str, jwt: Optional[str] = None):
        self.base_url = base_url
        self.jwt = jwt

    async def get_bestsellers(
        self,
        area_id: Optional[str] = None,
        shop_id: Optional[str] = None,
        limit: int = 10,
    ) -> List[Dict[str, Any]]:
        """
        GET /analytics/read-models/orders/bestsellers
        Phase 1 mock — Phase 3 sẽ thay bằng httpx thật.
        """
        return [
            {"item_id": "item-ct-001", "name": "Cơm Tấm Sườn Bì Chả", "total_sold": 320, "shop_id": "shop-001"},
            {"item_id": "item-bb-001", "name": "Bún Bò Huế đặc biệt", "total_sold": 280, "shop_id": "shop-002"},
        ][:limit]

    async def get_fraud_alerts(
        self,
        page: int = 0,
        size: int = 20,
    ) -> Dict[str, Any]:
        """
        GET /analytics/admin/fraud-alerts — tuỳ chọn, dùng nếu cần chặn trước khi redeem khuyến mãi.
        Phase 1 mock.
        """
        return {"content": [], "totalElements": 0}
