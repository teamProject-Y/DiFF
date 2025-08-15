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
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(hb -> hb.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        // 완전 공개
                        .requestMatchers(
                                "/", "/usr/draft/**",
                                "/resource/**","/css/**", "/js/**", "/images/**", "/oauth2/**", "/login/**",
                                "/api/DiFF/home/main", "/usr/draft/verifyGitUser",

                                "/DiFF/member/login", "/DiFF/member/doLogin","/DiFF/article/**",
                                "/DiFF/member/join", "/DiFF/member/doJoin", "/DiFF/member/login?error=true",
                                "/upload","/gpt/test,","/usr/draft/mkDraft","/api/DiFF/article/doWrite",
                                // 회원 관련
                                "/api/DiFF/auth/**", "/api/DiFF/member/doJoin", "/api/DiFF/member/login", "/api/DiFF/auth/refresh",
                                "/api/DiFF/member/check/**",
                                "/DiFF/member/doJoin", "/DiFF/member/login?error=true",
                                "/api/DiFF/member/login", "/api/DiFF/member/doLogin"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/DiFF/attachment/**", "/api/DiFF/comment/**",
                                "/api/DiFF/post/**", "/api/DiFF/article/list",
                                "/api/DiFF/article/trending","/api/DiFF/article/drafts","/api/DiFF/article/doWrite"
                        ).permitAll()

                        // 나머지 전부 인증
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
                        .logoutSuccessUrl("http://localhost:3000/DiFF/home/main")
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

    // CORS: REFRESH_TOKEN 허용/노출
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var cfg = new org.springframework.web.cors.CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.addAllowedOriginPattern("http://localhost:3000");
        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");
        cfg.addExposedHeader("Authorization");
        cfg.addExposedHeader("REFRESH_TOKEN");
        cfg.addAllowedHeader("REFRESH_TOKEN"); // 요청 허용
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}