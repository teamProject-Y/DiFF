package com.example.demo.controller;

import com.example.demo.service.DraftService;
import com.example.demo.service.GptService;
import com.example.demo.service.MemberService;
import com.example.demo.vo.Draft;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/usr/draft")
public class UsrDraftController {

    @Autowired
    private Rq rq;
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
        // System.out.println("repositoryId: " + repositoryId);
        System.out.println("lastChecksum: " + lastChecksum);
        System.out.println("diff:\n" + diff);

        if (diff == null || diff.trim().isEmpty()) {
            return ResultData.from("F-1", "diff 내용이 비어있습니다.");
        }

        String draft;
        try {
            draft = gptService.makeDraft(diff, repositoryId, memberId, lastChecksum);
        } catch (Exception e) {
            return ResultData.from("F-2", "초안 생성에 실패했습니다.", "error", e.getMessage());
        }

        return ResultData.from("S-1", "커밋 diff 수신 및 초안 생성에 성공했습니다.", "draft", draft);
    }

    @GetMapping("/drafts")
    public ResponseEntity<Map<String, Object>> getDrafts() {
        System.out.println("📥 /api/DiFF/article/drafts 요청 도착");

        Number memberIdNum = (Number) rq.getLoginedMemberId();
        Long memberId = memberIdNum.longValue();

        List<Draft> drafts = draftService.getDraftsByMember(memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("drafts", drafts);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/draft/{id}")
    public ResultData<Void> deleteDraft(HttpServletRequest req, @PathVariable Long id) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== [DELETE] /draft/" + id + " =====");
        System.out.println("memberId = " + memberId);
        System.out.println("id       = " + id);

        // 1) 존재 여부 확인
        Draft found = draftService.getDraftById(id);
        if (found == null) {
            System.out.println("[FAIL] draft not found: id=" + id);
            return ResultData.from("F-404", "해당 게시글이 존재하지 않습니다.");
        }

        // 2) 소유자 검증
        if (!found.getMemberId().equals(memberId)) {
            System.out.println("[FAIL] 권한 없음. owner=" + found.getMemberId() + ", me=" + memberId);
            return ResultData.from("F-403", "해당 게시글에 대한 권한이 없습니다.");
        }

        // 3) 삭제
        int rows = draftService.deleteDraft(id, memberId); // WHERE id=? AND memberId=?
        if (rows == 0) {
            System.out.println("[FAIL] delete 실패");
            return ResultData.from("F-500", "게시글 삭제 실패");
        }

        System.out.println("[OK] delete 성공");
        return ResultData.from("S-1", "게시글 삭제 성공");
    }


}