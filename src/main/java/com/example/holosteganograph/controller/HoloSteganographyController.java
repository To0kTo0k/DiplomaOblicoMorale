package com.example.holosteganograph.controller;

import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.exceptions.FindBytesFromImageException;
import com.example.holosteganograph.exceptions.FloatsToBytesTransformationException;
import com.example.holosteganograph.exceptions.HideBytesInImageException;
import com.example.holosteganograph.exceptions.PreholoImageCreationException;
import com.example.holosteganograph.exceptions.PreholoImageToBinaryMatrixTransformationException;
import com.example.holosteganograph.exceptions.ResourceResponseException;
import com.example.holosteganograph.model.IOContent;
import com.example.holosteganograph.service.HoloDecoderService;
import com.example.holosteganograph.service.HoloEncoderService;
import com.example.holosteganograph.service.impl.HoloDecoderServiceImpl;
import com.example.holosteganograph.service.impl.HoloEncoderServiceImpl;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/upload")
public class HoloSteganographyController {

    private static final Path UPLOAD_DIRECTORY = Paths.get("src/main/resources/static/");

    @PostMapping("/coding")
    public ResponseEntity<Resource> steganographyCoding(@RequestParam(value = "file") MultipartFile file,
                                                        @RequestParam("text") String text)
            throws ResourceResponseException,
            FloatsToBytesTransformationException,
            CacheImageDeletingException,
            HideBytesInImageException,
            FileNotUploadedException,
            PreholoImageCreationException {
        IOContent content = new IOContent();
        HoloEncoderService encoderService = new HoloEncoderServiceImpl();
        encoderService.textToSteganography(file, text, content, UPLOAD_DIRECTORY);
        try {
            Resource resource = new UrlResource(Paths.get(content.getFilename()).toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
        } catch (IOException e) {
            throw new ResourceResponseException("Error sending response as Resource" + e.getMessage());
        }
    }

    @PostMapping("/decoding")
    public ResponseEntity<String> steganographyDecoding(@RequestParam("file") MultipartFile file)
            throws CacheImageDeletingException,
            FileNotUploadedException,
            PreholoImageToBinaryMatrixTransformationException,
            FindBytesFromImageException {
        IOContent content = new IOContent();
        HoloDecoderService decoderService = new HoloDecoderServiceImpl();
        decoderService.steganographyToText(file, UPLOAD_DIRECTORY, content);
        return new ResponseEntity<>(content.getText(), HttpStatus.OK);
    }

    @DeleteMapping("/deleting")
    public void deleteCash() throws CacheImageDeletingException {
        try {
            FileUtils.cleanDirectory(UPLOAD_DIRECTORY.toFile());
        } catch (IOException e) {
            throw new CacheImageDeletingException("Error cleaning uploaded files" + e.getMessage());
        }
    }
}
