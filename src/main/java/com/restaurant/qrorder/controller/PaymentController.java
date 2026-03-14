package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.common.PaymentMethod;
import com.restaurant.qrorder.domain.dto.request.CreatePaymentRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Create payment", description = "Create a new payment for a bill (Authenticated users)")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details (Admin/Staff only)")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse response = paymentService.checkPaymentStatus(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by Bill ID
     */
    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get payment by Bill ID", description = "Retrieve payment by bill ID (Admin/Staff only)")
    public ResponseEntity<PaymentResponse> getPaymentByBillId(@PathVariable Long billId) {
        PaymentResponse response = paymentService.getPaymentByBillId(billId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check payment status
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'MANAGER', 'CASHIER')")
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
     * IMPORTANT: This URL must be publicly accessible (use ngrok for local development)
     */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Map<String, Object>> momoIpn(@RequestBody Map<String, String> params) {
        log.info("Received MoMo IPN callback: {}", params);

        try {
            String orderId = params.get("orderId");
            String transId = params.get("transId");
            String resultCode = params.get("resultCode");
            String message = params.get("message");
            String signature = params.get("signature");

            // TODO: Verify signature for security in production
            // if (!paymentService.verifyMoMoSignature(params, signature)) {
            //     log.error("Invalid MoMo signature for order: {}", orderId);
            //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            //         Map.of("status", "error", "message", "Invalid signature")
            //     );
            // }

            paymentService.handleMoMoCallback(orderId, transId, resultCode, message);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Callback processed");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing MoMo IPN callback", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * MoMo notify endpoint (alias for IPN - backward compatibility)
     */
    @PostMapping("/momo/notify")
    public ResponseEntity<Map<String, Object>> momoNotify(@RequestBody Map<String, String> params) {
        return momoIpn(params);
    }

    /**
     * MoMo return URL - where customer is redirected after payment
     * This endpoint can return HTML to redirect to frontend, or JSON for API response
     */
    @GetMapping("/momo/return")
    public ResponseEntity<?> momoReturn(
            @RequestParam String orderId,
            @RequestParam String resultCode,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String extraData) {
        
        log.info("MoMo return - Order: {}, ResultCode: {}, Message: {}", orderId, resultCode, message);

        boolean isSuccess = "0".equals(resultCode);
        String status = isSuccess ? "success" : "failed";
        String displayMessage = isSuccess ? "Payment successful!" : (message != null ? message : "Payment failed");

        // Option 1: Return JSON response (for API testing)
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", displayMessage);
        response.put("orderId", orderId);
        response.put("resultCode", resultCode);
        
        // TODO: For production, redirect to frontend with payment status
        // String frontendUrl = String.format(
        //     "http://localhost:3000/payment/%s?orderId=%s&status=%s",
        //     status, orderId, resultCode
        // );
        // return ResponseEntity.status(HttpStatus.FOUND)
        //     .header("Location", frontendUrl)
        //     .build();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{reservationId}/deposit")
    @Operation(summary = "Pay reservation deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> payReservationDeposit(
            @PathVariable Long reservationId,
            @RequestParam PaymentMethod request) {

        PaymentResponse response = paymentService.createReservationDepositPayment(reservationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<PaymentResponse>builder()
                        .statusCode(201)
                        .message("Deposit payment created successfully")
                        .data(response)
                        .build());

     //  return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PostMapping("/{paymentId}/confirm-deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    @Operation(summary = "Manually confirm deposit payment and confirm reservation")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmDeposit(
            @PathVariable Long paymentId) {

        PaymentResponse response = paymentService.confirmDepositPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .statusCode(200)
                .message("Deposit confirmed — reservation is now CONFIRMED")
                .data(response)
                .build());
    }

    @GetMapping("/reservations/{reservationId}/deposit/status")
    @Operation(summary = "Check deposit payment status and auto-confirm reservation")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkDepositStatus(
            @PathVariable Long reservationId) {

        PaymentResponse response = paymentService.checkAndConfirmDeposit(reservationId);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .statusCode(200)
                .message("Deposit status checked")
                .data(response)
                .build());
    }

    @PostMapping("/momo/confirm-order")
    @Operation(summary = "Confirm MoMo order status manually from frontend")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmMomoOrder(@RequestParam String orderId) {
        // Gọi service và nhận về đối tượng response thay vì void
        PaymentResponse response = paymentService.handleMoMoCallback(orderId, null, "0", "Manual confirmation");

        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .statusCode(200)
                .message("Payment status synchronized")
                .data(response)
                .build());
    }
}
