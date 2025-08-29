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
    private String name;
    private String url;
    private String defaultBranch;
    private String lastRqCommit;
    private boolean aPrivate;
    private boolean delStatus;
    private LocalDateTime delDate;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    private String owner;

    public Repository(Long id, String fullName, boolean aPrivate, String defaultBranch, String htmlUrl) {
        this.id = id;
        this.name = fullName;
        this.aPrivate = aPrivate;
        this.defaultBranch = defaultBranch;
        this.url = htmlUrl;
    }
}
