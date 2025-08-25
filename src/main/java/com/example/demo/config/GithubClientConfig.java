package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GithubClientConfig {

    @Bean(name = "github")
    public WebClient github(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "DiFF-App/1.0")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}
