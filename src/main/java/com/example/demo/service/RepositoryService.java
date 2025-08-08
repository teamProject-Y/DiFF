package com.example.demo.service;

import com.example.demo.repository.RepositoryRepository;
import org.aspectj.apache.bcel.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepositoryService {

    @Autowired
    private RepositoryRepository repositoryRepository;

    public RepositoryService(RepositoryRepository repositoryRepository) {this.repositoryRepository = repositoryRepository;}

    public List<Repository> getRepositoriesByMemberId(Long memberId) {
        return repositoryRepository.getRepositoriesByMemberId(memberId);
    }

    public void makeRepository(int memberId, String repoName, String lastRqCommit) {
        repositoryRepository.makeRepository(memberId, repoName, lastRqCommit);
    }

}
