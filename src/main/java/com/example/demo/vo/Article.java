package com.example.demo.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Article {
    public Article(String title, String body, int memberId, int boardId) {
        this.title = title;
        this.body = body;
        this.memberId = memberId;
        this.boardId = boardId;
    }

    private int id;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
    private String title;
    private String body;
    private int memberId;
    private int boardId;
    private int hits;

    private String extra_writer;
    private String extra_boardCode;

    private int extra_goodReactionPoint;
    private int extra_badReactionPoint;
    private int extra_sumReactionPoint;

    private boolean userCanModify;
    private boolean userCanDelete;
    private int userReaction;

}