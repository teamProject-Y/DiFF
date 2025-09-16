package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ReactionRepository {

    int like(String relType, Long relId, Long memberId);

    int unlike(String relType, Long relId, Long memberId);

    long getCount(String relType, Long relId);

    boolean isLiked(String relType, Long relId, Long memberId);

    Long getLikeCountsByMemberId(Long id);
}