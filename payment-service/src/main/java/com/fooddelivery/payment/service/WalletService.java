package com.fooddelivery.payment.service;

import com.fooddelivery.payment.entity.Wallet;
import com.fooddelivery.payment.entity.WalletTransaction;
import com.fooddelivery.payment.enums.WalletStatus;
import com.fooddelivery.payment.enums.WalletTxType;
import com.fooddelivery.payment.repository.WalletRepository;
import com.fooddelivery.payment.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .creditLimit(BigDecimal.ZERO)
                        .status(WalletStatus.ACTIVE)
                        .build()));
    }

    @Transactional
    public Wallet remitCod(Long userId, BigDecimal amount, Long orderId, String description) {
        Wallet wallet = getOrCreateWallet(userId);
        
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new RuntimeException("Wallet is not active");
        }
        
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        
        wallet.setBalance(balanceAfter);
        wallet.setLastTransactionAt(OffsetDateTime.now());
        wallet = walletRepository.save(wallet);
        
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .orderId(orderId)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .txType(WalletTxType.CREDIT)
                .description(description)
                .build();
                
        walletTransactionRepository.save(transaction);
        
        log.info("Remitted {} COD to user {}'s wallet (Wallet ID: {})", amount, userId, wallet.getId());
        return wallet;
    }

    public List<WalletTransaction> getWalletTransactions(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
