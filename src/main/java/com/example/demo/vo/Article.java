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
    public Article(String title, String body, int memberId) {
        this.title = title;
        this.body = body;
        this.memberId = memberId;
    }

    private int id;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
    private String title;
    private String body;
    private int memberId;
    private int hits;

    private String extra_writer;
    private String extra_boardCode;

    private int extra_ReactionPoint;

    private boolean userCanModify;
    private boolean userCanDelete;
    private int userReaction;

}