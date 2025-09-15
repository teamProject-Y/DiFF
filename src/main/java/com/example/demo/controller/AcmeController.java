// src/main/java/com/example/demo/controller/AcmeController.java
package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/.well-known/acme-challenge")
public class AcmeController {
    @GetMapping("/**")
    public ResponseEntity<String> ok() {
        return ResponseEntity.ok("ok");
    }
}
