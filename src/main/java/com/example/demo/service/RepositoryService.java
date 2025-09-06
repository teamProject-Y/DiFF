package com.example.demo.service;

import com.example.demo.repository.RepositoryRepository;
import com.example.demo.vo.Article;
import com.example.demo.vo.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RepositoryService {

    @Autowired
    private RepositoryRepository repositoryRepository;

    public RepositoryService(RepositoryRepository repositoryRepository) {this.repositoryRepository = repositoryRepository;}

    public List<Repository> getRepositoriesByMemberId(Long memberId) {
        return repositoryRepository.getRepositoriesByMemberId(memberId);
    }

    public void makeRepository(Long memberId, String repoName, String lastRqCommit) {
        repositoryRepository.makeRepository(memberId, repoName, lastRqCommit);
    }

    public Repository getRepositoryByIdAndMember(Long repositoryId, Long memberId) {
        return repositoryRepository.getRepositoryByIdAndMember(repositoryId, memberId);
    }

    public boolean existsByMemberIdAndRepoName(Long memberId, String name) {
        return repositoryRepository.existsByMemberIdAndRepoName(memberId, name) > 0;
    }

    public Long getRepoIdByMemberIdAndGithubRepoName(Long memberId, String githubName) {
        return repositoryRepository.getRepoIdByMemberIdAndGithubRepoName(memberId, githubName);
    }

    public int getLastInsertId() {
        return repositoryRepository.getLastInsertId();
    }

    public void insertRepository(
            Long memberId, String name, boolean aPrivate, String url, String defaultBranch, String owner, String githubName, String githubOwner) {
        repositoryRepository.insertRepository(memberId, name, aPrivate, url, defaultBranch, owner, githubName, githubOwner);
    }

    public boolean isRepoOwner(Long memberId, Long repositoryId) {
        return Objects.equals(memberId, repositoryRepository.getMemberIdByRepositoryId(repositoryId));
    }

    public List<Map<String, Object>> getLanguageDistributionByRepo(Long repositoryId) {
        return repositoryRepository.getLanguageDistributionByRepo(repositoryId);
    }

    public int renameRepository(Long id, String name) {
        return repositoryRepository.renameRepository(id, name);
    }
}