package com.example.demo.controller;

import com.example.demo.vo.Auth;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class UsrAuthController {

    private final AuthService authService;

    /** 로그인 API */
    @PostMapping("/api/v1/DiFF/auth/login")
    public ResponseEntity<?> login(@RequestBody Auth authRq) {
        Auth authRp = this.authService.login(authRq);
        return ResponseEntity.ok(authRp);
    }

    /** 토큰갱신 API */
    @GetMapping("/api/v1/DiFF/auth/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("REFRESH_TOKEN") String refreshToken) {
        String newAccessToken = this.authService.refreshToken(refreshToken);
        return ResponseEntity.ok(newAccessToken);
    }
}
