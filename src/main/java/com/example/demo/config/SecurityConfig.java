package com.example.demo.config;


import com.example.demo.service.GitHubOAuth2UserService;
import com.example.demo.service.GoogleOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private GitHubOAuth2UserService githubOAuth2UserService;

    @Autowired
    private GoogleOAuth2UserService googleOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/usr/home/main", "/usr/member/verifyGitUser",
                                "/resource/**","/css/**", "/js/**", "/images/**",
                                "/usr/member/login", "/usr/member/doLogin",
                                "/usr/member/join", "/usr/member/doJoin",
                                "/oauth2/**", "/login/**","/WEB-INF/jsp/usr/member/login.jsp",
                                "/upload", "/api/**"
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
                                .userService(this::selectOAuthService) // 서비스 선택 로직
                        )
                        .defaultSuccessUrl("/main", true)
                )
                 .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/usr/member/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
        );
        return http.build();
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