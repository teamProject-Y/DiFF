package com.example.demo.service;

import com.example.demo.repository.AnalysisRepository;
import com.example.demo.repository.ArticleRepository;
import com.example.demo.vo.Analysis;
import com.example.util.SonarGradeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    @Autowired
    private AnalysisRepository analysisRepository;

    public Map<String, Object> getAverageMetrics(Long repositoryId) {
        return analysisRepository.getAverageMetrics(repositoryId);
    }

    public List<Analysis> getAnalysisHistory(Long repoId) {
        return analysisRepository.getAnalysisHistory(repoId);
    }

    public String calculateTotalGradeByHistory(Long repoId) {
        List<Analysis> history = analysisRepository.findAllByRepoId(repoId);

        if (history == null || history.isEmpty()) {
            return null;
        }

        // 평균 계산
        double avgVulnerabilities = history.stream()
                .mapToInt(Analysis::getVulnerabilities)
                .average().orElse(0);

        double avgBugs = history.stream()
                .mapToInt(Analysis::getBugs)
                .average().orElse(0);

        double avgCodeSmells = history.stream()
                .mapToInt(Analysis::getCodeSmells)
                .average().orElse(0);

        double avgCoverage = history.stream()
                .mapToDouble(Analysis::getCoverage)
                .average().orElse(0);

        double avgDuplications = history.stream()
                .mapToDouble(Analysis::getDuplicatedLinesDensity)
                .average().orElse(0);

        double avgComplexity = history.stream()
                .mapToInt(Analysis::getComplexity)
                .average().orElse(0);

        // 개별 지표 → 등급 변환
        String security = SonarGradeUtil.gradeSecurity((int) avgVulnerabilities);
        String reliability = SonarGradeUtil.gradeReliability((int) avgBugs);
        String maintainability = SonarGradeUtil.gradeMaintainability((int) avgCodeSmells);
        String coverage = SonarGradeUtil.gradeCoverage(avgCoverage);
        String duplication = SonarGradeUtil.gradeDuplications(avgDuplications);
        String complexity = SonarGradeUtil.gradeComplexity((int) avgComplexity);

        // ✅ 토탈 등급 계산
        return SonarGradeUtil.totalGrade(security, reliability, maintainability, coverage, duplication, complexity);
    }

    public void updateTotalScore(Analysis analysis) {
        String security = SonarGradeUtil.gradeSecurity(analysis.getVulnerabilities());
        String reliability = SonarGradeUtil.gradeReliability(analysis.getBugs());
        String maintainability = SonarGradeUtil.gradeMaintainability(analysis.getCodeSmells());
        String coverage = SonarGradeUtil.gradeCoverage(analysis.getCoverage());
        String duplication = SonarGradeUtil.gradeDuplications(analysis.getDuplicatedLinesDensity());
        String complexity = SonarGradeUtil.gradeComplexity(analysis.getComplexity());

        // 총합 등급
        String totalGrade = SonarGradeUtil.totalGrade(security, reliability, maintainability, coverage, duplication, complexity);

        // 점수화
        int totalScore = SonarGradeUtil.gradeToScore(totalGrade);

        analysis.setTotalScore((double) totalScore);
        analysisRepository.updateTotalScore(analysis);
    }

}
