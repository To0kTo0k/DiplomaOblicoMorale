package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.exceptions.BytesToFloatsTransformationException;
import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.exceptions.FindBytesFromImageException;
import com.example.holosteganograph.exceptions.IllegalFileContentException;
import com.example.holosteganograph.exceptions.PreholoImageToBinaryMatrixTransformationException;
import com.example.holosteganograph.service.HoloDecoderService;
import com.fasterxml.uuid.Generators;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class HoloDecoderServiceImpl extends HoloServiceImpl implements HoloDecoderService {

    @Override
    public String steganographyToText(MultipartFile file, Path uploadDirectory) {
        Path filePath = defineFilepath(file, uploadDirectory);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileNotUploadedException("Error get file as InputStream " + e.getMessage());
        }
        return decodeHoloInBytesToText(readHiddenBytesFromImage(filePath.toString()));
    }

    private byte[] readHiddenBytesFromImage(String path) {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            var bytes = new byte[image.getWidth() * image.getHeight()];
            int byteIndex = 0;

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int pixel = image.getRGB(i, j);
                    int a = (pixel >> 24) & 0xff;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;

                    byte aByte = (byte) (a & 3);
                    aByte <<= 2;
                    aByte |= (r & 3);
                    aByte <<= 2;
                    aByte |= (g & 3);
                    aByte <<= 2;
                    aByte |= (b & 3);
                    bytes[byteIndex] = aByte;

                    byteIndex++;
                }
            }
            return bytes;
        } catch (IOException e) {
            throw new FindBytesFromImageException("Error find bytes of holo from image " + e.getMessage());
        }
    }

    private String decodeHoloInBytesToText(byte[] bytes) {
        String filename = "decode" + Generators.timeBasedGenerator().generate().toString() + ".png";
        hologramToImage(bytesToHolo(bytes), filename);
        String string = binaryMatrixToBinaryString(imageToBinaryMatrix(filename));
        try {
            Files.delete(Path.of(filename));
        } catch (IOException e) {
            throw new CacheImageDeletingException("Error deleting cache files " + e.getMessage());
        }
        return toCharString(string);
    }

    private Mat bytesToHolo(byte[] bytes) {
        try {
            int matrixSize = ByteBuffer.wrap(bytes).getInt();
            bytes = Arrays.copyOfRange(bytes, 4, bytes.length);

            int hologramBytesSize = ByteBuffer.wrap(bytes).getInt();
            bytes = Arrays.copyOfRange(bytes, 4, bytes.length);

            byte[] firstPlaneByte = Arrays.copyOfRange(bytes, 0, hologramBytesSize);
            bytes = Arrays.copyOfRange(bytes, hologramBytesSize, bytes.length);
            byte[] secondPlaneByte = Arrays.copyOfRange(bytes, 0, hologramBytesSize);

            float[] firstPlaneFloat = bytesToFloats(firstPlaneByte);
            float[] secondPlaneFloat = bytesToFloats(secondPlaneByte);

            var size = new Size(matrixSize, matrixSize);
            var firstPlane = new Mat(size, CvType.CV_32F);
            var secondPlane = new Mat(size, CvType.CV_32F);

            firstPlane.put(0, 0, firstPlaneFloat);
            secondPlane.put(0, 0, secondPlaneFloat);

            List<Mat> planes = new ArrayList<>();
            planes.add(firstPlane);
            planes.add(secondPlane);

            var complex = new Mat();
            Core.merge(planes, complex);

            return complex;
        } catch (IllegalArgumentException e) {
            throw new IllegalFileContentException("File contains illegal information");
        }
    }

    private float[] bytesToFloats(byte[] bytes) {
        if (bytes.length % Float.BYTES != 0)
            throw new BytesToFloatsTransformationException(
                    "Illegal bytes length: " + bytes.length + " % " + Float.BYTES + " != 0");
        var floats = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes).asFloatBuffer().get(floats);
        return floats;
    }

    private void hologramToImage(Mat hologram, String filename) {
        Core.idft(hologram, hologram);
        var out = new Mat();
        List<Mat> planes = new ArrayList<>();
        Core.split(hologram, planes);
        Core.normalize(planes.get(0), out, 0, 255, Core.NORM_MINMAX);
        out.convertTo(out, CvType.CV_8U);
        Imgcodecs.imwrite(filename, out);
    }

    private boolean[][] imageToBinaryMatrix(String filename) {
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            var matrix = new boolean[image.getHeight()][image.getWidth()];
            for (int i = 0; i < image.getHeight(); i++) {
                for (int j = 0; j < image.getWidth(); j++) {
                    int rgba = image.getRGB(i, j);
                    int color = (rgba >> 16) & 255;
                    matrix[i][j] = color == 0;
                }
            }
            return matrix;
        } catch (IOException e) {
            throw new PreholoImageToBinaryMatrixTransformationException(
                    "Error preholo image to binary matrix transformation " + e.getMessage());
        }
    }

    private String binaryMatrixToBinaryString(boolean[][] matrix) {
        var binaryString = new StringBuilder();
        for (boolean[] booleans : matrix) {
            for (int j = 0; j < matrix.length; j++) {
                if (booleans[j]) {
                    binaryString.append('1');
                } else {
                    binaryString.append('0');
                }
            }
        }
        return binaryString.toString().trim();
    }

    private String toCharString(String biteString) {
        var charString = new StringBuilder();
        for (int i = 0; i < biteString.length() / 16; i++) {
            charString.append(Character.toChars(Integer.parseInt(biteString.substring(i * 16, i * 16 + 16), 2)));
        }
        return charString.toString();
    }
}
