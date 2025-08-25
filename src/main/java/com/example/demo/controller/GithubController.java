package com.example.demo.controller;

import com.example.demo.service.OAuthAccountService;
import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.vo.Repository;
import com.example.demo.vo.Rq;
import com.example.demo.vo.ResultData;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/DiFF/github")
public class GithubController {

    private final WebClient github; // bean: baseUrl=https://api.github.com, ACCEPT 설정
    private final MemberService memberService;
    private final OAuthAccountService oAuthAccountService;

    @GetMapping("/repos")
    public ResultData<List<Repository>> listRepos(HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        System.out.println("🐳🐳 github token: " + token);

        List<Map<String, Object>> res;
        try {
            res = github.get()
                    .uri(uri -> uri.path("/user/repos")
                            .queryParam("per_page", 100)
                            .queryParam("affiliation", "owner,collaborator,organization_member")
                            .queryParam("visibility", "all")
                            .build())
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String,Object>>>() {})
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            return ResultData.from("F-2", "깃허브 API 호출 실패: " + e.getMessage());
        }

        if (res == null) return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");

        if (!res.isEmpty()) {
            System.out.println("sample keys: " + res.get(0).keySet());
            System.out.println("full_name: " + res.get(0).get("full_name"));
            System.out.println("html_url: " + res.get(0).get("html_url"));
        }

        List<Repository> repos = res.stream().map(m -> {
            Repository r = new Repository();
            r.setName((String) m.get("full_name"));
            r.setUrl((String) m.get("html_url"));
            r.setAPrivate(Boolean.TRUE.equals(m.get("private")));
            r.setDefaultBranch((String) m.get("default_branch"));
            return r;
        }).toList();

        return ResultData.from("S-1", "리포지토리 조회 성공", repos);
    }
}
