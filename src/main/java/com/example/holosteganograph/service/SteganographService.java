package com.example.holosteganograph.service;

import java.io.IOException;

public interface SteganographService {
    void textToSteganography() throws IOException;

    void SteganographyToText() throws IOException;

    void hideBytesInImage(byte[] bytes, String path);

    byte[] readHidenBytesFromImage(String path);
}
