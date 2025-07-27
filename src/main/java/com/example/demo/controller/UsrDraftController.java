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
        System.out.println(memberId);
        System.out.println(repoName);
        System.out.println(firstCommit);

        boolean existsRepoName = draftService.existsByMemberIdAndRepoName(memberId, repoName);
        if(!existsRepoName) return ResultData.from("F-1", "이미 존재하는 리포지토리 이름");

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
        if(isUsableRepoName){
            return ResultData.from("S-1", "리포지토리 이름 중복 여부", "가능", isUsableRepoName);
        }else {
            return ResultData.from("F-1", "리포지토리 이름 중복 여부", "불가능", isUsableRepoName);
        }
    }

    @PostMapping("/receiveDiff")
    @ResponseBody
    public ResultData<String> receiveDiff(@RequestBody Map<String, Object> param) {
        int memberId = (Integer) param.get("memberId");
        String commitHash = (String) param.get("commitHash");
        String diff = (String) param.get("diff");

        System.out.println("👤 memberId: " + memberId);
        System.out.println("🔨 commitHash: " + commitHash);
        System.out.println("📦 diff:\n" + diff);

        // 👉 여기서 GPT 요청 또는 DB 저장 처리 로직을 이어서 넣을 수 있음
        // 예: gptService.summarizeDiff(diff), draftService.saveDiff(...)

        return ResultData.from("S-1", "커밋 diff 수신 완료", "diff", diff);
    }

}