package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String accessToken = getTokenFromRequest(request);

        // R2 전체, 로그인/회원가입, 업로드는 토큰 검사 생략
        if (path.startsWith("/r2/")
                || path.startsWith("/api/DiFF/auth/login")
                || path.startsWith("/api/DiFF/auth/join")
                || path.startsWith("/api/DiFF/auth/refresh")
                || path.startsWith("/api/DiFF/auth/link/**")
                || path.startsWith("/oauth2/**")
                || path.startsWith("/error")
                || path.equals("/upload")) {
            System.out.println("🟢 [JwtTokenFilter] bypass: " + path);
            chain.doFilter(request, response);
            return;
        }

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            Long memberId = jwtTokenProvider.getMemberIdFromToken(accessToken);
            String email = jwtTokenProvider.getMemberEmailFromToken(accessToken);
            String nickName = jwtTokenProvider.getNickNameFromToken(accessToken);
            System.out.println("✅ [JwtTokenFilter] token valid, memberId=" + memberId);

            CustomUserDetails userDetails = new CustomUserDetails(memberId, nickName, email);
            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            System.out.println("❌ [JwtTokenFilter] token missing/invalid, path=" + path);
        }

        chain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
