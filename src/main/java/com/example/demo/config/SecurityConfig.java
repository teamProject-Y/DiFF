package com.example.demo.config;


import com.example.demo.service.GitHubOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private GitHubOAuth2UserService gitHubOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/DiFF/home/main",
                                "/resource/**","/css/**", "/js/**", "/images/**",
                                "/DiFF/member/login", "/DiFF/member/doLogin",
                                "/DiFF/member/join", "/DiFF/member/doJoin",
                                "/oauth2/**", "/login/**","/upload","/api/**"
                        ).permitAll()
                        .anyRequest().authenticated() //
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
                        .loginPage("/DiFF/member/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(gitHubOAuth2UserService)
                        )
                        .defaultSuccessUrl("http://localhost:3000/DiFF/home/main", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/DiFF/member/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

}