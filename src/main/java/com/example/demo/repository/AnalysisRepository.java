package com.example.demo.repository;

import com.example.demo.vo.Analysis;
import com.example.demo.vo.AnalysisLanguage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisRepository {

    void insert(Analysis analysis);

    void insertLanguage(AnalysisLanguage lang);

    Map<String, Object> getAverageMetrics(Long repositoryId);

    Analysis findByChecksum(String checksum);

    int updateRepositoryIdByChecksum(String checksum, Long repositoryId);

    List<Analysis> getAnalysisHistory(Long repoId);

    List<Analysis> findAllByRepoId(Long repoId);

    void updateTotalScore(Analysis analysis);

    List<Analysis> getAnalysisRecent(Long repoId);
}
