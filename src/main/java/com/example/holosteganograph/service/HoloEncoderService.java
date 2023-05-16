package com.example.holosteganograph.service;

import org.opencv.core.Mat;

public interface HoloEncoderService {
    byte[] encodeTextToHoloInBytes(String text);

    String toBinaryString(String string);

    String expandBinaryString(String string);

    boolean[][] binaryStringToBinaryMatrix(String charString);

    int getBinaryMatrixSize(int stringSize);

    void binaryMatrixToImage(boolean[][] matrix, String filename);

    Mat imageToHologram(String filename);

    byte[] holoToBytes(Mat hologram);

    byte[] floatsToBytes(float[] floats);
}
