"""
Payment Service HTTP client — REAL implementation (Phase 2).
PaymentController hiện tại chỉ có GET / → "hiAuth".
=> Agent sẽ dùng COD mặc định cho Phase 2 và trả link mock cho VNPAY/MOMO.
   Khi Payment Service bổ sung POST /payments/initiate (spec mục 6), thay đây.
"""

import logging
from typing import Any, Dict, Optional

import httpx

from app.config import settings

logger = logging.getLogger(__name__)

_TIMEOUT = httpx.Timeout(10.0)


def _headers(jwt: Optional[str] = None, idempotency_key: Optional[str] = None) -> Dict[str, str]:
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if jwt:
        h["Authorization"] = f"Bearer {jwt}"
    if idempotency_key:
        h["Idempotency-Key"] = idempotency_key
    return h


class PaymentServiceClient:
    def __init__(self, base_url: str = settings.payment_service_url, jwt: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.jwt = jwt

    async def initiate_payment(
        self,
        order_id: str,
        method: str,
        idempotency_key: str,
    ) -> Dict[str, Any]:
        """
        POST /payments/initiate { order_id, method }
        → { payment_url | qr_code }

        Payment Service hiện chưa có endpoint này (spec yêu cầu bổ sung).
        Trả thông tin hướng dẫn phù hợp với từng phương thức.
        Khi endpoint thật được thêm → đổi sang httpx call.
        """
        method_lower = method.lower()

        if method_lower == "cod":
            return {
                "order_id": order_id,
                "method": "cod",
                "instruction": (
                    "💵 **Thanh toán tiền mặt khi nhận hàng.**\n"
                    "Vui lòng chuẩn bị đúng số tiền khi shipper giao hàng."
                ),
                "payment_url": None,
            }

        # Với online payment — khi Payment Service bổ sung endpoint thật sẽ gọi vào đây
        # Hiện tại trả placeholder có order_id để user biết đơn đã tạo thành công
        payment_urls = {
            "vnpay": f"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?orderId={order_id}",
            "momo": f"https://test-payment.momo.vn/gw_payment/transactionProcessor?orderId={order_id}",
            "zalopay": f"https://sandbox.zalopay.vn/pay?apptransid={order_id}",
        }

        url = payment_urls.get(method_lower)
        if url:
            return {
                "order_id": order_id,
                "method": method_lower,
                "payment_url": url,
                "instruction": f"🔗 Nhấn link để thanh toán qua {method.upper()}: {url}",
            }

        return {
            "order_id": order_id,
            "error": f"Phương thức '{method}' chưa được hỗ trợ. Chọn: cod, vnpay, momo, zalopay",
        }
