package com.example.demo.service;

import com.example.demo.repository.DraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DraftService {

    @Autowired
    private DraftRepository draftRepository;

    public DraftService(DraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    public void makeRepository(int memberId, String repoName, String firstCommit) {
        draftRepository.makeRepository(memberId, repoName, firstCommit);
    }

    public int getLastInsertId() {
        return draftRepository.getLastInsertId();
    }

    public boolean existsByMemberIdAndRepoName(int memberId, String repoName) {
        return draftRepository.existsByMemberIdAndRepoName(memberId, repoName) == 0;
    }
}