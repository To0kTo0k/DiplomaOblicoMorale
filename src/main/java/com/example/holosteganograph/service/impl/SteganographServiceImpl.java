package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.dto.HoloSteganographDto;
import com.example.holosteganograph.service.HoloDecoderService;
import com.example.holosteganograph.service.HoloEncoderService;
import com.example.holosteganograph.service.SteganographService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class SteganographServiceImpl implements SteganographService {

    @Autowired
    HoloEncoderService encoderService;

    @Autowired
    HoloDecoderService decoderService;

    /**
     * Хранит текст и картинку
     **/
    @Autowired
    HoloSteganographDto steganographDto;

    /**
     * Прячет заданный текст в заданную картинку
     **/
    @Override
    public void textToSteganography() {
        byte[] bytes = encoderService.encodeTextToHoloInBytes(steganographDto.getText());
        hideBytesInImage(bytes, steganographDto.getFilename());
    }

    /**
     * Достает текст из картинки
     **/
    @Override
    public void SteganographyToText() {
        byte[] bytes = readHidenBytesFromImage(steganographDto.getFilename());
        steganographDto.setText(decoderService.decodeHoloInBytesToText(bytes));
    }

    @Override
    public void hideBytesInImage(byte[] bytes, String path) {
        try {
            BufferedImage image = ImageIO.read(new File(path));
            int byteIndex = 0;

            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    if (byteIndex >= bytes.length) {
                        File output = new File(path);
                        ImageIO.write(image, "png", output);
                        return;
                    }

                    byte aByte = bytes[byteIndex];
                    int pixel = image.getRGB(i, j);
                    int a = (pixel >> 24) & 0xff;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;

                    b = (b & ~3) | (aByte & 3);
                    aByte >>= 2;
                    g = (g & ~3) | (aByte & 3);
                    aByte >>= 2;
                    r = (r & ~3) | (aByte & 3);
                    aByte >>= 2;
                    a = (a & ~3) | (aByte & 3);

                    pixel = (a << 24) | (r << 16) | (g << 8) | b;
                    image.setRGB(i, j, pixel);

                    byteIndex++;
                }

            }
        } catch (IOException e) {
            System.out.println("Warning hide bytes in image");
        }
    }

    @Override
    public byte[] readHidenBytesFromImage(String path) {
        try {

            BufferedImage image = ImageIO.read(new File(path));

            byte[] bytes = new byte[image.getWidth() * image.getHeight()];

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
            System.out.println("Warning read hiden bytes from image");
            return new byte[0];
        }
    }
}
