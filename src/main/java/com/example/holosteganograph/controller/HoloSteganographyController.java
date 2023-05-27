package com.example.holosteganograph.controller;

import com.example.holosteganograph.service.HoloDecoderService;
import com.example.holosteganograph.service.HoloEncoderService;
import com.example.holosteganograph.service.HoloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class HoloSteganographyController {
    private static final Path UPLOAD_DIRECTORY = Paths.get("src/main/resources/static/");
    private final HoloService service;
    private final HoloEncoderService encoderService;
    private final HoloDecoderService decoderService;

    @Autowired
    public HoloSteganographyController(@Qualifier("holoServiceImpl") HoloService service,
                                       HoloEncoderService encoderService,
                                       HoloDecoderService decoderService) {
        this.service = service;
        this.encoderService = encoderService;
        this.decoderService = decoderService;
    }

    @PostMapping("/code")
    public ResponseEntity<Resource> steganographyCoding(@RequestParam(value = "file") MultipartFile file,
                                                        @RequestParam(value = "text") String text) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(encoderService.textToSteganography(file, text, UPLOAD_DIRECTORY));
    }

    @PostMapping("/decode")
    public ResponseEntity<String> steganographyDecoding(@RequestParam(value = "file") MultipartFile file) {
        return new ResponseEntity<>(decoderService.steganographyToText(file, UPLOAD_DIRECTORY), HttpStatus.OK);
    }

    @DeleteMapping
    public void deleteCash() {
        service.deleteCacheImage(UPLOAD_DIRECTORY);
    }
}
