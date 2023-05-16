package com.example.holosteganograph.service;

import org.opencv.core.Mat;

import java.io.IOException;

public interface HoloEncoderService {
    byte[] encodeTextToHoloInBytes(String text) throws IOException;

    String toBinaryString(String string);

    String expandBinaryString(String string);

    boolean[][] binaryStringToBinaryMatrix(String charString);

    int getBinaryMatrixSize(int stringSize);

    void binaryMatrixToImage(boolean[][] matrix, String filename);

    Mat imageToHologram(String filename);

    byte[] holoToBytes(Mat hologram);

    byte[] floatsToBytes(float[] floats);
}
