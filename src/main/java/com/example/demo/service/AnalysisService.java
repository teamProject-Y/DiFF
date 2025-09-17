package com.example.demo.service;

import com.example.demo.repository.AnalysisRepository;
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
