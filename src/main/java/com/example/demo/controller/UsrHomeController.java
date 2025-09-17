package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "https://diff.io.kr",
        "https://diff-front.fly.dev",
        "http://localhost:3000",
        "http://127.0.0.1:3000"
})
@RequiredArgsConstructor
public class UsrHomeController {

    @GetMapping("/api/DiFF/home/main")
    public Map<String, String> showMain() {
        System.out.println("===== 🏠 [Get] /api/DiFF/home/main =====");
        return Map.of("text", "🏠 home request is successful ");
    }

    @RequestMapping("/")
    public String connectMain() {
        return "redirect:/api/DiFF/home/main";
    }

}