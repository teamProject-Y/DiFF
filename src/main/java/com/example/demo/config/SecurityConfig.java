package com.example.demo.config;

import com.example.demo.service.GitHubOAuth2UserService;
import com.example.demo.service.GoogleOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // #### 1) R2 전용 체인: /r2/** 전체 허용 ####
    @Bean
    @Order(1)
    public SecurityFilterChain r2Chain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/r2/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS 적용
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/r2/**").permitAll()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler())
                );
        return http.build();
    }

    // #### 2) 기본 체인: 나머지 엔드포인트 ####
    @Bean
    @Order(2)
    public SecurityFilterChain appChain(HttpSecurity http,
                                        GitHubOAuth2UserService githubOAuth2UserService,
                                        GoogleOAuth2UserService googleOAuth2UserService) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS 적용
                .csrf(csrf -> csrf.disable())
                .httpBasic(hb -> hb.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
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
                        .requestMatchers(HttpMethod.GET,
                                "/api/DiFF/attachment/**",
                                "/api/DiFF/reply/**",
                                "/api/DiFF/article/**",
                                "/api/DiFF/github/**"
                        ).permitAll()
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
                                    String rid = request.getClientRegistration().getRegistrationId();
                                    if ("github".equals(rid)) return githubOAuth2UserService.loadUser(request);
                                    if ("google".equals(rid)) return googleOAuth2UserService.loadUser(request);
                                    throw new OAuth2AuthenticationException("Unsupported provider: " + rid);
                                })
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("http://13.124.33.233:3000/DiFF/home/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(List.of(
                "https://diff.io.kr",
                "https://diff-front.fly.dev",
                "http://13.124.33.233:3000",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization", "REFRESH_TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
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
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> {
            System.out.println("🟢 WebSecurityCustomizer: ignoring /r2/**");
            web.ignoring().requestMatchers("/r2/**"); // 필터 자체를 우회
        };
    }
}
