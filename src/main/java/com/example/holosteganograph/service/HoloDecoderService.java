package com.example.holosteganograph.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface HoloDecoderService {
    String steganographyToText(MultipartFile file, Path uploadDirectory);
}
