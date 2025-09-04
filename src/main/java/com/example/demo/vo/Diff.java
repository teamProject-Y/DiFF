package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Diff {
    private Long id;         // diffId
    private Long draftId;    // 연결된 draftId
    private String checksum; // commit checksum
    private String regDate;
}
