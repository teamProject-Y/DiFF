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
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

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

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           GitHubOAuth2UserService githubOAuth2UserService,
                                           GoogleOAuth2UserService googleOAuth2UserService, JwtTokenProvider jwtTokenProvider) throws Exception {
        http
                // jwt
                .csrf(csrf -> csrf.disable())
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
                                "/", "/DiFF/home/main", "/DiFF/member/verifyGitUser", "/DiFF/draft/**",
                                "/resource/**","/css/**", "/js/**", "/images/**",
                                "/usr/member/login", "/usr/member/doLogin",
                                "/usr/member/join", "/usr/member/doJoin", "/usr/member/login?error=true",
                                "/oauth2/**", "/login/**","/WEB-INF/jsp/usr/member/login.jsp",
                                "/upload","/gpt/test,","/usr/draft/receiveDiff"

                        ).permitAll()
                        .anyRequest().authenticated() //
                )
                .formLogin(form -> form
                        .loginPage("/DiFF/member/login")
                        .loginProcessingUrl("/DiFF/member/doLogin")
                        .usernameParameter("loginId")
                        .passwordParameter("loginPw")
                        .defaultSuccessUrl("http://localhost:3000/", true)
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
//    private OAuth2User selectOAuthService(OAuth2UserRequest request,
//                                          GitHubOAuth2UserService githubOAuth2UserService,
//                                          GoogleOAuth2UserService googleOAuth2UserService) {
//        String registrationId = request.getClientRegistration().getRegistrationId();
//        if ("github".equals(registrationId)) {
//            return githubOAuth2UserService.loadUser(request);
//        } else if ("google".equals(registrationId)) {
//            return googleOAuth2UserService.loadUser(request);
//        }
//        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
//    }
}