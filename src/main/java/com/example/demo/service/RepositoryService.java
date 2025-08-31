package com.example.demo.service;

import com.example.demo.repository.RepositoryRepository;
import com.example.demo.vo.Article;
import com.example.demo.vo.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public int getLastInsertId() {
        return repositoryRepository.getLastInsertId();
    }

    public void insertRepository(Long memberId, String name, boolean aPrivate, String url, String defaultBranch, String owner) {
        repositoryRepository.insertRepository(memberId, name, aPrivate, url, defaultBranch, owner);
    }

}