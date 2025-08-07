// 위치: src/main/java/com/example/demo/vo/Repository.java
package com.example.demo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Repository {
    private Long id;
    private Long memberId;
    private String title;
    private String url;
    private Long lastRequestCommitId;
    private boolean delStatus;
    private LocalDateTime delDate;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
    private String owner;
}
