package com.example.demo.repository;

import com.example.demo.vo.Repository;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepositoryRepository {
    void makeRepository(Long memberId, String repoName, String lastRqCommit);

    List<com.example.demo.vo.Repository> getRepositoriesByMemberId(Long memberId);

    Repository getRepositoryByIdAndMember(Long repositoryId, Long memberId);

    int existsByMemberIdAndRepoName(Long memberId, String name);

    void insertRepository(Long memberId, String name);

    int getLastInsertId();

}
