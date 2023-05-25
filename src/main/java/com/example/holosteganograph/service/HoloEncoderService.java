package com.example.holosteganograph.service;

import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.exceptions.FloatsToBytesTransformationException;
import com.example.holosteganograph.exceptions.HideBytesInImageException;
import com.example.holosteganograph.exceptions.PreholoImageCreationException;
import com.example.holosteganograph.model.IOContent;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface HoloEncoderService {
    void textToSteganography(MultipartFile file, String text, IOContent content, Path uploadDirectory)
            throws FileNotUploadedException,
            CacheImageDeletingException,
            FloatsToBytesTransformationException,
            HideBytesInImageException,
            PreholoImageCreationException;
}
