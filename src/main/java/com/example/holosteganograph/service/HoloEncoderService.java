package com.example.holosteganograph.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface HoloEncoderService {

    Resource textToSteganography(MultipartFile file, String text, Path uploadDirectory);
}
