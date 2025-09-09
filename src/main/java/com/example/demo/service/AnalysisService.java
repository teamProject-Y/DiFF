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

        String security = SonarGradeUtil.gradeSecurity((int) avgVulnerabilities);
        String reliability = SonarGradeUtil.gradeReliability((int) avgBugs);
        String maintainability = SonarGradeUtil.gradeMaintainability((int) avgCodeSmells);
        String coverage = SonarGradeUtil.gradeCoverage(avgCoverage);
        String duplication = SonarGradeUtil.gradeDuplications(avgDuplications);
        String complexity = SonarGradeUtil.gradeComplexity((int) avgComplexity);

        return SonarGradeUtil.totalGrade(security, reliability, maintainability, coverage, duplication, complexity);
    }

    public void updateTotalScore(Analysis analysis) {
        int sec = SonarGradeUtil.gradeToScore(SonarGradeUtil.gradeSecurity(analysis.getVulnerabilities()));
        int rel = SonarGradeUtil.gradeToScore(SonarGradeUtil.gradeReliability(analysis.getBugs()));
        int main = SonarGradeUtil.gradeToScore(SonarGradeUtil.gradeMaintainability(analysis.getCodeSmells()));
        int cov = SonarGradeUtil.gradeToScore(SonarGradeUtil.gradeCoverage(analysis.getCoverage()));
        int dup = SonarGradeUtil.gradeToScore(SonarGradeUtil.gradeDuplications(analysis.getDuplicatedLinesDensity()));
        int comp = SonarGradeUtil.gradeToScore(SonarGradeUtil.gradeComplexity(analysis.getComplexity()));

        double avgScore = (sec + rel + main + cov + dup + comp) / 6.0;

        analysis.setTotalScore(avgScore);

        analysisRepository.updateTotalScore(analysis);
    }

    public List<Analysis> getAnalysisRecent(Long repoId) {
        return analysisRepository.getAnalysisRecent(repoId);
    }
}
