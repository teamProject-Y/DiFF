package com.example.demo.repository;

import com.example.demo.vo.Repository;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface RepositoryRepository {
    void makeRepository(Long memberId, String repoName, String lastRqCommit);

    List<Repository> getRepositoriesByMemberId(Long memberId);

    Repository getRepositoryByIdAndMember(Long repositoryId, Long memberId);

    int existsByMemberIdAndRepoName(Long memberId, String name);

    void insertRepository(Long memberId, String name, boolean aPrivate, String url, String defaultBranch, String owner, String githubName, String githubOwner);

    int getLastInsertId();

    Long getMemberIdByRepositoryId(Long repositoryId);

    Long getRepoCountsByMemberId(Long id);

    List<Map<String, Object>> getLanguageDistributionByRepo(Long repositoryId);
  
    int renameRepository(Long id, String name);

    int connectRepository(Long id, String url, String githubOwner, String githubName, String defaultBranch);

    Long getRepoIdByMemberIdAndGithubRepoName(Long memberId, String githubName);

    Repository getRepositoryById(Long id);

    int deleteRepository(Long id, Long loginedMemberId);
}