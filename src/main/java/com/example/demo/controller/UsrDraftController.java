package com.example.demo.controller;

import com.example.demo.service.DraftService;
import com.example.demo.service.GptService;
import com.example.demo.service.MemberService;
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

    @Autowired
    private GptService gptService;

    @Autowired
    private MemberService memberService;


    @PostMapping("/verifyGitUser")
    @ResponseBody
    public ResultData verifyGitUser(@RequestBody Map<String, String> requestMap) {

        String email = requestMap.get("email");
        Integer verifiedMemberId = memberService.isVerifiedUser(email);

        if(verifiedMemberId != null) {
            System.out.println("git email로 찾은 memberID: " + verifiedMemberId);
            return ResultData.from("S-1", "사용자 인증 완료", "인증된 사용자 id", verifiedMemberId);
        }else {
            System.err.println("git email로 찾은 member 없음");
            return ResultData.from("F-1", "사용자 인증 실패");
        }
    }

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

    @PostMapping("/mkDraft")
    @ResponseBody
    public ResultData<String> receiveDiff(@RequestBody Map<String, Object> param) {
        System.out.println("receiveDiff 메서드 진입" );
        System.out.println("🍔param: " + param);

        Number memberIdNum = (Number) param.get("memberId");
        Number repositoryIdNum = (Number) param.get("repositoryId");

        Long memberId = memberIdNum.longValue();
        Long repositoryId = repositoryIdNum.longValue();
        String lastChecksum = (String) param.get("lastChecksum");
        String diff = (String) param.get("diff");
        System.out.println("memberId: " + memberId);
        System.out.println("lastChecksum: " + lastChecksum);
        System.out.println("diff:\n" + diff);

        if (diff == null || diff.trim().isEmpty()) {
            return ResultData.from("F-1", "diff 내용이 비어있습니다.");
        }

        String summary;
        try {
            summary = gptService.summarizeDiff(diff, memberId, repositoryId, lastChecksum);
        } catch (Exception e) {
            return ResultData.from("F-2", "GPT 요약 실패", "error", e.getMessage());
        }

        return ResultData.from("S-1", "커밋 diff 수신 및 요약/저장 완료", "summary", summary);
    }


}