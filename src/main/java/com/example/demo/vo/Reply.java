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
public class Reply {

    private Long id;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
    private Long memberId;
    private Long relId;
    private String body;

    private String extra__writer;
    private Long extra__ReactionPoint;

    private boolean userCanModify;
    private boolean userCanDelete;
    private boolean userReaction;

}