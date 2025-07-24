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

//    @Autowired
//    private GitHubOAuth2UserService githubOAuth2UserService;
//
//    @Autowired
//    private GoogleOAuth2UserService googleOAuth2UserService;

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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           GitHubOAuth2UserService githubOAuth2UserService,
                                           GoogleOAuth2UserService googleOAuth2UserService) throws Exception {
        http
                // jwt
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/**", "/api/v2/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/**",  "/api/v2/auth/**").permitAll()
                        .requestMatchers("/api/v1/user/check/**", "/api/v2/user/check/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/attachment/**", "/api/v2/attachment/**",
                                "/api/v1/comment/**",    "/api/v2/comment/**",
                                "/api/v1/post/**",       "/api/v2/post/**")
                        .permitAll()
                        .requestMatchers("/api/v1/user/**", "/api/v2/user/**").authenticated()
                        .requestMatchers("/api/v1/**",      "/api/v2/**").authenticated()
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                                "/", "/usr/home/main", "/usr/member/verifyGitUser", "/usr/draft/**",
                                "/resource/**","/css/**", "/js/**", "/images/**",
                                "/usr/member/login", "/usr/member/doLogin",
                                "/usr/member/join", "/usr/member/doJoin", "/usr/member/login?error=true",
                                "/oauth2/**", "/login/**","/WEB-INF/jsp/usr/member/login.jsp",
                                "/upload"
                        ).permitAll()
                        .anyRequest().authenticated() //
                )
                .formLogin(form -> form
                        .loginPage("/usr/member/login")
                        .loginProcessingUrl("/usr/member/doLogin")
                        .usernameParameter("loginId")
                        .passwordParameter("loginPw")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/usr/member/login?error=true")
                        .permitAll()
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
                        .defaultSuccessUrl("http://localhost:3000/DiFF/home/main", true))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/usr/member/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }
    private OAuth2User selectOAuthService(OAuth2UserRequest request,
                                          GitHubOAuth2UserService githubOAuth2UserService,
                                          GoogleOAuth2UserService googleOAuth2UserService) {
        String registrationId = request.getClientRegistration().getRegistrationId();
        if ("github".equals(registrationId)) {
            return githubOAuth2UserService.loadUser(request);
        } else if ("google".equals(registrationId)) {
            return googleOAuth2UserService.loadUser(request);
        }
        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
    }
}