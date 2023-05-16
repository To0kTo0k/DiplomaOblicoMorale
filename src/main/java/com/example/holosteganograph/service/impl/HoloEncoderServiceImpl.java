package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.service.HoloEncoderService;
import com.fasterxml.uuid.Generators;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HoloEncoderServiceImpl implements HoloEncoderService {

    @Override
    public byte[] encodeTextToHoloInBytes(String text) {
        UUID uuid = Generators.timeBasedGenerator().generate();
        String filename = "encode" + uuid.toString() + ".png";
        String string = toBinaryString(text);
        string = expandBinaryString(string);
        boolean[][] matrix = binaryStringToBinaryMatrix(string);
        binaryMatrixToImage(matrix, filename);
        Mat hologram = imageToHologram(filename);
        try {
            Files.delete(Path.of(filename));
        } catch (IOException ignored) {}
        return holoToBytes(hologram);
    }

    /**
     * Приведение текстовой строки к битовой строке
     **/
    @Override
    public String toBinaryString(String string) {
        char[] charString = string.toCharArray();
        StringBuilder biteString = new StringBuilder();
        for (char s : charString) {
            StringBuilder symbolInBites = new StringBuilder(Integer.toBinaryString(s));
            while (symbolInBites.length() < 16) {
                symbolInBites.insert(0, "0");
            }
            biteString.append(symbolInBites);
        }
        return biteString.toString();
    }

    /**
     * Расширение битовой строки битовыми кодами пробелов до квадратной матрице с числом элементов кратным 16
     **/
    @Override
    public String expandBinaryString(String string) {
        StringBuilder spaceSymbol = new StringBuilder("0000000000100000");
        StringBuilder expandedString = new StringBuilder(string);
        int i = getBinaryMatrixSize(string.length());
        while (expandedString.length() < i * i) {
            expandedString.append(spaceSymbol);
        }
        return expandedString.toString();
    }

    /**
     * Преобразование битовой строки в бинарную матрицу
     **/
    @Override
    public boolean[][] binaryStringToBinaryMatrix(String charString) {
        int i = getBinaryMatrixSize(charString.length());
        boolean[][] matrixBinary = new boolean[i][i];
        int counter = 0;
        for (int row = 0; row < i; row++) {
            for (int column = 0; column < i; column++) {
                matrixBinary[row][column] = '1' == charString.charAt(counter);
                counter++;
            }
        }
        return matrixBinary;
    }

    /**
     * Подбор размера для квадратной бинарной матрицы с числом элементов кратным 16
     **/
    @Override
    public int getBinaryMatrixSize(int stringSize) {
        int i = 4;
        while (true) {
            if (((i * i) % 16 == 0) && (i * i >= stringSize)) {
                return i;
            }
            i += 2;
        }
    }

    /**
     * Преобразование бинарной матрицы в изображение
     **/
    @Override
    public void binaryMatrixToImage(boolean[][] matrix, String filename) {
        try {
            BufferedImage image = new BufferedImage(matrix.length, matrix.length, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix.length; j++) {
                    Color newColor;
                    if (matrix[i][j]) {
                        newColor = new Color(0, 0, 0);
                    } else {
                        newColor = new Color(255, 255, 255);
                    }
                    image.setRGB(i, j, newColor.getRGB());
                }
            }
            File output = new File(filename);
            ImageIO.write(image, "png", output);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Преобразование изображения в голограмму
     **/
    @Override
    public Mat imageToHologram(String filename) {
        Mat image = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE);

        image.convertTo(image, CvType.CV_32F);

        java.util.List<Mat> planes = new ArrayList<>();
        planes.add(image);
        planes.add(Mat.zeros(image.size(), CvType.CV_32F));

        Mat complex = new Mat();
        Core.merge(planes, complex);

        Core.dft(complex, complex);

        return complex;
    }

    @Override
    public byte[] holoToBytes(Mat hologram) {
        List<Mat> planes = new ArrayList<>();
        Core.split(hologram, planes);

        float[] firstPlaneFloat = new float[planes.get(0).rows() * planes.get(0).cols() * (int) planes.get(0).elemSize()];
        float[] secondPlaneFloat = new float[planes.get(1).rows() * planes.get(1).cols() * (int) planes.get(1).elemSize()];

        planes.get(0).get(0, 0, firstPlaneFloat);
        planes.get(1).get(0, 0, secondPlaneFloat);

        byte[] firstPlaneByte = floatsToBytes(firstPlaneFloat);
        byte[] secondPlaneByte = floatsToBytes(secondPlaneFloat);

        int matrixSize = (int) hologram.size().height;

        byte[] matrixSizeByte = ByteBuffer
                .allocate(4)
                .putInt(matrixSize)
                .array();

        byte[] hologramInBytesSize = ByteBuffer
                .allocate(4)
                .putInt(firstPlaneByte.length + secondPlaneByte.length)
                .array();

        byte[] combined = new byte[
                matrixSizeByte.length
                        + hologramInBytesSize.length
                        + firstPlaneByte.length
                        + secondPlaneByte.length
                ];

        ByteBuffer buffer = ByteBuffer.wrap(combined);
        buffer.put(matrixSizeByte);
        buffer.put(hologramInBytesSize);
        buffer.put(firstPlaneByte);
        buffer.put(secondPlaneByte);

        return buffer.array();
    }

    /**
     * Преобразование массива float в массив байтов
     **/
    @Override
    public byte[] floatsToBytes(float[] floats) {
        byte[] bytes = new byte[Float.BYTES * floats.length];
        ByteBuffer.wrap(bytes).asFloatBuffer().put(floats);
        return bytes;
    }
}
