package com.fooddelivery.payment.service;

import com.fooddelivery.payment.dto.request.PaymentQrRequest;
import com.fooddelivery.payment.dto.request.SepayWebhookRequest;
import com.fooddelivery.payment.dto.response.PaymentQrResponse;
import com.fooddelivery.payment.entity.Transaction;

public interface PaymentService {
    PaymentQrResponse generateSepayQr(PaymentQrRequest request);
    boolean handleSepayWebhook(SepayWebhookRequest webhookRequest);
    Transaction getPaymentStatusByOrderId(Long orderId);
}
