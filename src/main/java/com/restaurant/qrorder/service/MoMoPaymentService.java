package com.restaurant.qrorder.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for MoMo payment integration
 * Reference: https://developers.momo.vn/v3/
 */
@Service
@Slf4j
public class MoMoPaymentService {

    @Value("${momo.partner-code:MOMO_PARTNER_CODE}")
    private String partnerCode;

    @Value("${momo.access-key:MOMO_ACCESS_KEY}")
    private String accessKey;

    @Value("${momo.secret-key:MOMO_SECRET_KEY}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Value("${momo.return-url:http://localhost:8080/api/payments/momo/return}")
    private String defaultReturnUrl;

    @Value("${momo.notify-url:http://localhost:8080/api/payments/momo/notify}")
    private String notifyUrl;

    /**
     * Create MoMo payment
     */
    public MoMoPaymentResult createPayment(
            String orderId,
            String requestId,
            BigDecimal amount,
            String orderInfo,
            String returnUrl,
            String customerName,
            String customerPhone) {

        try {
            String requestType = "captureWallet";
            String redirectUrl = returnUrl != null ? returnUrl : defaultReturnUrl;

            // Build raw signature
            String rawSignature = String.format(
                    "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    accessKey,
                    amount.longValue(),
                    "",  // extraData
                    notifyUrl,
                    orderId,
                    orderInfo,
                    partnerCode,
                    redirectUrl,
                    requestId,
                    requestType
            );

            // Generate signature
            String signature = hmacSHA256(rawSignature, secretKey);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("partnerName", "Restaurant QR Order");
            requestBody.put("storeId", "RestaurantStore");
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount.longValue());
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", redirectUrl);
            requestBody.put("ipnUrl", notifyUrl);
            requestBody.put("requestType", requestType);
            requestBody.put("extraData", "");
            requestBody.put("lang", "vi");
            requestBody.put("signature", signature);

            // In production, use HTTP client to call MoMo API
            // For now, return mock response
            log.info("Creating MoMo payment - Order: {}, Amount: {}", orderId, amount);

            // Mock payment URL
            String payUrl = String.format(
                    "https://test-payment.momo.vn/v2/gateway/pay?orderId=%s&amount=%d",
                    orderId, amount.longValue()
            );

            return MoMoPaymentResult.builder()
                    .success(true)
                    .payUrl(payUrl)
                    .orderId(orderId)
                    .requestId(requestId)
                    .message("Payment created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            return MoMoPaymentResult.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Check payment status with MoMo
     */
    public MoMoPaymentStatus checkPaymentStatus(String requestId) {
        try {
            // In production, call MoMo query API
            // For now, return mock status
            log.info("Checking MoMo payment status for request: {}", requestId);

            return MoMoPaymentStatus.builder()
                    .completed(false)
                    .failed(false)
                    .pending(true)
                    .message("Payment is pending")
                    .build();

        } catch (Exception e) {
            log.error("Error checking MoMo payment status", e);
            return MoMoPaymentStatus.builder()
                    .failed(true)
                    .message("Error checking status: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Process refund with MoMo
     */
    public boolean refundPayment(String transId, BigDecimal refundAmount, String reason) {
        try {
            log.info("Processing MoMo refund - TransID: {}, Amount: {}, Reason: {}", 
                    transId, refundAmount, reason);

            // In production, call MoMo refund API
            // For now, return success
            return true;

        } catch (Exception e) {
            log.error("Error processing MoMo refund", e);
            return false;
        }
    }

    /**
     * Verify MoMo callback signature
     */
    public boolean verifySignature(Map<String, String> params, String signature) {
        try {
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                    accessKey,
                    params.get("amount"),
                    params.getOrDefault("extraData", ""),
                    params.get("message"),
                    params.get("orderId"),
                    params.get("orderInfo"),
                    params.get("orderType"),
                    params.get("partnerCode"),
                    params.get("payType"),
                    params.get("requestId"),
                    params.get("responseTime"),
                    params.get("resultCode"),
                    params.get("transId")
            );

            String computedSignature = hmacSHA256(rawSignature, secretKey);
            return computedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Error verifying MoMo signature", e);
            return false;
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String hmacSHA256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA256", e);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoMoPaymentResult {
        private boolean success;
        private String payUrl;
        private String orderId;
        private String requestId;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoMoPaymentStatus {
        private boolean completed;
        private boolean failed;
        private boolean pending;
        private String transId;
        private String message;
    }
}
