package com.example.demo.service;

import java.util.Collections;
import java.util.List;

import com.example.demo.repository.*;
import com.example.demo.vo.*;
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
    private NotificationService notificationService;

    @Autowired
    private HitsRepository hitsRepository;

    public Long writeArticle(Long memberId,
                             String title,
                             String body,
                             boolean isPublic,
                             String checksum,
                             Long repositoryId,
                             Long draftId,
                             Long diffId) {

        // 글 저장
        Article article = Article.builder()
                .memberId(memberId)
                .title(title)
                .body(body)
                .isPublic(isPublic)
                .checksum(checksum)
                .repositoryId(repositoryId)
                .draftId(draftId)
                .diffId(diffId)
                .build();

        articleRepository.writeArticle(article);
        Long articleId = article.getId();
        System.out.println("✅ 새 글 저장됨 → articleId=" + articleId + ", checksum=" + checksum);

        // Analysis repositoryId 동기화
        if (checksum != null && repositoryId != null) {
            int updated = analysisRepository.updateRepositoryIdByChecksum(checksum, repositoryId);
            System.out.println("✅ Analysis repositoryId 동기화됨 (rows=" + updated + ")");
        }

        // 드래프트 삭제 (이미 사용된 임시저장글이면 삭제)
        if (draftId != null) {
            draftService.deleteDraft(draftId, memberId);
        }

        // 작성자 정보 가져오기
        Member writer = memberRepository.getMemberById(memberId);

        // 팔로워 목록 조회
        List<Member> followers = memberRepository.getFollowingList(writer.getId());

        // 팔로워들에게 푸시 알림 + DB 알림 발송
        for (Member follower : followers) {

            String msg = writer.getNickName() + " has published a new post";

            //  DB Notification 저장
            Notification notification = Notification.builder()
                    .memberId(follower.getId())
                    .type("ARTICLE")
                    .message(msg)
                    .isRead(false)
                    .relId(articleId)
                    .build();

            notificationService.saveNotification(notification);
            System.out.println("✅ DB 알림 저장 → " + follower.getNickName());

            //  FCM 발송
            if (follower.isAllowArticleNotification()) {
                if (follower.getFcmToken() != null && !follower.getFcmToken().isEmpty()) {
                    try {
                        fcmService.sendMessage(
                                follower.getFcmToken(),
                                "New Post Alert",
                                msg,
                                null
                        );
                        System.out.println("✅ FCM 알림 전송 → " + follower.getNickName());
                    } catch (Exception e) {
                        System.out.println("⚠️ FCM 발송 실패 → " + follower.getNickName() + " : " + e.getMessage());
                    }
                } else {
                    System.out.println("⚠️ 알림 건너뜀 (토큰 없음) → " + follower.getNickName());
                }
            } else {
                System.out.println("⚠️ 글 작성 알림 OFF → FCM 스킵 (DB 저장은 완료)");
            }
        }


        return articleId;
    }

    public int getLastInsertId() {
        return articleRepository.getLastInsertId();
    }

    public List<Article> getArticles(Long repositoryId,
                                     String keyword,
                                     int searchItem,
                                     Long loginedMemberId) {

        List<Article> articles = articleRepository.getArticles(repositoryId, keyword, searchItem, loginedMemberId);

        for (Article article : articles) {

            Analysis analysis = analysisRepository.findByChecksum(article.getChecksum());

            if (analysis != null) {
                analysis.setGradeCoverage(SonarGradeUtil.gradeCoverage(analysis.getCoverage()));
                analysis.setGradeReliability(SonarGradeUtil.gradeReliability(analysis.getBugs()));
                analysis.setGradeMaintainability(SonarGradeUtil.gradeMaintainability(analysis.getCodeSmells()));
                analysis.setGradeDuplications(SonarGradeUtil.gradeDuplications(analysis.getDuplicatedLinesDensity()));
                analysis.setGradeSecurity(SonarGradeUtil.gradeSecurity(analysis.getVulnerabilities()));
                analysis.setGradeComplexity(SonarGradeUtil.gradeComplexity(analysis.getComplexity()));

                article.setAnalysis(analysis);
            }
        }
        return articles;
    }

    public List<Article> getTrendingArticles(Integer count, Integer days, Long loginedMemberId) {
        return articleRepository.getTrendingArticles(count, days, loginedMemberId);
    }

    public Article getArticleById(Long id, Long loginedMemberId) {
        Article article = articleRepository.getArticleById(id);

        System.out.println("getArticleById: " + article);

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

    public List<Article> getFollowingArticles(int limitFrom, int itemsInAPage, Long loginedMemberId) {
        return articleRepository.getFollowingArticles(limitFrom, itemsInAPage, loginedMemberId);
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

    public List<Article> searchArticles(String keyword, Long loginedMemberId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return articleRepository.searchArticles("%" + keyword + "%", loginedMemberId);
    }

    public Long getWriterIdByArticleId(Long articleId) {
        return articleRepository.getWriterIdByArticleId(articleId);
    }

}
