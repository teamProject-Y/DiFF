package com.example.demo.controller;

import com.example.demo.service.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gpt")
public class GptTestController {

    private final GptService gptService;

    @GetMapping("/test")
    public String testGptSummary() {
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

        return gptService.summarizeDiff(dummyDiff);
    }
}
