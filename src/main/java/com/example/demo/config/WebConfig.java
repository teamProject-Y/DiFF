package com.example.demo.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://diff-front.fly.dev",
                        "https://diff.io.kr",
                        "http://localhost:3000",
                        "http://127.0.0.1:3000"
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "REFRESH_TOKEN")
                .allowCredentials(true);
    }
}
