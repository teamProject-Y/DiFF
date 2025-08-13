package com.example.demo.controller;

import com.example.demo.service.RepositoryService;
import com.example.demo.vo.Repository;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/repository")
@RequiredArgsConstructor
public class UsrRepositoryController {

    @Autowired
    private Rq rq;

    @Autowired
    private  RepositoryService repositoryService;

    @GetMapping("/my")
    public ResultData<Map<String, Object>> getMyRepositories(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== [GET] /api/DiFF/repository/my =====");
        System.out.println("memberId = " + memberId);

        List<Repository> repos = repositoryService.getRepositoriesByMemberId(memberId);
        System.out.println("repo count = " + repos.size());
        for (Repository r : repos) {
            System.out.println(" - repo[id=" + r.getId() + ", name=" + r.getName() + "]");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("repositories", repos);

        return ResultData.from("S-1", "내 리포지토리 목록", data);
    }
}
