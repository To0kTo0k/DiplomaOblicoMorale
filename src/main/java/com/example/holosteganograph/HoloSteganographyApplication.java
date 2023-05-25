package com.example.holosteganograph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoloSteganographyApplication {
    static {
        nu.pattern.OpenCV.loadLocally();
    }

    public static void main(String[] args) {
        SpringApplication.run(HoloSteganographyApplication.class, args);
    }
}
