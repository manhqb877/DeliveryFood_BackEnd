package com.fooddelivery.payment.enums;
/**
 * Phân loại giao dịch tài chính
 */
public enum TransactionType {
    ORDER_PAYMENT,     // Thanh toán cho đơn hàng
    REFUND,            // Hoàn tiền khi đơn bị huỷ
    COD_COLLECTION,    // Ghi nhận Shipper thu COD
    WALLET_TOPUP,      // Nạp tiền vào ví trả sau
    COMMISSION_DEDUCT  // Trừ hoa hồng từ Shop
}