package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class UsrHomeController {

    @GetMapping("/api/DiFF/home/main")
    public Map<String, String> showMain() {

        System.err.println("home come in");



        return Map.of("text", "Spring에서 온 메인 메시지!");
    }

    @RequestMapping("/")
    public String connectMain() {
        return "redirect:/api/DiFF/home/main";
    }

}