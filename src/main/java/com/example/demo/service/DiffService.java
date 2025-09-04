package com.example.demo.service;

import com.example.demo.repository.DiffRepository;
import com.example.demo.vo.Diff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiffService {

    private final DiffRepository diffRepository;

    public Long saveDiff(Diff diff) {
        if (diff.getId() != null) {
            System.out.println("🛠️ [Service] 기존 diff 업데이트 실행 (id=" + diff.getId() + ")");
            diffRepository.updateDiff(diff);
        } else {
            System.out.println("🛠️ [Service] 새 diff 인서트 실행");
            diffRepository.insertDiff(diff);
            System.out.println("🛠️ [Service] insert 후 diff.id=" + diff.getId());
        }

        return diff.getId();
    }

    public void updateDiff(Diff diffEntity) {
        diffRepository.updateDiff(diffEntity);
    }
}
