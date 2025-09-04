package com.example.demo.service;

import com.example.demo.repository.DraftRepository;
import com.example.demo.repository.RepositoryRepository;
import com.example.demo.vo.Draft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DraftService {

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

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

    public List<Draft> getDraftsByMember(Long memberId) {

        return draftRepository.getDraftsByMember(memberId);
    }


    public Draft getDraftById(Long id) {
        return draftRepository.getDraftById(id);
    }

    public int deleteDraft(Long id, Long memberId) {
        return draftRepository.deleteDraft(id,memberId);
    }

    public Long saveDraft(Draft draft) {
        if (draft.getId() != null) {
            System.out.println("🛠️ [Service] 기존 draft 업데이트 실행 (id=" + draft.getId() + ")");
            draftRepository.updateDraft(draft);
        } else {
            System.out.println("🛠️ [Service] 새 draft 인서트 실행");
            System.out.println("👉 draft.memberId = " + draft.getMemberId());
            draftRepository.insertDraft(draft);
            System.out.println("🛠️ [Service] insert 후 draft.id=" + draft.getId());
        }

        return draft.getId();
    }

    public void updateDraft(Draft draft) {
        draftRepository.updateDraft(draft);
    }
}