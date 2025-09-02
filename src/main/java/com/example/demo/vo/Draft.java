package com.example.demo.vo;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Draft {
    private Long id;
    private Long memberId;
    private Long repositoryId;
    private String checksum;
    private String title;
    private String body;
    private LocalDateTime regDate;

    private String extra__writer;
    private String extra__repositoryName;
}
