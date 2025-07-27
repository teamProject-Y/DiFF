package com.example.demo.service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GptServiceTest {

    @Autowired
    private GptService gptService;

    @Test
    public void testSummarizeDiff() {
        String dummyDiff = """
            diff --git a/Main.java b/Main.java
            index e69de29..4b825dc 100644
            --- a/Main.java
            +++ b/Main.java
            @@ public class Main {
            +   public static void main(String[] args) {
            +       System.out.println("Hello, world!");
            +   }
        """;

        String summary = gptService.summarizeDiff(dummyDiff);
        System.out.println("GPT 요약 결과: " + summary);
    }
}
