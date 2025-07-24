package com.example.demo.config;


import com.example.demo.service.GitHubOAuth2UserService;
import com.example.demo.service.GoogleOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;

    @Autowired
    private GitHubOAuth2UserService githubOAuth2UserService;

    @Autowired
    private GoogleOAuth2UserService googleOAuth2UserService;

    @Bean
    public HttpFirewall allowSemicolonFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true); // 세미콜론 허용
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
        return (web) -> web.httpFirewall(firewall);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 백엔드 API v1/v2
                        // 승인된 사용자 -> 로그인하여 토큰을 발급받은 사용자 요청
                        // 순서 중요

                        //관리자 관련 모든 요청에 대해 승인된 사용자 중 ADMIN 권한이 있는 사용자만 허용
                        .requestMatchers("/api/v1/admin/**", "/api/v2/admin/**").hasRole("ADMIN")
                        // 회원가입 및 로그인 관련 모든 요청에 대해 아무나 승인
                        .requestMatchers("/api/v1/auth/**",  "/api/v2/auth/**").permitAll()
                        // 중복체크 관련 모든 요청에 대해 아무나 허용
                        .requestMatchers("/api/v1/user/check/**","/api/v2/user/check/**").permitAll()
                        // 유저정보 관련 모든 요청에 대해 승인된 사용자만 허용
                        .requestMatchers("/api/v1/user/**", "/api/v2/user/**").authenticated()
                        // 첨부파일 관련 GET 요청에 대해 아무나 승인
                        .requestMatchers(HttpMethod.GET, "/api/v1/attachment/**",
                                "/api/v2/attachment/**").permitAll()
                        // 댓글 관련 GET 요청에 대해 아무나 승인
                        .requestMatchers(HttpMethod.GET, "/api/v1/comment/**",
                                "/api/v2/comment/**").permitAll()
                        // 게시글 관련 GET 요청에 대해 아무나 승인
                        .requestMatchers(HttpMethod.GET, "/api/v1/post/**",
                                "/api/v2/post/**").permitAll()
                        // 기타 모든 요청에 대해 승인된 사용자만 허용
                        .requestMatchers("/api/v1/**", "/api/v2/**").authenticated()

                        // 기존에 있던 것들
                        .requestMatchers(
                                "/", "/DiFF/home/main", "/DiFF/member/verifyGitUser",
                                "/", "/usr/home/main", "/usr/member/verifyGitUser", "/usr/draft/**",
                                "/resource/**","/css/**", "/js/**", "/images/**",
                                "/DiFF/member/login", "/DiFF/member/doLogin",
                                "/DiFF/member/join", "/DiFF/member/doJoin",
                                "/oauth2/**", "/login/**",
                                "/upload", "/api/**", "/error/**",
                                "/usr/member/login", "/usr/member/doLogin",
                                "/usr/member/join", "/usr/member/doJoin", "/usr/member/login?error=true",
                                "/oauth2/**", "/login/**",
                                "/upload"
                        ).permitAll()
                        .anyRequest().authenticated() // 그 외는 무조건 인증
                )
                .formLogin(form -> form
                        .loginPage("/DiFF/member/login")
                        .loginProcessingUrl("/DiFF/member/doLogin")
                        .usernameParameter("loginId")
                        .passwordParameter("loginPw")
                        .defaultSuccessUrl("http://localhost:3000/", true)
                        .failureUrl("/DiFF/member/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(this::selectOAuthService) // 서비스 선택 로직
                        )
                        .defaultSuccessUrl("http://localhost:3000/", true)
                )
                 .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("http://localhost:3000/DiFF/member/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
        )
        // JWT 필터 추가 (토큰 유효성 검사)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    // password를 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private OAuth2User selectOAuthService(OAuth2UserRequest request) {
        String registrationId = request.getClientRegistration().getRegistrationId();
        if ("github".equals(registrationId)) {
            return githubOAuth2UserService.loadUser(request);
        } else if ("google".equals(registrationId)) {
            return googleOAuth2UserService.loadUser(request);
        }
        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }
}