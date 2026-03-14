package com.restaurant.qrorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.qrorder.domain.common.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @Value("${momo.query-url:https://test-payment.momo.vn/v2/gateway/api/query}")
    private String momoQueryUrl; // ✅ add this with the other @Value fields

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create MoMo payment
     * Documentation: https://developers.momo.vn/v3/
     */
    public MoMoPaymentResult createPayment(
            String orderId,
            String requestId,
            BigDecimal amount,
            String orderInfo,
            String returnUrl, String notifyUrl) {

        try {
            String requestType = "captureWallet";
            String redirectUrl = returnUrl != null ? returnUrl : defaultReturnUrl;
            String extraData = "";

            // Build raw signature (theo đúng thứ tự của MoMo v2 API)
            String rawSignature = String.format(
                    "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    accessKey,
                    amount.longValue(),
                    extraData,
                    notifyUrl,
                    orderId,
                    orderInfo,
                    partnerCode,
                    redirectUrl,
                    requestId,
                    requestType
            );

            log.debug("MoMo raw signature: {}", rawSignature);

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
            requestBody.put("extraData", extraData);
            requestBody.put("lang", "vi");
            requestBody.put("signature", signature);

            log.info("Creating MoMo payment - Order: {}, Amount: {}", orderId, amount);
            log.debug("MoMo request body: {}", objectMapper.writeValueAsString(requestBody));

            // Call MoMo API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new RuntimeException("MoMo API returned empty response");
            }

            log.info("MoMo API response: {}", objectMapper.writeValueAsString(responseBody));

            // MoMo v2 API returns: resultCode, message, payUrl (hoặc deeplink)
            String resultCode = String.valueOf(responseBody.get("resultCode"));
            String message = String.valueOf(responseBody.get("message"));
            
            if (!"0".equals(resultCode)) {
                log.error("MoMo payment creation failed - Code: {}, Message: {}", resultCode, message);
                return MoMoPaymentResult.builder()
                        .success(false)
                        .message("MoMo Error: " + message)
                        .build();
            }

            // Lấy payment URL từ response
            String payUrl = String.valueOf(responseBody.get("payUrl"));
            if (payUrl == null || "null".equals(payUrl)) {
                payUrl = String.valueOf(responseBody.get("deeplink"));
            }

            return MoMoPaymentResult.builder()
                    .success(true)
                    .payUrl(payUrl)
                    .orderId(orderId)
                    .requestId(requestId)
                    .message(message)
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
    public MoMoPaymentStatus checkPaymentStatus(String momoOrderId, String momoRequestId) {
        try {
            String rawHash = "accessKey=" + accessKey +
                    "&orderId=" + momoOrderId +
                    "&partnerCode=" + partnerCode +
                    "&requestId=" + momoRequestId;

            String signature = hmacSHA256(rawHash, secretKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("partnerCode", partnerCode);
            body.put("requestId",   momoRequestId);
            body.put("orderId",     momoOrderId);
            body.put("lang",        "en");
            body.put("signature",   signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    momoQueryUrl,   // e.g. https://test-payment.momo.vn/v2/gateway/api/query
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> result = response.getBody();
            if (result == null) {
                return MoMoPaymentStatus.builder().failed(true).message("Empty response from MoMo").build();
            }

            int resultCode = ((Number) result.get("resultCode")).intValue();
            String message  = (String) result.getOrDefault("message", "");
            String transId  = String.valueOf(result.getOrDefault("transId", ""));

            return MoMoPaymentStatus.builder()
                    .completed(resultCode == 0)
                    .failed(resultCode != 0 && resultCode != 1000)
                    .pending(resultCode == 1000)   // 1000 = still processing
                    .resultCode(resultCode)
                    .message(message)
                    .transId(transId)
                    .build();

        } catch (Exception e) {
            log.error("Error checking MoMo payment status for order {}", momoOrderId, e);
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
            log.info("Processing MoMo refund - TransID: {}, Amount  : {}, Reason: {}",
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
        private int     resultCode;
        private String  message;
        private String  transId;

        public PaymentStatus toPaymentStatus() {

            if (isCompleted()) {
                return PaymentStatus.COMPLETED;
            }

            if (isPending()) {
                return PaymentStatus.PENDING;
            }

            if (isFailed()) {
                return PaymentStatus.FAILED;
            }

            return PaymentStatus.PENDING;
        }// ✅ MoMo transaction ID to store on Payment
    }
}
