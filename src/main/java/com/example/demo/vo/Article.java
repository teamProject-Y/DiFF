package com.example.demo.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Article {
    private Long id;
    private Long memberId;
    private Long repositoryId;
    private String title;
    private String body;
    private String checksum;
    private Integer hits;
    private Boolean isDraft;
    private Boolean isPublic;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    private int extra__sumReaction;
    private int extra__sumComment;
    private String extra__writer;

    private boolean userCanModify;
    private boolean userCanDelete;
    private boolean userReaction;
}