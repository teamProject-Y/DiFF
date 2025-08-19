package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;

import com.example.demo.repository.ReactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.ArticleRepository;
import com.example.demo.vo.Article;
import com.example.demo.vo.ResultData;

import com.example.util.Ut;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public int getLastInsertId() {
        return articleRepository.getLastInsertId();
    }

    public int getArticlesCnt(Long repository, String keyword, int searchItem) {
        return articleRepository.getArticlesCnt(repository, keyword, searchItem);
    }

    public List<Article> getArticles(Long repositoryId, String keyword, int searchItem, int limitFrom, int itemsInAPage) {
        return articleRepository.getArticles(repositoryId, keyword, searchItem, limitFrom, itemsInAPage);
    }

    public List<Article> getTrendingArticles(Integer count, Integer days) {
        return articleRepository.getTrendingArticles(count, days);
    }

    public int writeArticle(Long memberId, String title, String body, String checksum, Long repositoryId) {
        return articleRepository.writeArticle(memberId, title, body, checksum, repositoryId);
    }
    
    public Article getArticleById(Long id, Long loginedMemberId) {

        Article article = articleRepository.getArticleById(id);

        updateForPrintData(loginedMemberId, article);

        return article;
    }

    private void updateForPrintData(Long loginedMemberId, Article article) {
        if (article == null) return;

        ResultData userCanModifyRd = userCanModify(loginedMemberId, article);
        article.setUserCanModify(userCanModifyRd.isSuccess());
        System.err.println("📌 userCanModifyRd: "  + userCanModifyRd.isSuccess());

        ResultData userCanDeleteRd = userCanDelete(loginedMemberId, article);
        article.setUserCanDelete(userCanDeleteRd.isSuccess());
        System.err.println("📌 userCanDeleteRd: " + userCanDeleteRd.isSuccess());

//      좋아요 여부
//		ResultData userReactionRd = userReaction(loginedMemberId, article.getId());
//		if(userReactionRd == null) return;
//		article.setUserReaction((int)userReactionRd.getData1());

    }

    public ResultData userCanModify(Long loginedMemberId, Article article) {

        if (article.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 수정 권한 없음", article.getId()));
        }
        return ResultData.from("S-1", Ut.f("%d번 게시글 수정 권한 있음", article.getId()));
    }

    private ResultData userCanDelete(Long loginedMemberId, Article article) {

        if (article.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 삭제 권한 없음", article.getId()));
        }
        return ResultData.from("S-1", Ut.f("%d번 게시글 삭제 권한 있음", article.getId()));
    }

    public int modifyArticle(Article article) {
        return articleRepository.modifyArticle(article); // update된 row 수 반환
    }

    public int deleteArticle(Long id, Long memberId) {
        return articleRepository.deleteArticle(id, memberId);
    }

    public List<Article> getFollowingArticles(Long memberId, int limitFrom, int itemsInAPage) {
        return articleRepository.getFollowingArticles(memberId, limitFrom, itemsInAPage);
    }

    public int getFollowingArticlesCnt(Long memberId, Long repositoryId, String keyword, int searchItem) {
        return articleRepository.getFollowingArticlesCnt(memberId, repositoryId, keyword, searchItem);
    }
}