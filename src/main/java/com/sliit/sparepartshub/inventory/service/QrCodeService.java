package com.sliit.sparepartshub.inventory.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Service
public class QrCodeService {

    public byte[] png(String value, int size) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("QR value is missing.");
        }
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(value.trim(), BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate QR code.", e);
        }
    }

    public String decode(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Choose a QR image first.");
        }
        try {
            BufferedImage buffered = ImageIO.read(image.getInputStream());
            if (buffered == null) {
                throw new IllegalArgumentException("The uploaded file is not a supported image.");
            }
            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(buffered))
            );
            String value = new MultiFormatReader().decode(bitmap).getText();
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("No QR value was found in the image.");
            }
            return value.trim();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read a valid QR code from that image.");
        }
    }
}
