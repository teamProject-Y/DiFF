package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HitsRepository {
    int exists(Long articleId, Long memberId);

    void save(Long articleId, Long memberId);
}
