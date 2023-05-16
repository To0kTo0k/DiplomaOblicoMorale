package com.example.holosteganograph.controller;

import com.example.holosteganograph.dto.HoloSteganographDto;
import com.example.holosteganograph.service.SteganographService;
import com.fasterxml.uuid.Generators;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class HoloSteganographController {

    private static final Path UPLOAD_DIRECTORY = Paths.get("src/main/resources/static/");

    @Autowired
    SteganographService steganographService;

    @Autowired
    HoloSteganographDto steganographDto;

    @GetMapping("/coding")
    public ResponseEntity<?> steganographyCoding(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            UUID uuid = Generators.timeBasedGenerator().generate();
            String filename = uuid.toString() + "_" + file.getOriginalFilename();
            Path filePath = UPLOAD_DIRECTORY.resolve(filename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            steganographDto.setFilename(filePath.toString());
            steganographDto.setText("Coded text");
            steganographService.textToSteganography();
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } catch (IOException e) {
            throw new IOException("Error saving uploaded file" + file.getOriginalFilename(), e);
        }
    }

    @GetMapping("/decoding")
    public ResponseEntity<?> steganographyDecoding(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            UUID uuid = Generators.timeBasedGenerator().generate();
            String filename = uuid.toString() + "_" + file.getOriginalFilename();
            Path filePath = UPLOAD_DIRECTORY.resolve(filename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            steganographDto.setFilename(filePath.toString());
            steganographService.SteganographyToText();
            return new ResponseEntity<>(steganographDto.getText(), HttpStatus.OK);
        } catch (IOException e) {
            throw new IOException("Error saving uploaded file" + file.getOriginalFilename(), e);
        }
    }

    @DeleteMapping("/deleting")
    public void deleteCash() throws IOException {
        FileUtils.cleanDirectory(UPLOAD_DIRECTORY.toFile());
    }
}
