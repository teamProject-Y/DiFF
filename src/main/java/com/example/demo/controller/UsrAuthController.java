package com.example.demo.controller;

import com.example.demo.vo.Auth;
import com.example.demo.service.AuthService;
import com.example.demo.vo.ResultData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class UsrAuthController {

    private final AuthService authService;

    /** 로그인 API */
    @PostMapping("/api/DiFF/auth/login")
    public ResponseEntity<?> login(@RequestBody Auth authRq) {
        Auth authRp = this.authService.login(authRq);
        return ResponseEntity.ok( ResultData.from("S-1", "로그인 성공", "accessToken", authRp));
    }

    /** 토큰갱신 API */
    @GetMapping("/api/DiFF/auth/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("REFRESH_TOKEN") String refreshToken) {
        String newAccessToken = this.authService.refreshToken(refreshToken);
        return ResponseEntity.ok(newAccessToken);
    }
}
