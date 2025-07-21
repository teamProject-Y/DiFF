package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.ArticleService;
import com.example.demo.service.CommentService;
import com.example.demo.service.ReactionService;
import com.example.demo.vo.Article;
import com.example.demo.vo.Comment;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;

import jakarta.servlet.http.HttpServletRequest;
import util.Ut;

@Controller
public class UsrArticleController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private CommentService commentService;

    UsrArticleController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    // 액션메서드
//    @RequestMapping("/usr/article/detail")
//    public String getArticle(Model model, HttpServletRequest req, int id) {
//
//        return "/usr/article/detail";
//    }

//    @RequestMapping("/usr/article/doGoodReaction")
//    @ResponseBody
//    public ResultData doGoodReaction(HttpServletRequest req,@RequestParam int id,@RequestParam(name="relTypeCode") String relTypeCode) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//
//        ResultData doReactionRd;
//
//        if(rq.getLoginedMemberId() <= 0) {
//            return null;
//        }
//
//        if(relTypeCode.equals("article")) {
//            doReactionRd = doGoodToArticle((int) rq.getLoginedMemberId(), id);
//
//        }else {
//            doReactionRd = articleService.userReaction((int) rq.getLoginedMemberId(), id);
//            // comment 업데이트 함수 구현
//        }
//
//        return doReactionRd;
//    }

//    private ResultData doGoodToArticle(int loginedMemberId, int articleId) {
//
//        Article article;
//        ResultData doReactionRd = articleService.userReaction(loginedMemberId, articleId);
//
//        if(doReactionRd == null) {
//            // 삽입
//            reactionService.doGoodReaction((int) rq.getLoginedMemberId(), articleId, "article");
//            article = articleService.getArticleForPrint(articleId, loginedMemberId);
//            doReactionRd = ResultData.from("S-1","reaction 성공", "싫어요", article);
//
//        }else if((int)doReactionRd.getData1() == 1) {
//            // 취소
//            reactionService.doChangeReaction((int) rq.getLoginedMemberId(), articleId, 0, "article");
//            article = articleService.getArticleForPrint(articleId, loginedMemberId);
//            doReactionRd = doReactionRd.newData(doReactionRd, "좋아요 취소",article);
//
//        }else {
//            // 수정
//            reactionService.doChangeReaction((int) rq.getLoginedMemberId(), articleId, 1, "article");
//            article = articleService.getArticleForPrint(articleId, loginedMemberId);
//            doReactionRd = doReactionRd.newData(doReactionRd, "좋아요로 수정", article);
//        }
//
//        return doReactionRd;
//
//    }

    // 수정 필요
//    private ResultData doGoodToComment(int loginedMemberId, int commentId) {
//
//        Article article;
//        ResultData doReactionRd = articleService.userReaction(loginedMemberId, commentId);
//
//        if(doReactionRd == null) {
//            // 삽입
//            reactionService.doGoodReaction((int) rq.getLoginedMemberId(), commentId, "commentId");
//            article = articleService.getArticleForPrint(commentId, loginedMemberId);
//            doReactionRd = ResultData.from("S-1","reaction 성공", "싫어요", article);
//
//        }else if((int)doReactionRd.getData1() == 1) {
//            // 취소
//            reactionService.doChangeReaction((int) rq.getLoginedMemberId(), commentId, 0, "commentId");
//            article = articleService.getArticleForPrint(commentId, loginedMemberId);
//            doReactionRd = doReactionRd.newData(doReactionRd, "좋아요 취소",article);
//
//        }else {
//            // 수정
//            reactionService.doChangeReaction((int) rq.getLoginedMemberId(), commentId, 1, "commentId");
//            article = articleService.getArticleForPrint(commentId, loginedMemberId);
//            doReactionRd = doReactionRd.newData(doReactionRd, "좋아요로 수정", article);
//        }
//
//        return doReactionRd;
//
//    }


