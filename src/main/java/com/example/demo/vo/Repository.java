package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Repository {
    private Long id;
    private Long memberId;
    private String name;              // title → name 으로 수정
    private String url;
    private String lastRqCommit;     // Long → String 으로 수정 (commit hash는 문자열)
    private boolean delStatus;
    private LocalDateTime delDate;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
}
