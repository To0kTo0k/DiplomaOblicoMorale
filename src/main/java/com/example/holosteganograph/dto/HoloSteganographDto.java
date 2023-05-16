package com.example.holosteganograph.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class HoloSteganographDto {
    private String filename;
    private String text;
}
