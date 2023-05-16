package com.example.holosteganograph.service;

public interface SteganographService {
    void textToSteganography();

    void SteganographyToText();

    void hideBytesInImage(byte[] bytes, String path);

    byte[] readHidenBytesFromImage(String path);
}
