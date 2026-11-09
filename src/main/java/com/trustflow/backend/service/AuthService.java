package com.trustflow.backend.service;

import com.trustflow.backend.core.EthSignatureVerifier;
import com.trustflow.backend.core.JwtService;
import com.trustflow.backend.model.User;
import com.trustflow.backend.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EthSignatureVerifier verifier;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, EthSignatureVerifier verifier, JwtService jwtService) {
        this.userRepository = userRepository;
        this.verifier = verifier;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issueNonce(String walletAddress) {
        String addr = walletAddress.toLowerCase();
        String nonce = randomNonce();
        Optional<User> existing = userRepository.findByWalletAddress(addr);
        if (existing.isEmpty()) {
            User u = new User();
            u.setWalletAddress(addr);
            u.setNonce(nonce);
            u.setCreatedAt(OffsetDateTime.now());
            userRepository.save(u);
        } else {
            User u = existing.get();
            u.setNonce(nonce);
            userRepository.save(u);
        }
        return nonce;
    }

    @Transactional
    public LoginResult login(String walletAddress, String signature) {
        String addr = walletAddress.toLowerCase();
        Optional<User> existing = userRepository.findByWalletAddress(addr);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("user_not_found");
        }
        User u = existing.get();
        String nonce = u.getNonce();
        if (nonce == null || nonce.isEmpty()) {
            throw new IllegalStateException("nonce_not_found");
        }
        boolean ok = verifier.verifyPersonalSign(addr, nonce, signature);
        if (!ok) {
            throw new SecurityException("signature_invalid");
        }
        String token = jwtService.issue(addr, u.getUserId());
        String newNonce = randomNonce();
        u.setNonce(newNonce);
        userRepository.save(u);
        return new LoginResult(token, u.getUserId(), addr);
    }

    private String randomNonce() {
        byte[] b = new byte[24];
        secureRandom.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static class LoginResult {
        public final String token;
        public final Long userId;
        public final String walletAddress;

        public LoginResult(String token, Long userId, String walletAddress) {
            this.token = token;
            this.userId = userId;
            this.walletAddress = walletAddress;
        }
    }
}

