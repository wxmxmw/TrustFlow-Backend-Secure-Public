package com.trustflow.backend.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trustflow.backend.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController

@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    public static class NonceRequest {
        @NotBlank
        @JsonProperty("wallet_address")
        public String wallet_address;
    }

    public static class LoginRequest {
        @NotBlank
        @JsonProperty("wallet_address")
        public String wallet_address;

        @NotBlank
        @JsonProperty("signature")
        public String signature;
    }

    @PostMapping("/nonce")
    public Map<String, String> nonce(@Valid @RequestBody NonceRequest req) {

        String nonce = authService.issueNonce(req.wallet_address);
        Map<String, String> res = new LinkedHashMap<>();
        res.put("nonce", nonce);
        return res;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        try {

            AuthService.LoginResult r = authService.login(req.wallet_address, req.signature);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("access_token", r.token);
            data.put("token_type", "bearer");

            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("user_id", r.userId);
            userInfo.put("wallet_address", r.walletAddress);
            data.put("user_info", userInfo);

            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (SecurityException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(401).body(err);
        }
    }
}