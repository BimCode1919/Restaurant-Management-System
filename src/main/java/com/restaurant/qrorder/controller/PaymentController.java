package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreatePaymentRequest;
import com.restaurant.qrorder.domain.dto.response.PaymentResponse;
import com.restaurant.qrorder.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "APIs for managing payments and MoMo integration")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create payment for a bill
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Create payment", description = "Create a new payment for a bill (Authenticated users)")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details (Admin/Staff only)")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse response = paymentService.checkPaymentStatus(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by Bill ID
     */
    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get payment by Bill ID", description = "Retrieve payment by bill ID (Admin/Staff only)")
    public ResponseEntity<PaymentResponse> getPaymentByBillId(@PathVariable Long billId) {
        PaymentResponse response = paymentService.getPaymentByBillId(billId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check payment status
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Check payment status", description = "Check current payment status (Authenticated users)")
    public ResponseEntity<PaymentResponse> checkPaymentStatus(@PathVariable Long id) {
        PaymentResponse response = paymentService.checkPaymentStatus(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Process refund
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process refund", description = "Process payment refund (Admin only)")
    public ResponseEntity<PaymentResponse> processRefund(
            @PathVariable Long id,
            @RequestParam BigDecimal refundAmount,
            @RequestParam String reason) {
        
        PaymentResponse response = paymentService.processRefund(id, refundAmount, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * MoMo IPN (Instant Payment Notification) callback
     * This endpoint is called by MoMo server after payment
     */
    @PostMapping("/momo/notify")
    public ResponseEntity<Map<String, Object>> momoNotify(@RequestBody Map<String, String> params) {
        log.info("Received MoMo IPN callback: {}", params);

        try {
            String orderId = params.get("orderId");
            String transId = params.get("transId");
            String resultCode = params.get("resultCode");
            String message = params.get("message");
            // String signature = params.get("signature"); // TODO: Verify signature

            // TODO: Verify signature for security
            // if (!momoService.verifySignature(params, signature)) {
            //     return error response
            // }

            paymentService.handleMoMoCallback(orderId, transId, resultCode, message);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Callback processed");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * MoMo return URL - where customer is redirected after payment
     */
    @GetMapping("/momo/return")
    public ResponseEntity<Map<String, Object>> momoReturn(
            @RequestParam String orderId,
            @RequestParam String resultCode,
            @RequestParam(required = false) String message) {
        
        log.info("MoMo return - Order: {}, ResultCode: {}", orderId, resultCode);

        Map<String, Object> response = new HashMap<>();
        
        if ("0".equals(resultCode)) {
            response.put("status", "success");
            response.put("message", "Payment successful");
        } else {
            response.put("status", "failed");
            response.put("message", message != null ? message : "Payment failed");
        }
        
        response.put("orderId", orderId);
        
        return ResponseEntity.ok(response);
    }
}
