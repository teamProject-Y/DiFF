package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {
    private Long id;
    private Long toMemberId;
    private Long fromMemberId;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
}
