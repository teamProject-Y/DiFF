package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ReactionRepository {

    public int like(String relType, Long relId, Long memberId);

    public int unlike(String relType, Long relId, Long memberId);

    public long getCount(String relType, Long relId);

    public boolean isLiked(String relType, Long relId, Long memberId);

    Long getLikeCountsByMemberId(Long id);
}