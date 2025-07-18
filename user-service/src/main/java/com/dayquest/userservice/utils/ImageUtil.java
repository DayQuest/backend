package com.dayquest.userservice.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageUtil {
    @Async
    public CompletableFuture<byte[]> compressImage(MultipartFile originalFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());

        originalImage = fixImageOrientation(originalImage, originalFile).join();

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double scale = Math.min(360.0 / originalWidth, 360.0 / originalHeight);

        int newWidth = (int) Math.round(originalWidth * scale);
        int newHeight = (int) Math.round(originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Write resized image to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);

        return CompletableFuture.completedFuture(baos.toByteArray());
    }

    // Fix orientation based on Exif metadata
    @Async
    public CompletableFuture<BufferedImage> fixImageOrientation(BufferedImage image, MultipartFile originalFile) throws IOException {
        try (InputStream inputStream = originalFile.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);

                switch (orientation) {
                    case 6:
                        return CompletableFuture.completedFuture(rotateImage(image, 90).join());
                    case 3:
                        return CompletableFuture.completedFuture(rotateImage(image, 180).join());
                    case 8:
                        return CompletableFuture.completedFuture(rotateImage(image, -90).join());
                    default:
                        return CompletableFuture.completedFuture(image);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not read EXIF metadata: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(image);
    }

    @Async
    public CompletableFuture<BufferedImage> rotateImage(BufferedImage image, int angle) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rotatedImage = new BufferedImage(height, width, image.getType());
        Graphics2D g2d = rotatedImage.createGraphics();

        g2d.rotate(Math.toRadians(angle), height / 2.0, height / 2.0);
        g2d.translate((height - width) / 2.0, (width - height) / 2.0);
        g2d.drawRenderedImage(image, null);
        g2d.dispose();

        return CompletableFuture.completedFuture(rotatedImage);
    }
}

