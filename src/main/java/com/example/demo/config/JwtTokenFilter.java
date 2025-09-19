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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = getTokenFromRequest(request);
        String path = request.getRequestURI();

        // 로그인/회원가입은 토큰 검사 생략
        if (path.equals("/api/DiFF/member/doJoin") || path.equals("/api/DiFF/member/login") || path.equals("/upload")
        || path.equals("/.well-known/**") || path.equals("/actuator/**")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("/upload".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            long start = System.currentTimeMillis();
            String len = request.getHeader("Content-Length");
            String type = request.getHeader("Content-Type");
            System.out.printf(">> /upload start t=%d, CL=%s, CT=%s%n", start, len, type);
            try {
                filterChain.doFilter(request, response);
            } finally {
                long ms = System.currentTimeMillis() - start;
                System.out.printf("<< /upload end   ms=%d, status=%d%n", ms, response.getStatus());
            }
        } else {
            filterChain.doFilter(request, response);
        }

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            // 토큰에서 사용자 정보 직접 꺼냄
            Long memberId = jwtTokenProvider.getMemberIdFromToken(accessToken);
            String email = jwtTokenProvider.getMemberEmailFromToken(accessToken);
            String nickName = jwtTokenProvider.getNickNameFromToken(accessToken);

            CustomUserDetails userDetails = new CustomUserDetails(memberId, nickName, email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
