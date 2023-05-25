package com.example.holosteganograph.service;

import com.example.holosteganograph.exceptions.BytesToFloatsTransformationException;
import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.exceptions.FindBytesFromImageException;
import com.example.holosteganograph.exceptions.IllegalFileContentException;
import com.example.holosteganograph.exceptions.PreholoImageToBinaryMatrixTransformationException;
import com.example.holosteganograph.model.IOContent;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface HoloDecoderService {
    void steganographyToText(MultipartFile file, Path uploadDirectory, IOContent content)
            throws FileNotUploadedException,
            CacheImageDeletingException,
            PreholoImageToBinaryMatrixTransformationException,
            FindBytesFromImageException,
            IllegalFileContentException, BytesToFloatsTransformationException;
}