//    @RequestMapping("/usr/article/doIncHits")
//    @ResponseBody
//    public ResultData doIncHits(int id) {
//
//        ResultData increaseHitCountRd = articleService.doIncHits(id);
//
//        if (increaseHitCountRd.isFail()) {
//            return increaseHitCountRd;
//        }
//
//        return ResultData.from(increaseHitCountRd.getResultCode(), increaseHitCountRd.getMsg(),
//                "hitCount", articleService.getHits(id), "article id", id);
//    }
//
//    @RequestMapping("/usr/article/doCommentWrite")
//    @ResponseBody
//    public String doCommentWrite(HttpServletRequest req, int id, String body) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//
//        if (Ut.isEmpty(body) || body.trim().length() == 0)
//            return Ut.jsHistoryBack("F-1", "내용 안썻어");
//
//        int commentId = commentService.doCommentWrtie("article", id, (int) rq.getLoginedMemberId(), body);
//
//        return Ut.jsReplace(Ut.f("../article/detail?id=%d", id));
//    }
//
//    @RequestMapping("/usr/article/list")
//    public String getArticles(Model model, String keyword,
//                              @RequestParam(defaultValue = "1") int searchItem, @RequestParam(defaultValue = "1") int page) {
//
//        // pagenation
//        int itemsInAPage = 10; // 한페이지에 보여줄 게시글 수
//        int limitFrom = (page - 1) * itemsInAPage; // 몇번부터
//
//        int totalCnt =articleService.getArticlesCnt(keyword, searchItem); // 검색한 article의 총 개수
//        int totalPage = (int) Math.ceil(totalCnt / (double) itemsInAPage); // article 나누기 page
//
//        List<Article> articles = articleService.getArticles(keyword, searchItem, limitFrom, itemsInAPage);
//
//        model.addAttribute("articles", articles);
//        model.addAttribute("keyword", keyword);
//        model.addAttribute("searchItem", searchItem);
//
//        model.addAttribute("page", page);
//        model.addAttribute("totalCnt", totalCnt);
//        model.addAttribute("totalPage", totalPage);
//
//        return "/usr/article/list";
//    }
//
//    @RequestMapping("/usr/article/write")
//    public String write() {
//        return "/usr/article/write";
//    }
//
//    @RequestMapping("/usr/article/doWrite")
//    @ResponseBody
//    public String doWrite(HttpServletRequest req, String title, String body) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//
//        if (Ut.isEmpty(title))
//            return Ut.jsHistoryBack("F-2", "제목을 작성하세요");
//        if (Ut.isEmpty(body))
//            return Ut.jsHistoryBack("F-2", "내용을 작성하세요");
//
//        Article article = articleService.writeArticle(title, body, (int) rq.getLoginedMemberId());
//        int id = articleService.getLastInsertId();
//
//        return Ut.jsReplace("S-1", Ut.f("게시글 %d 번 작성 완료", id), Ut.f("../article/detail?id=%d", id));
//    }
//
//    @RequestMapping("/usr/article/modify")
//    public String modify(Model model, HttpServletRequest req, int id) throws IOException { // , String title, String body
//
//        Rq rq = (Rq) req.getAttribute("rq");
//
//        Article article = articleService.getArticleById(id);
//
//        // 권한이 없다면 이전 페이지로 돌아가야하고,
//        if (article == null) {
//            rq.printHistoryBack(Ut.f("%d 번 게시물은 존재하지 않습니다.", id));
//        }
//
//        if (article.getMemberId() != rq.getLoginedMemberId()) {
//            rq.printHistoryBack("권한이 없습니다.");
//        }
//
//        model.addAttribute("article", article);
//
//        // 권한이 있다면 수정페이지로 돌아가야한다.
//        return "/usr/article/modify";
//    }
//
//    // 로그인 체크 -> 유무 체크 -> 권한 체크
//    @RequestMapping("/usr/article/doModify")
//    @ResponseBody
//    public String doModify(int id, String title, String body) {
//
//        articleService.modifyArticle(id, title, body);
//
//        return Ut.jsReplace("S-1", Ut.f("%d 번 게시물 수정 완료", id), Ut.f("../article/detail?id=%d", id));
//    }
//
//    @RequestMapping("/usr/article/doDelete")
//    @ResponseBody
//    public String doDelete(HttpServletRequest req, int id) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//
//        Article article = articleService.getArticleById(id);
//
//        if (article == null) {
//            ResultData.from("F-1", Ut.f("%d번 게시글은 없거던", id));
//            return Ut.jsHistoryBack("F-1", Ut.f("%d 번 게시물은 없으시오", id));
//        }
//
//        if (article.getMemberId() != rq.getLoginedMemberId()) {
//            ResultData.from("F-A", "권한 없음");
//            return Ut.jsHistoryBack("F-A", Ut.f("%d번 게시물에 대한 권한이 업습", id));
//        }
//
//        articleService.deleteArticle(id);
//
//        return Ut.jsReplace("S-1", Ut.f("%d번 게시물 삭제 완료", id), "../article/list");
//    }
}