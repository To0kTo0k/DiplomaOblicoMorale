package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.service.HoloDecoderService;
import com.fasterxml.uuid.Generators;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class HoloDecoderServiceImpl implements HoloDecoderService {

    @Override
    public String decodeHoloInBytesToText(byte[] bytes) {
        UUID uuid = Generators.timeBasedGenerator().generate();
        String filename = "decode" + uuid.toString() + ".png";

        Mat hologram = bytesToHolo(bytes);
        hologramToImage(hologram, filename);
        boolean[][] matrix = imageToBinaryMatrix(filename);
        String string = binaryMatrixToBinaryString(matrix);
        try {
            Files.delete(Path.of(filename));
        } catch (IOException ignored) {}
        return toCharString(string);
    }

    @Override
    public Mat bytesToHolo(byte[] bytes) {
        bytes = Arrays.copyOfRange(bytes, 0, 32776);

        int matrixSize = ByteBuffer.wrap(bytes).getInt();
        bytes = Arrays.copyOfRange(bytes, 4, bytes.length);


        int hologramBytesSize = ByteBuffer.wrap(bytes).getInt();
        bytes = Arrays.copyOfRange(bytes, 4, bytes.length);

        hologramBytesSize = hologramBytesSize / 2;

        byte[] firstPlaneByte = Arrays.copyOfRange(bytes, 0, hologramBytesSize);
        bytes = Arrays.copyOfRange(bytes, hologramBytesSize, bytes.length);
        byte[] secondPlaneByte = Arrays.copyOfRange(bytes, 0, hologramBytesSize);

        float[] firstPlaneFloat = bytesToFloats(firstPlaneByte);
        float[] secondPlaneFloat = bytesToFloats(secondPlaneByte);

        Size size = new Size(matrixSize, matrixSize);

        Mat firstPlane = new Mat(size, CvType.CV_32F);
        Mat secondPlane = new Mat(size, CvType.CV_32F);

        firstPlane.put(0, 0, firstPlaneFloat);
        secondPlane.put(0, 0, secondPlaneFloat);

        List<Mat> planes = new ArrayList<>();
        planes.add(firstPlane);
        planes.add(secondPlane);

        Mat complex = new Mat();
        Core.merge(planes, complex);

        return complex;
    }

    /**
     * Преобразование массива байтов в массив float
     **/
    private float[] bytesToFloats(byte[] bytes) {
        if (bytes.length % Float.BYTES != 0)
            throw new RuntimeException("Illegal length");
        float[] floats = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes).asFloatBuffer().get(floats);
        return floats;
    }

    /**
     * Преобразование голограммы в изображение
     **/
    @Override
    public void hologramToImage(Mat hologram, String filename) {
        Core.idft(hologram, hologram);

        Mat out = new Mat();
        List<Mat> planes = new ArrayList<>();
        Core.split(hologram, planes);
        Core.normalize(planes.get(0), out, 0, 255, Core.NORM_MINMAX);
        out.convertTo(out, CvType.CV_8U);
        Imgcodecs.imwrite(filename, out);
    }

    /**
     * Преобразование изображения в бинарную матрицу
     **/
    @Override
    public boolean[][] imageToBinaryMatrix(String filename) {
        try {
            BufferedImage image = ImageIO.read(new File(filename));
            boolean[][] matrix = new boolean[image.getHeight()][image.getWidth()];
            for (int i = 0; i < image.getHeight(); i++) {
                for (int j = 0; j < image.getWidth(); j++) {
                    int rgba = image.getRGB(i, j);
                    int color = (rgba >> 16) & 255;
                    matrix[i][j] = color == 0;
                }
            }
            return matrix;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return new boolean[0][0];
        }
    }

    @Override
    public String binaryMatrixToBinaryString(boolean[][] matrix) {
        StringBuilder binaryString = new StringBuilder();
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

    /**
     * Приведение битовой строки к текстовой строке
     **/
    @Override
    public String toCharString(String biteString) {
        StringBuilder charString = new StringBuilder();
        for (int i = 0; i < biteString.length() / 16; i++) {
            String symbolInBites = biteString.substring(i * 16, i * 16 + 16);
            charString.append(Character.toChars(Integer.parseInt(symbolInBites, 2)));
        }
        return charString.toString();
    }
}
