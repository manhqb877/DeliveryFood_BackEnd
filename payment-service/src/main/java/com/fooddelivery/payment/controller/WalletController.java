package com.fooddelivery.payment.controller;

import com.fooddelivery.payment.dto.request.RemitCodRequest;
import com.fooddelivery.payment.entity.Wallet;
import com.fooddelivery.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(userId));
    }

    @PostMapping("/remit-cod")
    public ResponseEntity<Wallet> remitCod(@RequestBody RemitCodRequest request) {
        return ResponseEntity.ok(walletService.remitCod(
                request.getOwnerId(), 
                request.getAmount(), 
                request.getOrderId(), 
                "Shipper remitted COD"
        ));
    }
}
