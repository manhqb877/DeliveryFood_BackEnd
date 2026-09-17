package com.fooddelivery.orders.enums;

public enum PaymentMethod {
    COD,     // Tiền mặt khi nhận hàng
    ONLINE,  // Cổng thanh toán (VNPay/Momo/ZaloPay)
    WALLET   // Ví trả sau / công nợ (chỉ CUSTOMER xác thực khu)
}