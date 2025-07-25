package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UsrHomeController {

    @RequestMapping("/usr/home/main")
    public String showMain() {
        return "usr/home/main";
    }

    @RequestMapping("/")
    public String connectMain() {
        return "redirect:/usr/home/main";
    }

}