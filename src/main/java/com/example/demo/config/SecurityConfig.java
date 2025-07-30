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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
        firewall.setAllowSemicolon(true); // 세미콜론 허용
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
        return (web) -> web.httpFirewall(firewall);
    }

//    @Bean
//   public PasswordEncoder passwordEncoder() {
//       return new BCryptPasswordEncoder();
//   }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           GitHubOAuth2UserService githubOAuth2UserService,
                                           GoogleOAuth2UserService googleOAuth2UserService, JwtTokenProvider jwtTokenProvider) throws Exception {
        http
                // jwt
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(hb -> hb.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api//DiFF/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/DiFF/auth/**", "/api/DiFF/member/login").permitAll()
                        .requestMatchers("/api//DiFF/member/check/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/DiFF/attachment/**",
                                "/api/DiFF/comment/**",
                                "/api/DiFF/post/**")
                        .permitAll()
                        .requestMatchers("/api/DiFF/member/**").authenticated()
                        .requestMatchers("/api/DiFF/**").authenticated()
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/DiFF/admin/**", "/api/v2/diff/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/DiFF/auth/**",  "/api/v2/diff/auth/**").permitAll()
                        .requestMatchers("/api/v1/DiFF/member/check/**", "/api/v2/diff/member/check/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/diff/attachment/**", "/api/v2/diff/attachment/**",
                                "/api/v1/diff/comment/**",    "/api/v2/diff/comment/**",
                                "/api/v1/diff/post/**",       "/api/v2/diff/post/**")
                        .permitAll()
                        .requestMatchers("/api/v1/diff/member/**", "/api/v2/diff/member/**").authenticated()
                        .requestMatchers("/api/v1/diff/**",      "/api/v2/diff/**").authenticated()
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/DiFF/home/main", "/usr/draft/verifyGitUser", "/usr/draft/**",
                                "/resource/**","/css/**", "/js/**", "/images/**",
                                "/DiFF/member/login", "/DiFF/member/doLogin",
                                "/DiFF/member/join", "/DiFF/member/doJoin", "/DiFF/member/login?error=true",
                                "/oauth2/**", "/login/**",
                                "/upload","/gpt/test,","/usr/draft/receiveDiff"

                        ).permitAll()
                        .anyRequest().authenticated() //
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler())
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
//                .formLogin(form -> form
//                        .loginPage("/DiFF/member/login")
//                        .loginProcessingUrl("/DiFF/member/login")
//                        .usernameParameter("loginId")
//                        .passwordParameter("loginPw")
//                        .defaultSuccessUrl("http://localhost:3000/", true)
//                        .failureUrl("/DiFF/member/login?error=true")
//                        .permitAll()
//                )

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
                        .successHandler(oAuth2SuccessHandler))
                // .defaultSuccessUrl("http://localhost:3000/DiFF/home/main", true))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("http://localhost:3000/DiFF/home/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }

    // 401 처리 핸들러 (익명)
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(401);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"로그인 필요\"}");
        };
    }

    // 403 처리 핸들러 (권한 없음)
    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(403);
            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"권한 없음\"}");
        };
    }

}