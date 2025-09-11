package com.example.demo.config;

import com.example.demo.service.GitHubOAuth2UserService;
import com.example.demo.service.GoogleOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public HttpFirewall allowSemicolonFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
        return web -> web.httpFirewall(firewall);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           GitHubOAuth2UserService githubOAuth2UserService,
                                           GoogleOAuth2UserService googleOAuth2UserService) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(hb -> hb.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()

                        // 완전 공개 (누구나 접근 가능)
                        .requestMatchers(
                                "/", "/api/DiFF/home/main",
                                "/resource/**", "/css/**", "/js/**", "/images/**",
                                "/oauth2/**", "/login/**",

                                // 드래프트
                                "/api/DiFF/draft/**", "/upload", "/api/DiFF/github/diag",

                                // 로그인 & 회원가입
                                "/DiFF/member/login", "/DiFF/member/doLogin",
                                "/DiFF/member/join", "/DiFF/member/doJoin",
                                "/DiFF/member/login?error=true",

                                // 회원 관련 API
                                "/api/DiFF/auth/**", "/api/DiFF/auth/refresh",
                                "/api/DiFF/member/login", "/api/DiFF/member/doJoin",
                                "/api/DiFF/member/check/**",
                                "/api/DiFF/member/findPw",
                                "/api/DiFF/member/updatePassword",
                                "/api/DiFF/member/verify",

                                // 글 관련 API
                                "/api/DiFF/article/**", "/api/DiFF/reply/list",

                                // 알림
                                "/api/DiFF/notify/**"
                        ).permitAll()

                        // GET 요청은 모두 허용
                        .requestMatchers(HttpMethod.GET,
                                "/api/DiFF/attachment/**",
                                "/api/DiFF/reply/**",
                                "/api/DiFF/article/**",
                                "/api/DiFF/github/**"
                        ).permitAll()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler())
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(request -> {
                                    String registrationId = request.getClientRegistration().getRegistrationId();
                                    if ("github".equals(registrationId)) {
                                        return githubOAuth2UserService.loadUser(request);
                                    } else if ("google".equals(registrationId)) {
                                        return googleOAuth2UserService.loadUser(request);
                                    }
                                    throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
                                })
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("http://44.206.130.144:3000/DiFF/home/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(401);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"로그인 필요\"}");
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(403);
            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"권한 없음\"}");
        };
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var cfg = new org.springframework.web.cors.CorsConfiguration();
        cfg.setAllowCredentials(true);

        // 허용할 프론트 주소들
        cfg.addAllowedOriginPattern("http://44.206.130.144:3000");
        cfg.addAllowedOriginPattern("http://localhost:3000");
        cfg.addAllowedOriginPattern("http://127.0.0.1:3000");

        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");

        // 토큰 헤더도 노출
        cfg.addExposedHeader("Authorization");
        cfg.addExposedHeader("REFRESH_TOKEN");
        cfg.addAllowedHeader("REFRESH_TOKEN");

        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

}
