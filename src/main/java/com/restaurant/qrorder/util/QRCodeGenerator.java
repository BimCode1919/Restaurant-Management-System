package com.restaurant.qrorder.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class QRCodeGenerator {

    @Value("${app.qr.base-url}")
    private String baseUrl;

    @Value("${app.qr.image-path}")
    private String imagePath;

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    public String generateQRCode(Long tableId, String tableNumber) {
        try {
            // Create directory if not exists
            Path directoryPath = Paths.get(imagePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Generate unique QR code identifier
            String qrCodeId = UUID.randomUUID().toString();
            
            // QR content: URL to order page with table info
            String qrContent = String.format("%s/order?table=%d&code=%s", 
                    baseUrl, tableId, qrCodeId);

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    WIDTH,
                    HEIGHT,
                    hints
            );

            // Save QR code image
            String fileName = String.format("table_%s_%s.png", tableNumber, qrCodeId);
            Path filePath = Paths.get(imagePath, fileName);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);

            log.info("QR code generated successfully for table {}: {}", tableNumber, fileName);
            
            return qrCodeId;

        } catch (WriterException | IOException e) {
            log.error("Error generating QR code for table {}: {}", tableNumber, e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public void deleteQRCode(String tableNumber, String qrCodeId) {
        try {
            String fileName = String.format("table_%s_%s.png", tableNumber, qrCodeId);
            Path filePath = Paths.get(imagePath, fileName);
            Files.deleteIfExists(filePath);
            log.info("QR code deleted for table {}", tableNumber);
        } catch (IOException e) {
            log.error("Error deleting QR code: {}", e.getMessage());
        }
    }
}
