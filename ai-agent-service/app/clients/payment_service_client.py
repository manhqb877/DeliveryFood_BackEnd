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

<<<<<<< HEAD
    async def generate_sepay_qr(
        self,
        order_id: int,
        order_code: str,
        amount: float,
        user_id: Optional[int] = None,
    ) -> Optional[Dict[str, Any]]:
        """
        Gọi payment-service POST /payments/sepay/qr để tạo mã VietQR chuẩn SePay.
        """
        async with httpx.AsyncClient(timeout=_TIMEOUT) as client:
            try:
                payload = {
                    "orderId": order_id,
                    "orderCode": order_code,
                    "amount": amount,
                }
                if user_id:
                    payload["userId"] = user_id

                resp = await client.post(
                    f"{self.base_url}/payments/sepay/qr",
                    json=payload,
                    headers=_headers(self.jwt),
                )
                if resp.status_code == 200:
                    return resp.json()
                logger.warning(f"[PaymentClient] POST /payments/sepay/qr returned {resp.status_code}: {resp.text}")
            except Exception as e:
                logger.error(f"[PaymentClient] generate_sepay_qr error: {e}")

        # Fallback tạo trực tiếp link SePay chuẩn nếu payment-service tạm thời bận
        import urllib.parse
        encoded_desc = urllib.parse.quote(order_code)
        fallback_qr = f"https://qr.sepay.vn/img?acc=025452790502&bank=MBBank&amount={int(amount)}&des={encoded_desc}"
        return {
            "orderId": order_id,
            "orderCode": order_code,
            "amount": amount,
            "bankName": "MBBank",
            "accountNumber": "025452790502",
            "accountHolder": "NGUYEN THAI AN",
            "paymentDescription": order_code,
            "qrUrl": fallback_qr,
            "paymentStatus": "PENDING",
        }

=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    async def initiate_payment(
        self,
        order_id: str,
        method: str,
        idempotency_key: str,
<<<<<<< HEAD
        order_code: Optional[str] = None,
        amount: float = 0.0,
        user_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        Khởi tạo thông tin thanh toán:
        - cod: Thanh toán tiền mặt khi nhận hàng.
        - sepay / vietqr / qr / chuyen_khoan / online: Tạo mã VietQR SePay.
        - vnpay / momo / zalopay: Link thanh toán tương ứng.
        """
        method_lower = method.lower()

        if method_lower in ["sepay", "vietqr", "qr", "chuyen_khoan", "online"]:
            oid = int(order_id) if str(order_id).isdigit() else 0
            code = order_code or f"ORD{order_id}"
            qr_data = await self.generate_sepay_qr(
                order_id=oid,
                order_code=code,
                amount=amount,
                user_id=user_id,
            )
            return {
                "order_id": order_id,
                "order_code": code,
                "method": "sepay",
                "payment_qr": qr_data,
                "instruction": (
                    f"⚡ **Mã thanh toán SePay VietQR đã được tạo:**\n"
                    f"- Ngân hàng: {qr_data.get('bankName', 'MBBank')}\n"
                    f"- Số tài khoản: `{qr_data.get('accountNumber', '025452790502')}`\n"
                    f"- Chủ tài khoản: **{qr_data.get('accountHolder', 'NGUYEN THAI AN')}**\n"
                    f"- Số tiền: **{int(amount):,}đ**\n"
                    f"- Nội dung: `{qr_data.get('paymentDescription', code)}`\n\n"
                    f"Quét mã QR hiển thị bên dưới hoặc chuyển khoản đúng nội dung trên."
                ),
            }

=======
    ) -> Dict[str, Any]:
        """
        POST /payments/initiate { order_id, method }
        → { payment_url | qr_code }

        Payment Service hiện chưa có endpoint này (spec yêu cầu bổ sung).
        Trả thông tin hướng dẫn phù hợp với từng phương thức.
        Khi endpoint thật được thêm → đổi sang httpx call.
        """
        method_lower = method.lower()

>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
        if method_lower == "cod":
            return {
                "order_id": order_id,
                "method": "cod",
                "instruction": (
<<<<<<< HEAD
                    "💵 **Thanh toán tiền mặt khi nhận hàng (COD).**\n"
=======
                    "💵 **Thanh toán tiền mặt khi nhận hàng.**\n"
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
                    "Vui lòng chuẩn bị đúng số tiền khi shipper giao hàng."
                ),
                "payment_url": None,
            }

<<<<<<< HEAD
=======
        # Với online payment — khi Payment Service bổ sung endpoint thật sẽ gọi vào đây
        # Hiện tại trả placeholder có order_id để user biết đơn đã tạo thành công
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
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
<<<<<<< HEAD
            "error": f"Phương thức '{method}' chưa được hỗ trợ. Chọn: cod, sepay (vietqr), vnpay, momo, zalopay",
=======
            "error": f"Phương thức '{method}' chưa được hỗ trợ. Chọn: cod, vnpay, momo, zalopay",
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
        }
