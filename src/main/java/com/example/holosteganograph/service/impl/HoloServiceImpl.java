package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.service.HoloService;
import com.fasterxml.uuid.Generators;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class HoloServiceImpl implements HoloService {

    @Override
    public void deleteCacheImage(Path directory) {
        try {
            FileUtils.cleanDirectory(directory.toFile());
        } catch (IOException e) {
            throw new CacheImageDeletingException("Error cleaning uploaded files " + e.getMessage());
        }
    }

    protected Path defineFilepath(MultipartFile file, Path directory) {
        if (file.isEmpty())
            throw new FileNotUploadedException("File is empty");
        String filename = Generators.timeBasedGenerator().generate().toString() + "_" + file.getOriginalFilename();
        return directory.resolve(filename);
    }

    protected int[] getImagePixel(BufferedImage image, int row, int column) {
        int pixel = image.getRGB(row, column);
        int[] rgb = new int[4];
        rgb[0] = (pixel >> 24) & 0xff;
        rgb[1] = (pixel >> 16) & 0xff;
        rgb[2] = (pixel >> 8) & 0xff;
        rgb[3] = pixel & 0xff;
        return rgb;
    }
}
