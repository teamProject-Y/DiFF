package com.example.demo.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ==== 프로젝트 내부 클래스 (서비스/VO 등) ====
import com.example.demo.service.ArticleService;
import com.example.demo.service.RepositoryService;
import com.example.demo.service.DraftService;   // 쓰는 경우만

// 유틸/인터셉터(필요 시)
import com.example.util.Ut;

@RestController
@RequestMapping("/api/DiFF/article")
public class UsrArticleController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private DraftService draftService;
    @Autowired
    private MemberRepository memberRepository;

    UsrArticleController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> showList(
            @RequestParam(defaultValue = "repositoryId") Long repositoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int searchItem) {
        int itemsInAPage = 10;
        int limitFrom = (page - 1) * itemsInAPage;

        int totalCnt = articleService.getArticlesCnt(repositoryId, keyword, searchItem);
        int totalPage = (int) Math.ceil(totalCnt / (double) itemsInAPage);
        List<Article> articles = articleService.getArticles(repositoryId, keyword, searchItem, limitFrom, itemsInAPage);

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);
        result.put("totalCnt", totalCnt);
        result.put("totalPage", totalPage);
        result.put("page", page);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending(
            @RequestParam(defaultValue = "100") Integer count,
            @RequestParam(defaultValue = "30") Integer days) {
        System.out.println("📥 /api/DiFF/article/trending 요청 도착");

        System.out.println("-> count: " + count);
        System.out.println("-> days: " + days);

        List<Article> articles = articleService.getTrendingArticles(count, days);

        for (Article article : articles) {
            System.out.println(article.getTitle());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);

        return ResponseEntity.ok(result);
    }



//    @GetMapping("/write")
//    public ResultData<Map<String, Object>> showWriteForm(HttpServletRequest req, @RequestParam Long repositoryId) {
//        Rq rq = (Rq) req.getAttribute("rq");
//        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();
//
//        System.out.println("\n===== [GET] /article/write =====");
//        System.out.println("\uD83C\uDF54 memberId      = " + memberId);
//
//        Repository repo = repositoryService.getRepositoryByIdAndMember(repositoryId, memberId);
//        if (repo == null) {
//            return ResultData.from("F-403", "해당 리포지토리에 대한 권한이 없습니다.");
//        }
//
//        System.out.println("\uD83C\uDF54 repositoryId  = " + repositoryId);
//        System.out.println("\uD83C\uDF54 repositoryName = " + repo.getName());
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("repository", repo);
//        return ResultData.from("S-1", "작성 폼 로드 성공", data);
//    }

    @PostMapping("/doWrite")
    @ResponseBody
    public ResultData<Integer> doWrite(HttpServletRequest req,
                                       @RequestBody Draft draft) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();
        draft.setMemberId(memberId);

        System.out.println("\n===== [POST] /article/doWrite =====");
        System.out.println("memberId      = " + draft.getMemberId());
        System.out.println("title         = " + draft.getTitle());
        System.out.println("body.length   = " + (draft.getBody() != null ? draft.getBody().length() : null));
        System.out.println("checksum      = " + draft.getChecksum());
        System.out.println("repositoryId  = " + draft.getRepositoryId());

        if (draft.getRepositoryId() == null) {
            return ResultData.from("F-400", "repositoryId가 필요합니다.");
        }
        if (draft.getTitle() == null || draft.getTitle().trim().isEmpty()) {
            return ResultData.from("F-400", "제목을 입력하세요.");
        }
        if (draft.getBody() == null || draft.getBody().trim().isEmpty()) {
            return ResultData.from("F-400", "내용을 입력하세요.");
        }

        Repository repo = repositoryService.getRepositoryByIdAndMember(draft.getRepositoryId(), memberId);
        if (repo == null) {
            System.out.println("[FAIL] 권한 없음 / repo 미존재");
            return ResultData.from("F-403", "해당 리포지토리에 대한 권한이 없습니다.");
        }

        // 작성
        int wr = articleService.writeArticle(
                memberId,
                draft.getTitle(),
                draft.getBody(),
                draft.getChecksum(),
                draft.getRepositoryId()
        );

        return ResultData.from("S-1", "작성 성공", wr);
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

 }