package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.dto.TransferResponse;
import com.rs.payments.wallet.exception.InvalidAmountException;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import com.rs.payments.wallet.service.WalletService;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import org.slf4j.Logger;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository  transactionRepository;
    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    public WalletServiceImpl(UserRepository userRepository, WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Wallet createWalletForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getWallet() != null) {
            throw new IllegalStateException("Wallet already exists for user");
        }
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(user);
        user.setWallet(wallet);

        user = userRepository.save(user); // Cascade saves wallet
        return user.getWallet();
    }

    @Override
    @Transactional
    public Wallet deposit(UUID walletId, BigDecimal amount) {

        log.info("Deposit request received for walletId={} amount={}",walletId,amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {

            log.error("Invalid deposit amount: {}", amount);
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> {
                    log.error("Wallet not found with id={}", walletId);
                    return new ResourceNotFoundException("Wallet not found");
                });

        BigDecimal updatedBalance = wallet.getBalance().add(amount);
        wallet.setBalance(updatedBalance);

        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Deposit successful for walletId={} newBalance={}",walletId,updatedBalance);

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setType(TransactionType.DEPOSIT);

        transactionRepository.save(tx);

        return savedWallet;
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID walletId, BigDecimal amount) {

        log.info("Withdraw request for walletId={} amount={}", walletId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            log.error("Insufficient balance for walletId={}", walletId);
            throw new InvalidAmountException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));

        Wallet savedWallet = walletRepository.save(wallet);

        //Transaction record
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setType(TransactionType.WITHDRAWAL);

        transactionRepository.save(tx);

        log.info("Withdraw successful walletId={} newBalance={}", walletId, wallet.getBalance());

        return savedWallet;


    }

    @Override
    @Transactional
    public TransferResponse transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {

        log.info("Transfer request from={} to={} amount={}", fromWalletId, toWalletId, amount);

        // 1. Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        // 2. Prevent self-transfer (edge case)
        if (fromWalletId.equals(toWalletId)) {
            throw new InvalidAmountException("Cannot transfer to the same wallet");
        }

        // 3. Fetch wallets
        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("From wallet not found"));

        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("To wallet not found"));

        // 4. Check sufficient balance
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            log.error("Insufficient balance for walletId={}", fromWalletId);
            throw new InvalidAmountException("Insufficient balance");
        }

        // 5. Debit sender
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));

        // 6. Credit receiver
        toWallet.setBalance(toWallet.getBalance().add(amount));

        // 7. Save both wallets
        Wallet updatedFromWallet = walletRepository.save(fromWallet);
        Wallet updatedToWallet = walletRepository.save(toWallet);

        // 8. Create transaction OUT
        Transaction txOut = new Transaction();
        txOut.setWallet(updatedFromWallet);
        txOut.setAmount(amount);
        txOut.setTimestamp(LocalDateTime.now());
        txOut.setType(TransactionType.TRANSFER_OUT);
        transactionRepository.save(txOut);

        // 9. Create transaction IN
        Transaction txIn = new Transaction();
        txIn.setWallet(updatedToWallet);
        txIn.setAmount(amount);
        txIn.setTimestamp(LocalDateTime.now());
        txIn.setType(TransactionType.TRANSFER_IN);
        transactionRepository.save(txIn);

        log.info("Transfer successful from={} to={} amount={}", fromWalletId, toWalletId, amount);

        // 10. Return response DTO
        return new TransferResponse(fromWalletId, toWalletId, amount);
    }
}