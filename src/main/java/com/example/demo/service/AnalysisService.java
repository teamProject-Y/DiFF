package com.example.demo.service;

import com.example.demo.repository.AnalysisRepository;
import com.example.demo.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AnalysisService {

    @Autowired
    private AnalysisRepository analysisRepository;

    public Map<String, Object> getAverageMetrics(Long repositoryId) {
        return analysisRepository.getAverageMetrics(repositoryId);
    }
}
