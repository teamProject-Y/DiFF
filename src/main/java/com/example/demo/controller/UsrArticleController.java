package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private ReactionService reactionService;

    UsrArticleController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> showList(
            @RequestParam(defaultValue = "repositoryId") Long repositoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int searchItem) {

        System.out.println("list 진입");

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

    @GetMapping("/search")
    public ResultData<List<Article>> searchArticles(@RequestParam String keyword) {
        System.out.println("📥 /article/search 요청 keyword=" + keyword);

        List<Article> results = articleService.searchArticles(keyword);

        return ResultData.from("S-1", "검색 결과", "articles", results);
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending(
            @RequestParam(defaultValue = "100") Integer count,
            @RequestParam(defaultValue = "30") Integer days) {
        System.out.println("📥 /api/DiFF/article/trending 요청 도착");

        System.out.println("-> count: " + count);
        System.out.println("-> days: " + days);

        List<Article> articles = articleService.getTrendingArticles(count, days);

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/doWrite")
    @ResponseBody
    public ResultData<Integer> doWrite(HttpServletRequest req,
                                       @RequestBody Article draft) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();
        draft.setMemberId(loginedMemberId);

        System.out.println("\n===== 🐶🐶 [POST] /article/doWrite =====");
        System.out.println("memberId      = " + draft.getMemberId());
        System.out.println("title         = " + draft.getTitle());
        System.out.println("body.length   = " + (draft.getBody() != null ? draft.getBody().length() : null));
        System.out.println("checksum      = " + draft.getChecksum());
        System.out.println("repositoryId  = " + draft.getRepositoryId());
        System.out.println("draftId       = " + draft.getDraftId());

        if (draft.getRepositoryId() == null) {
            return ResultData.from("F-400", "repositoryId가 필요합니다.");
        }
        if (draft.getTitle() == null || draft.getTitle().trim().isEmpty()) {
            return ResultData.from("F-400", "제목을 입력하세요.");
        }
        if (draft.getBody() == null || draft.getBody().trim().isEmpty()) {
            return ResultData.from("F-400", "내용을 입력하세요.");
        }

        Repository repo = repositoryService.getRepositoryByIdAndMember(
                draft.getRepositoryId(),
                loginedMemberId
        );
        if (repo == null) {
            System.out.println("[FAIL] 권한 없음 / repo 미존재");
            return ResultData.from("F-403", "해당 리포지토리에 대한 권한이 없습니다.");
        }

        int wr = articleService.writeArticle(
                loginedMemberId,
                draft.getTitle(),
                draft.getBody(),
                draft.getChecksum(),
                draft.getRepositoryId(),
                draft.getDraftId()
        );

        return ResultData.from("S-1", "작성 성공", wr);
    }


    @GetMapping("/detail")
    public ResultData<Article> getArticle(HttpServletRequest req, @RequestParam Long id) {

        System.out.println("\n===== 🐶🐶 [GET] /api/DiFF/article/detail?id=" + id + " =====");
        System.out.println("detail 진입" + id);

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        Article article = articleService.getArticleById(id, loginedMemberId);

        if (article == null) {
            return ResultData.from("F-404", "해당 게시글이 존재하지 않습니다.");
        }

        System.out.println(article.getExtra__writer());

        return ResultData.from("S-1", "게시글 조회 성공", article);
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResultData<Integer> modifyArticle(HttpServletRequest req, @RequestBody Article article) {

        System.out.println("\n===== 🐶🐶 [POST] /api/DiFF/article/modify =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        if (loginedMemberId == null) {
            return ResultData.from("F-1", "로그인 후 이용 가능합니다.");
        }

        Article oldArticle = articleService.getArticleById(article.getId(), loginedMemberId);

        if (oldArticle == null) {
            return ResultData.from("F-2", "존재하지 않는 게시글입니다.");
        }

        if (!oldArticle.getMemberId().equals(loginedMemberId) || !article.isUserCanModify()) {
            return ResultData.from("F-3", "권한이 없습니다. 본인 글만 수정 가능합니다.");
        }

        article.setUpdateDate(LocalDateTime.now());
        int affectedRow = articleService.modifyArticle(article);

        if (affectedRow == 0) {
            return ResultData.from("F-4", "수정 실패", 0);
        }
        return ResultData.from("S-1", "수정 성공", affectedRow);
    }

    @DeleteMapping("/{id}")
    public ResultData<Integer> deleteArticle(
            HttpServletRequest req, @PathVariable Long id) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== \uD83D\uDC36 \uD83D\uDC36 [DELETE] /api/DiFF/article/" + id + " =====");

        Article article = articleService.getArticleById(id, loginedMemberId);
        if (article == null) {
            return ResultData.from("F-404", "해당 게시글이 존재하지 않습니다.");
        }
        if (!article.getMemberId().equals(loginedMemberId)) {
            return ResultData.from("F-403", "해당 게시글에 대한 권한이 없습니다.");
        }

        int rows = articleService.deleteArticle(id, loginedMemberId);
        if (rows == 0) {
            return ResultData.from("F-500", "게시글 삭제 실패");
        }

        return ResultData.from("S-1", "게시글 삭제 성공", rows);
    }

    @GetMapping("/followingArticleList")
    public ResponseEntity<Map<String, Object>> showFollowingArticleList(
            HttpServletRequest req,
            @RequestParam(required = false, defaultValue = "0") Long repositoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int searchItem) {

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();
        System.out.println("\n===== 🐶🐶 [GET] /api/DiFF/article/followingArticleList =====");

        int itemsInAPage = 10;
        int limitFrom = (page - 1) * itemsInAPage;

        List<Article> followingArticles = articleService.getFollowingArticles(memberId, limitFrom, itemsInAPage);

        int totalCnt = articleService.getFollowingArticlesCnt(memberId, repositoryId, keyword, searchItem);
        int totalPage = (int) Math.ceil(totalCnt / (double) itemsInAPage);

        Map<String, Object> result = new HashMap<>();
        result.put("followingArticles", followingArticles);
        result.put("totalCnt", totalCnt);
        result.put("totalPage", totalPage);
        result.put("page", page);

        System.out.println("asas");

        return ResponseEntity.ok(result);
    }

    // 좋아요
    @PostMapping("/like/{articleId}")
    public Map<String,Object> likeArticle(HttpServletRequest req, @PathVariable Long articleId) {

        System.out.println("put/like/article 진입");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.like("article", articleId, memberId);

        System.out.println("dolike success: " + row);

        return Map.of("relType","article",
                "relId",articleId,
                "liked",true,
                "count", reactionService.count("article", articleId));
    }

    // 취소
    @DeleteMapping("/like/{articleId}")
    public Map<String,Object> unlikeArticle(HttpServletRequest req, @PathVariable Long articleId) {

        System.out.println("delete/like/article 진입");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.unlike("article", articleId, memberId);

        System.out.println("dounlike success: " + row);

        return Map.of("relType","article",
                "relId",articleId,
                "liked",false,
                "count", reactionService.count("article", articleId));
    }

    // 개수
    @GetMapping("/like/{articleId}")
    public Map<String,Object> getArticleLike(HttpServletRequest req, @PathVariable Long articleId) {

        System.out.println("get/like/article 진입");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        return Map.of("relType","article",
                "relId",articleId,
                "liked", reactionService.isLiked("article", articleId, memberId),
                "count", reactionService.count("article", articleId));
    }

    @PostMapping("/hits/{articleId}")
    @ResponseBody
    public Map<String, Object> increaseHits(HttpServletRequest req, @PathVariable Long articleId) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        boolean success = articleService.increaseHits(articleId, memberId);

        return Map.of(
                "resultCode", success ? "S-1" : "F-1",
                "msg", success ? "조회수가 증가했습니다." : "이미 조회한 게시글입니다."
        );
    }

}