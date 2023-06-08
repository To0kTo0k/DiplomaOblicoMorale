package com.example.holosteganograph.service.impl;

import com.example.holosteganograph.exceptions.CacheImageDeletingException;
import com.example.holosteganograph.exceptions.FileNotUploadedException;
import com.example.holosteganograph.exceptions.FloatsToBytesTransformationException;
import com.example.holosteganograph.exceptions.HideBytesInImageException;
import com.example.holosteganograph.exceptions.PreholoImageCreationException;
import com.example.holosteganograph.exceptions.ResourceResponseException;
import com.example.holosteganograph.service.HoloEncoderService;
import com.fasterxml.uuid.Generators;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class HoloEncoderServiceImpl extends HoloServiceImpl implements HoloEncoderService {
    @Override
    public Resource textToSteganography(MultipartFile file, String text, Path uploadDirectory) {
        Path filePath = defineFilepath(file, uploadDirectory);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileNotUploadedException("Error get file as InputStream " + e.getMessage());
        }
        hideBytesInImage(encodeTextToHoloInBytes(text), filePath.toString());
        return codingResponse(filePath.toString());
    }

    private byte[] encodeTextToHoloInBytes(String text) {
        String filename = "encode" + Generators.timeBasedGenerator().generate().toString() + ".png";
        binaryMatrixToImage(binaryStringToBinaryMatrix(expandBinaryString(toBinaryString(text))), filename);
        Mat hologram = imageToHologram(filename);
        try {
            Files.delete(Path.of(filename));
        } catch (IOException e) {
            throw new CacheImageDeletingException("Error deleting cache files " + e.getMessage());
        }
        return holoToBytes(hologram);
    }

    private String toBinaryString(String string) {
        var biteString = new StringBuilder();
        for (char s : string.toCharArray()) {
            var symbolInBites = new StringBuilder(Integer.toBinaryString(s));
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
        var matrixBinary = new boolean[i][i];
        int counter = 0;
        for (int row = 0; row < i; row++) {
            for (int column = 0; column < i; column++) {
                matrixBinary[row][column] = '1' == charString.charAt(counter);
                counter++;
            }
        }
        return matrixBinary;
    }

    private void binaryMatrixToImage(boolean[][] matrix, String filename) {
        try {
            var image = new BufferedImage(matrix.length, matrix.length, BufferedImage.TYPE_INT_RGB);
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
            ImageIO.write(image, "png", new File(filename));
        } catch (IOException e) {
            throw new PreholoImageCreationException("Error preholo image creation " + e.getMessage());
        }
    }

    private Mat imageToHologram(String filename) {
        var image = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE);
        image.convertTo(image, CvType.CV_32F);
        List<Mat> planes = new ArrayList<>();
        planes.add(image);
        planes.add(Mat.zeros(image.size(), CvType.CV_32F));
        var complex = new Mat();
        Core.merge(planes, complex);
        Core.dft(complex, complex);
        return complex;
    }

    private byte[] holoToBytes(Mat hologram) {
        List<Mat> planes = new ArrayList<>();
        Core.split(hologram, planes);
        var firstPlaneFloat = new float
                [planes.get(0).rows() * planes.get(0).cols() * (int) planes.get(0).elemSize()];
        var secondPlaneFloat = new float
                [planes.get(1).rows() * planes.get(1).cols() * (int) planes.get(1).elemSize()];
        planes.get(0).get(0, 0, firstPlaneFloat);
        planes.get(1).get(0, 0, secondPlaneFloat);
        byte[] firstPlaneByte = floatsToBytes(firstPlaneFloat);
        byte[] secondPlaneByte = floatsToBytes(secondPlaneFloat);
        var matrixSize = (int) hologram.size().height;
        byte[] matrixSizeByte = intToBytes(matrixSize);
        byte[] hologramInBytesSize = intToBytes(firstPlaneByte.length);
        var combined = new byte[
                matrixSizeByte.length
                        + hologramInBytesSize.length
                        + firstPlaneByte.length
                        + secondPlaneByte.length
                ];
        var buffer = ByteBuffer.wrap(combined);
        buffer.put(matrixSizeByte);
        buffer.put(hologramInBytesSize);
        buffer.put(firstPlaneByte);
        buffer.put(secondPlaneByte);
        return buffer.array();
    }

    private byte[] floatsToBytes(float[] floats) {
        try {
            var outputStream = new ByteArrayOutputStream();
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

    private void hideBytesInImage(byte[] bytes, String path) {
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
                    int[] rgb = getImagePixel(image, i, j);
                    rgb[3] = (rgb[3] & ~3) | (aByte & 3);
                    aByte >>= 2;
                    rgb[2] = (rgb[2] & ~3) | (aByte & 3);
                    aByte >>= 2;
                    rgb[1] = (rgb[1] & ~3) | (aByte & 3);
                    aByte >>= 2;
                    rgb[0] = (rgb[0] & ~3) | (aByte & 3);

                    int pixel = (rgb[0] << 24) | (rgb[1] << 16) | (rgb[2] << 8) | rgb[3];
                    image.setRGB(i, j, pixel);

                    byteIndex++;
                }
            }
        } catch (IOException e) {
            throw new HideBytesInImageException("Error hide bytes of holo to image " + e.getMessage());
        }
    }

    private Resource codingResponse(String filename) {
        try {
            return new UrlResource(Paths.get(filename).toUri());
        } catch (IOException e) {
            throw new ResourceResponseException("Error sending response as Resource " + e.getMessage());
        }
    }
}
