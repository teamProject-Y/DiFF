package com.example.demo.controller;

import com.example.demo.service.DraftService;
import com.example.demo.vo.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/usr/draft")
public class UsrDraftController {

    @Autowired
    private DraftService draftService;

    @PostMapping("/mkRepo")
    @ResponseBody
    public ResultData mkRepo(@RequestBody Map<String, Object> param) {

        int memberId = (Integer) param.get("memberId");
        String repoName = (String) param.get("repoName");
        String firstCommit = (String) param.get("firstCommit");

        boolean existsRepoName = draftService.existsByMemberIdAndRepoName(memberId, repoName);
        if(existsRepoName) return ResultData.from("F-1", "이미 존재하는 리포지토리 이름");

        draftService.makeRepository(memberId, repoName, firstCommit);
        int repoId = draftService.getLastInsertId();

        return ResultData.from("S-1", "리포지토리 생성", "repositoryID", repoId);
    }

    @PostMapping("/isUsableRepoName")
    @ResponseBody
    public ResultData isUsableRepoName(@RequestBody Map<String, Object> param) {

        int memberId = (Integer) param.get("memberId");
        String repoName = (String) param.get("repoName");

        boolean isUsableRepoName = draftService.existsByMemberIdAndRepoName(memberId, repoName);
        return ResultData.from("S-1", "리포지토리 이름 중복 여부", "가능", true);
    }

}