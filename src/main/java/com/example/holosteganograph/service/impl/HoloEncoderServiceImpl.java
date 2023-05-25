package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.exceptions.FloatsToBytesTransformationException;
import com.example.holosteganograph.exceptions.HideBytesInImageException;
import com.example.holosteganograph.exceptions.PreholoImageCreationException;
import com.example.holosteganograph.model.IOContent;
import com.example.holosteganograph.service.HoloEncoderService;
import com.fasterxml.uuid.Generators;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HoloEncoderServiceImpl implements HoloEncoderService {

    @Override
    public void textToSteganography(MultipartFile file, String text, IOContent content, Path uploadDirectory)
            throws FileNotUploadedException,
            CacheImageDeletingException,
            FloatsToBytesTransformationException,
            HideBytesInImageException,
            PreholoImageCreationException {
        try (InputStream inputStream = file.getInputStream()) {
            UUID uuid = Generators.timeBasedGenerator().generate();
            String filename = uuid.toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadDirectory.resolve(filename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            content.setFilename(filePath.toString());
            content.setText(text);
            byte[] bytes = encodeTextToHoloInBytes(content.getText());
            hideBytesInImage(bytes, content.getFilename());
        } catch (IOException e) {
            throw new FileNotUploadedException("Error get file as InputStream " + e.getMessage());
        } catch (CacheImageDeletingException e) {
            throw new CacheImageDeletingException(e.getMessage());
        } catch (FloatsToBytesTransformationException e) {
            throw new FloatsToBytesTransformationException(e.getMessage());
        } catch (HideBytesInImageException e) {
            throw new HideBytesInImageException(e.getMessage());
        } catch (PreholoImageCreationException e) {
            throw new PreholoImageCreationException(e.getMessage());
        }
    }

    private byte[] encodeTextToHoloInBytes(String text)
            throws CacheImageDeletingException,
            FloatsToBytesTransformationException,
            PreholoImageCreationException {
        UUID uuid = Generators.timeBasedGenerator().generate();
        String filename = "encode" + uuid.toString() + ".png";
        String string = toBinaryString(text);
        string = expandBinaryString(string);
        boolean[][] matrix = binaryStringToBinaryMatrix(string);
        binaryMatrixToImage(matrix, filename);
        Mat hologram = imageToHologram(filename);
        try {
            Files.delete(Path.of(filename));
        } catch (IOException e) {
            throw new CacheImageDeletingException("Error deleting cache files " + e.getMessage());
        }
        return holoToBytes(hologram);
    }

    private String toBinaryString(String string) {
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

    private String expandBinaryString(String string) {
        String utf16SpaceSymbol = "0000000000100000";
        var spaceSymbol = new StringBuilder(utf16SpaceSymbol);
        var expandedString = new StringBuilder(string);
        int i = getBinaryMatrixSize(string.length());
        while (expandedString.length() < i * i) {
            expandedString.append(spaceSymbol);
        }
        return expandedString.toString();
    }

    private int getBinaryMatrixSize(int stringSize) {
        int i = 4;
        while (true) {
            if (((i * i) % 16 == 0) && (i * i >= stringSize)) {
                return i;
            }
            i += 2;
        }
    }

    private boolean[][] binaryStringToBinaryMatrix(String charString) {
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

    private void binaryMatrixToImage(boolean[][] matrix, String filename) throws PreholoImageCreationException {
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
            throw new PreholoImageCreationException("Error preholo image creation " + e.getMessage());
        }
    }

    private Mat imageToHologram(String filename) {
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

    private byte[] holoToBytes(Mat hologram) throws FloatsToBytesTransformationException {
        List<Mat> planes = new ArrayList<>();
        Core.split(hologram, planes);

        float[] firstPlaneFloat = new float
                [planes.get(0).rows() * planes.get(0).cols() * (int) planes.get(0).elemSize()];
        float[] secondPlaneFloat = new float
                [planes.get(1).rows() * planes.get(1).cols() * (int) planes.get(1).elemSize()];

        planes.get(0).get(0, 0, firstPlaneFloat);
        planes.get(1).get(0, 0, secondPlaneFloat);

        byte[] firstPlaneByte = floatsToBytes(firstPlaneFloat);
        byte[] secondPlaneByte = floatsToBytes(secondPlaneFloat);

        int matrixSize = (int) hologram.size().height;

        byte[] matrixSizeByte = intToBytes(matrixSize);

        byte[] hologramInBytesSize = intToBytes(firstPlaneByte.length);

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

    private byte[] floatsToBytes(float[] floats) throws FloatsToBytesTransformationException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (float aFloat : floats) {
                int intBits = Float.floatToIntBits(aFloat);
                byte[] bytes = {(byte) (intBits >> 24), (byte) (intBits >> 16), (byte) (intBits >> 8), (byte) (intBits)};
                outputStream.write(bytes);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new FloatsToBytesTransformationException("Error floats to bytes transformation " + e.getMessage());
        }
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    private void hideBytesInImage(byte[] bytes, String path) throws HideBytesInImageException {
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
            throw new HideBytesInImageException("Error hide bytes of holo to image " + e.getMessage());
        }
    }
}
