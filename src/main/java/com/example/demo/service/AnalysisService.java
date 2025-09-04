package com.example.demo.service;

import com.example.demo.repository.AnalysisRepository;
import com.example.demo.repository.ArticleRepository;
import com.example.demo.vo.Analysis;
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
}
