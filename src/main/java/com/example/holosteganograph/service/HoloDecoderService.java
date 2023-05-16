package com.example.holosteganograph.service;

import org.opencv.core.Mat;

import java.io.IOException;

public interface HoloDecoderService {

    String decodeHoloInBytesToText(byte[] bytes) throws IOException;

    Mat bytesToHolo(byte[] bytes);

    void hologramToImage(Mat hologram, String filename);

    boolean[][] imageToBinaryMatrix(String filename);

    String binaryMatrixToBinaryString(boolean[][] matrix);

    String toCharString(String biteString);
}
