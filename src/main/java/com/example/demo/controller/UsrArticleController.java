package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/DiFF/article")
public class UsrArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DraftService draftService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ReactionService reactionService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> showList( HttpServletRequest req,
            @RequestParam(defaultValue = "repositoryId") Long repositoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int searchItem) {

        System.out.println("===== 📑 [Get] /api/DiFF/article/list =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        List<Article> articles = articleService.getArticles(repositoryId, keyword, searchItem, loginedMemberId);

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    @ResponseBody
    public ResultData<List<Article>> searchArticles(HttpServletRequest req,
                                                    @RequestParam String keyword) {
        System.out.println("\n===== 🔎 [GET] /api/DiFF/article/search =====");
        System.out.println("🔎 요청 keyword = " + keyword);

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        List<Article> results = articleService.searchArticles(keyword, loginedMemberId);

        System.out.println("🔎 검색 결과 수 = " + (results != null ? results.size() : 0));

        return ResultData.from("S-1", "검색 결과", "articles", results);
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending(HttpServletRequest req,
                                                           @RequestParam(defaultValue = "100") Integer count,
                                                           @RequestParam(defaultValue = "30") Integer days) {
        System.out.println("\n===== 👏 [GET] /api/DiFF/article/trending =====");

        System.out.println("👏 count: " + count + ", days: " + days);

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq != null ? rq.getLoginedMemberId() : null;

        List<Article> articles = articleService.getTrendingArticles(count, days, loginedMemberId);

        System.out.println("👏 trending 결과 수 = " + (articles != null ? articles.size() : 0));

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/doWrite")
    @ResponseBody
    public ResultData<Long> doWrite(HttpServletRequest req,
                                    @RequestBody Article draft) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();
        draft.setMemberId(loginedMemberId);

        if (draft.getIsPublic() == null) {
            draft.setIsPublic(true);
        }

        System.out.println("\n===== ✍️ [POST] /article/doWrite =====");
        System.out.println("️✍️ memberId: " + draft.getMemberId());
        System.out.println("✍️ checksum      = " + draft.getChecksum());
        System.out.println("✍️ repositoryId  = " + draft.getRepositoryId());
        System.out.println("✍️ draftId       = " + draft.getDraftId());

        // 유효성 검사
        if (draft.getRepositoryId() == null) {
            return ResultData.from("F-400", "repositoryId가 필요합니다.");
        }
        if (draft.getTitle() == null || draft.getTitle().trim().isEmpty()) {
            return ResultData.from("F-400", "제목을 입력하세요.");
        }
        if (draft.getBody() == null || draft.getBody().trim().isEmpty()) {
            return ResultData.from("F-400", "내용을 입력하세요.");
        }

        // 리포 권한 확인
        Repository repo = repositoryService.getRepositoryByIdAndMember(
                draft.getRepositoryId(),
                loginedMemberId
        );
        if (repo == null) {
            System.out.println("✍️ [FAIL] 권한 없음 / repo 미존재");
            return ResultData.from("F-403", "해당 리포지토리에 대한 권한이 없습니다.");
        }

        String checksum = null;
        if (draft.getDraftId() != null) {
            Draft savedDraft = draftService.getDraftById(draft.getDraftId());
            if (savedDraft == null) {
                return ResultData.from("F-404", "임시저장 글이 존재하지 않습니다.");
            }
            checksum = savedDraft.getChecksum();
        }

        Long articleId = articleService.writeArticle(
                loginedMemberId,
                draft.getTitle(),
                draft.getBody(),
                draft.getIsPublic(),
                checksum,
                draft.getRepositoryId(),
                draft.getDraftId(),
                draft.getDiffId()
        );

        return ResultData.from("S-1", "작성 성공", "articleId", articleId);
    }

    @GetMapping("/detail")
    public ResultData<Article> getArticle(HttpServletRequest req, @RequestParam Long id) {

        System.out.println("\n===== ✏️ [GET] /api/DiFF/article/detail?id=" + id + " =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        Article article = articleService.getArticleById(id, loginedMemberId);

        if(!article.getIsPublic() && article.getMemberId() != loginedMemberId){
            return ResultData.from("F-401", "비공개 게시물. 접근 권한이 없습니다.");
        }

        if (article == null) {
            return ResultData.from("F-404", "해당 게시글이 존재하지 않습니다.");
        }

        return ResultData.from("S-1", "게시글 조회 성공", article);
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResultData<Integer> modifyArticle(HttpServletRequest req, @RequestBody Article article) {

        System.out.println("\n===== 📝 [POST] /api/DiFF/article/modify =====");

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
        if (article.getIsPublic() == null) {
            // null 방지
            article.setIsPublic(oldArticle.getIsPublic());
        }

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

        System.out.println("\n===== 🗑️ [DELETE] /api/DiFF/article/" + id + " =====");

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
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();
        System.out.println("\n===== 🌀 [GET] /api/DiFF/article/followingArticleList =====");

        int itemsInAPage = 10;
        int limitFrom = (page - 1) * itemsInAPage;

        List<Article> followingArticles = articleService.getFollowingArticles(limitFrom, itemsInAPage, loginedMemberId);

        int totalCnt = articleService.getFollowingArticlesCnt(loginedMemberId, repositoryId, keyword, searchItem);
        int totalPage = (int) Math.ceil(totalCnt / (double) itemsInAPage);

        Map<String, Object> result = new HashMap<>();
        result.put("followingArticles", followingArticles);
        result.put("totalCnt", totalCnt);
        result.put("totalPage", totalPage);
        result.put("page", page);

        return ResponseEntity.ok(result);
    }

    // 좋아요
    @PostMapping("/like/{articleId}")
    public Map<String,Object> likeArticle(HttpServletRequest req, @PathVariable Long articleId) {

        System.out.println("\n===== 👍📑 [Post] /api/DiFF/article/like/{articleId} =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.like("article", articleId, memberId);

        System.out.println("👍📑 dolike success: " + row);

        return Map.of("relType","article",
                "relId",articleId,
                "liked",true,
                "count", reactionService.count("article", articleId));
    }

    // 취소
    @DeleteMapping("/like/{articleId}")
    public Map<String,Object> unlikeArticle(HttpServletRequest req, @PathVariable Long articleId) {

        System.out.println("\n===== 👍🗑️ [Delete] /api/DiFF/article/like/{articleId} =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.unlike("article", articleId, memberId);

        System.out.println("👍🗑️ dounlike success: " + row);

        return Map.of("relType","article",
                "relId",articleId,
                "liked",false,
                "count", reactionService.count("article", articleId));
    }

    // 개수
    @GetMapping("/like/{articleId}")
    public Map<String,Object> getArticleLike(HttpServletRequest req, @PathVariable Long articleId) {

        System.out.println("\n===== 👍🔢️ [Get] /api/DiFF/article/like/{articleId} =====");

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

        System.out.println("\n===== 👀🔢️ [Get] /api/DiFF/article/hits/{articleId} =====");
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        boolean success = articleService.increaseHits(articleId, memberId);

        return Map.of(
                "resultCode", success ? "S-1" : "F-1",
                "msg", success ? "조회수가 증가했습니다." : "이미 조회한 게시글입니다."
        );
    }

}