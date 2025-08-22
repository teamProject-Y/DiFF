package com.example.demo.config;

import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Member;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

// Filter를 통해 시큐리티의 접근 권한 검사 수행
// Request로 부터 토큰을 추출하고, 토큰으로 부터 권한 정보를 추출
// 추출한 토큰이 유효한지 검사하고, 추출한 권한을 통해 Request, Response간 FilterChain을 수행
// 토큰 필터는 오직 토큰 유효성 검사만 수행 (UserDetailsService 호출 제거)
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = getTokenFromRequest(request);
        String path = request.getRequestURI();
        System.out.println("🔍 JwtTokenFilter - 요청 경로: " + path);

        // 로그인/회원가입은 토큰 검사 생략
        if (path.equals("/api/DiFF/member/doJoin") || path.equals("/api/DiFF/member/login") || path.equals("/upload")) {
            System.out.println("✅ 인증 생략 대상: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        if (accessToken != null) {
            System.out.println("🔑 추출된 accessToken: " + accessToken);

            if (jwtTokenProvider.validateToken(accessToken)) {
                String email = jwtTokenProvider.getMemberEmailFromToken(accessToken);
                System.out.println("✅ 토큰 유효, 사용자 Email: " + email);

                Member member = Optional.ofNullable(memberRepository.getMemberByEmail(email))
                        .orElseThrow(() -> new UsernameNotFoundException("Member not found with email: " + email));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(member, null, new ArrayList<>());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("🔐 SecurityContext에 인증 정보 설정 완료");
            } else {
                System.out.println("❌ 토큰 유효성 검사 실패");
            }
        } else {
            System.out.println("⚠️ Authorization 헤더에 Bearer 토큰 없음");
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");
        System.out.println("🙋‍♂️🙋‍♂️request: " + request.getRequestURI());
        System.out.println("🐻🐻 bearer token: " + bearerToken);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
