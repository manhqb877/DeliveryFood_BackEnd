package com.fooddelivery.payment.enums;

/**
 * Loại biến động số dư ví
 */
public enum WalletTxType {
    DEBIT,   // Trừ tiền
    CREDIT,  // Cộng tiền
    REFUND   // Hoàn tiền
}