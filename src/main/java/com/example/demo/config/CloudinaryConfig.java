package com.example.demo.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    @Bean
    public static Cloudinary getCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dxzjbhfv6");
        config.put("api_key", "447355269477933");
        config.put("api_secret", "O3OtqlfsK8d1lBCk4fYRLD0fJEQ");


        return new Cloudinary(config);
    }
}