package com.example.demo.controller;

import com.example.demo.service.AuthService;
import com.example.demo.service.OAuthAccountService;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/DiFF/auth")
public class UsrAuthController {

    private final AuthService authService;
    private final OAuthAccountService oAuthAccountService;
    private final Rq rq;

    /** 토큰갱신 API */
    @GetMapping("/refresh")
    public ResponseEntity<Object> refreshToken(@RequestHeader("REFRESH_TOKEN") String refreshToken) {
        String newAccessToken = this.authService.refreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    /** 로컬, 소셜 통합 **/
    @GetMapping("/link/{provider}")
    public void socialLogin(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = switch (provider.toLowerCase()) {
            case "github" -> "/oauth2/authorization/github";
            case "google" -> "/oauth2/authorization/google";
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/linked")
    public ResponseEntity<?> getLinked() {

        Long memberId = rq.getLoginedMemberId();
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인 필요"));
        }
        Map<String, Boolean> linked = oAuthAccountService.getLinkedProviders(memberId);
        return ResponseEntity.ok(linked);
    }

}
