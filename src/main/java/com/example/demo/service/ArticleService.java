package com.example.demo.service;

import java.util.Collections;
import java.util.List;

import com.example.demo.repository.*;
import com.example.demo.vo.Analysis;
import com.example.demo.vo.Article;
import com.example.demo.vo.Member;
import com.example.demo.vo.ResultData;
import com.example.util.SonarGradeUtil;
import com.example.util.Ut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private DraftService draftService;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private HitsRepository hitsRepository;

    public int writeArticle(Long memberId,
                            String title,
                            String body,
                            String checksum,
                            Long repositoryId,
                            Long draftId,
                            Long diffId) {

        // 1. 글 저장
        int rows = articleRepository.writeArticle(memberId, title, body, checksum, repositoryId, diffId);
        // 2. 드래프트 삭제 (이미 사용된 임시저장글이면 삭제)
        if (draftId != null) {
            draftService.deleteDraft(draftId, memberId);
        }

        // 3. 작성자 정보 가져오기
        Member writer = memberRepository.getMemberById(memberId);

        // 4. 팔로워 목록 조회
        List<Member> followers = memberRepository.getFollowingList(writer.getId());

        // 5. 팔로워들에게 푸시 알림 발송
        for (Member follower : followers) {
            if (follower.getFcmToken() != null && !follower.getFcmToken().isEmpty()) {
                fcmService.sendMessage(
                        follower.getFcmToken(),
                        "새 글 알림",
                        writer.getNickName() + "님이 새 글을 작성했습니다!",
                        null
                );
                System.out.println("✅ 알림 전송 → " + follower.getNickName());
            } else {
                System.out.println("⚠️ 알림 건너뜀 (토큰 없음) → " + follower.getNickName());
            }
        }
        return rows;
    }

    public int getLastInsertId() {
        return articleRepository.getLastInsertId();
    }

    public int getArticlesCnt(Long repository, String keyword, int searchItem) {
        return articleRepository.getArticlesCnt(repository, keyword, searchItem);
    }

    public List<Article> getArticles(Long repositoryId, String keyword, int searchItem, int limitFrom, int itemsInAPage) {
        System.out.println("📌 [getArticles] start: repoId=" + repositoryId + ", pageFrom=" + limitFrom);

        List<Article> articles = articleRepository.getArticles(repositoryId, keyword, searchItem, limitFrom, itemsInAPage);
        System.out.println("📋 [getArticles] articles.size=" + (articles != null ? articles.size() : 0));

        for (Article article : articles) {
            System.out.println("📝 [getArticles] article id=" + article.getId() + ", title=" + article.getTitle());

            // ✅ article.getDiffId() 기준으로 분석 찾기
            Analysis analysis = analysisRepository.findByDiffId(article.getDiffId());
            System.out.println("🔍 [getArticles] analysis for diffId=" + article.getDiffId() + " → " + (analysis != null ? "FOUND" : "null"));

            if (analysis != null) {
                System.out.println("   - coverage=" + analysis.getCoverage() + ", bugs=" + analysis.getBugs() + ", smells=" + analysis.getCodeSmells());

                analysis.setGradeCoverage(SonarGradeUtil.gradeCoverage(analysis.getCoverage()));
                analysis.setGradeReliability(SonarGradeUtil.gradeReliability(analysis.getBugs()));
                analysis.setGradeMaintainability(SonarGradeUtil.gradeMaintainability(analysis.getCodeSmells()));
                analysis.setGradeDuplications(SonarGradeUtil.gradeDuplications(analysis.getDuplicatedLinesDensity()));
                analysis.setGradeSecurity(SonarGradeUtil.gradeSecurity(analysis.getVulnerabilities()));

                article.setAnalysis(analysis);
                System.out.println("✅ [getArticles] analysis set with grades: " + analysis);
            }
        }

        return articles;
    }


    public List<Article> getTrendingArticles(Integer count, Integer days) {
        return articleRepository.getTrendingArticles(count, days);
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

    }

    public ResultData userCanModify(Long loginedMemberId, Article article) {
        if (!article.getMemberId().equals(loginedMemberId)) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 수정 권한 없음", article.getId()));
        }
        return ResultData.from("S-1", Ut.f("%d번 게시글 수정 권한 있음", article.getId()));
    }

    private ResultData userCanDelete(Long loginedMemberId, Article article) {
        if (!article.getMemberId().equals(loginedMemberId)) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 삭제 권한 없음", article.getId()));
        }
        return ResultData.from("S-1", Ut.f("%d번 게시글 삭제 권한 있음", article.getId()));
    }

    public int modifyArticle(Article article) {
        return articleRepository.modifyArticle(article);
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

    public boolean increaseHits(Long articleId, Long memberId) {

        int exists = hitsRepository.exists(articleId, memberId);
        if (exists > 0) {
            return false;
        }
        hitsRepository.save(articleId, memberId);
        articleRepository.increaseHits(articleId);

        return true;
    }

    public List<Article> getRepositoryArticles(Long repositoryId) {
        return articleRepository.getRepositoryArticles(repositoryId);
    }

    public List<Article> searchArticles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return articleRepository.searchArticles("%" + keyword + "%");
    }
}
