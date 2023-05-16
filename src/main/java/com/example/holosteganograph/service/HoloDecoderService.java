package com.example.holosteganograph.service;

import org.opencv.core.Mat;

public interface HoloDecoderService {

    String decodeHoloInBytesToText(byte[] bytes);

    Mat bytesToHolo(byte[] bytes);

    void hologramToImage(Mat hologram, String filename);

    boolean[][] imageToBinaryMatrix(String filename);

    String binaryMatrixToBinaryString(boolean[][] matrix);

    String toCharString(String biteString);
}
