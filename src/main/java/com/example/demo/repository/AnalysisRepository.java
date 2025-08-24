package com.example.demo.repository;

import com.example.demo.vo.Analysis;
import com.example.demo.vo.AnalysisLanguage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisRepository {
    void insert(Analysis analysis);

    void insertLanguage(AnalysisLanguage lang);
}
