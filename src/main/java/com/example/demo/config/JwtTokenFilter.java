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

    /** 공개/정적/소셜 경로는 아예 필터 스킵 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) throws ServletException {
        String uri = req.getRequestURI();
        String method = req.getMethod();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // 인증 없이 접근해야 하는 경로
        if (uri.startsWith("/api/DiFF/auth/")) return true;
        if (uri.startsWith("/oauth2/") || uri.startsWith("/login/")) return true;

        // 정적/에러/기타
        if (uri.equals("/error")) return true;
        if (uri.equals("/favicon.ico")) return true;
        if (uri.startsWith("/resource/") || uri.startsWith("/css/")
                || uri.startsWith("/js/") || uri.startsWith("/images/")) return true;

        if (uri.startsWith("/r2/")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            chain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        // 헤더가 있을 때만 검증
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Long memberId = jwtTokenProvider.getMemberIdFromToken(token);
                String email = jwtTokenProvider.getMemberEmailFromToken(token);
                String nickName = jwtTokenProvider.getNickNameFromToken(token);

                CustomUserDetails userDetails = new CustomUserDetails(memberId, nickName, email);
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {}
        } catch (Exception ex) {
            System.out.println("JWT parse/validate failed");
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
