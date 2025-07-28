package com.example.demo.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 사용자 정보에서 유저 PK 등 추출 (CustomUserDetails 등)
        CustomMemberDetails user = (CustomMemberDetails) authentication.getPrincipal();

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // 응답 헤더로 토큰 내려주기
        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("REFRESH_TOKEN", refreshToken);

        // 혹은 쿼리파라미터/쿠키/응답 Body 등 프론트와 맞게 전달 방식 (일단 남겨둠)
        // response.sendRedirect("http://localhost:3000/DiFF/home/main?token=" + accessToken);
    }
}

