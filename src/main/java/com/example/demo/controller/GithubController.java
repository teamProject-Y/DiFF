//package com.example.demo.controller;
//import com.example.demo.vo.Member;
//import com.example.demo.vo.Repository;
//import lombok.RequiredArgsConstructor;
//import org.apache.http.HttpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/DiFF/github")
//@RequiredArgsConstructor
//public class GithubController {
//
//    private String githubAccessToken;
//
//    private final WebClient gh = WebClient.builder()
//            .baseUrl("https://api.github.com")
//            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
//            .build();
//
//    Map<?,?> me = gh.get().uri("/user")
//            .headers(h -> h.setBearerAuth(githubAccessToken))
//            .retrieve().bodyToMono(Map.class).block();
//
//    @GetMapping("/repos")
//    public List<Repository> listRepos(@AuthenticationPrincipal Member me) {
//
//        String token = tokenService.getGithubToken(me.getId())
//                .orElseThrow(() -> new RuntimeException("깃허브 연동 안됨"));
//
//        var res = gh.get()
//                .uri(uri -> uri.path("/user/repos")
//                        .queryParam("per_page", 100)
//                        .queryParam("affiliation", "owner,collaborator,organization_member") // 내 접근 가능한 것
//                        .queryParam("visibility", "all")
//                        .build())
//                .headers(h -> h.setBearerAuth(token))
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<List<Map<String,Object>>>() {})
//                .block();
//
//        if (res == null) return List.of();
//
//        return res.stream().map(m -> {
//            Map<?,?> owner = (Map<?,?>) m.get("owner");
//            return new Repository(
//                    ((Number)m.get("id")).longValue(),
//                    (String)m.get("full_name"),
//                    Boolean.TRUE.equals(m.get("private")),
//                    (String)m.get("default_branch"),
//                    (String)m.get("html_url")
//            );
//        }).toList();
//    }
//}
