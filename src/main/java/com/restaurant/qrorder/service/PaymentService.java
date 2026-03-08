package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.common.PaymentMethod;
import com.restaurant.qrorder.domain.common.PaymentStatus;
import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreatePaymentRequest;
import com.restaurant.qrorder.domain.dto.response.PaymentResponse;
import com.restaurant.qrorder.domain.entity.Bill;
import com.restaurant.qrorder.domain.entity.BillTable;
import com.restaurant.qrorder.domain.entity.Payment;
import com.restaurant.qrorder.domain.entity.RestaurantTable;
import com.restaurant.qrorder.repository.BillRepository;
import com.restaurant.qrorder.repository.PaymentRepository;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final MoMoPaymentService moMoPaymentService;
    private final RestaurantTableRepository tableRepository;

    /**
     * Create payment for a bill
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Bill bill = billRepository.findById(request.getBillId())
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        // Validate bill status
        if (bill.getStatus() == BillStatus.PAID) {
            throw new RuntimeException("Bill is already paid");
        }

        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new RuntimeException("Cannot pay a cancelled bill");
        }

        // Check if payment already exists
        paymentRepository.findByBillId(bill.getId())
                .ifPresent(existingPayment -> {
                    if (existingPayment.getStatus() == PaymentStatus.COMPLETED) {
                        throw new RuntimeException("Bill already has a completed payment");
                    }
                    // Delete pending/failed payments to create new one
                    paymentRepository.delete(existingPayment);
                });

        // Get amount from bill
        BigDecimal amount = bill.getFinalPrice();

        Payment payment;

        if (PaymentMethod.CASH.equals(request.getPaymentMethod())) {
            payment = createCashPayment(bill, amount);
        } else if (PaymentMethod.MOMO.equals(request.getPaymentMethod())) {
            payment = createMoMoPayment(bill, request);
        } else {
            throw new RuntimeException("Payment method not supported yet");
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Created payment ID: {} for bill ID: {} with method: {}", 
                saved.getId(), bill.getId(), request.getPaymentMethod());

        return mapToResponse(saved);
    }

    /**
     * Create cash payment (immediate)
     */
    private Payment createCashPayment(Bill bill, BigDecimal amount) {
        Payment payment = Payment.builder()
                .bill(bill)
                .method(PaymentMethod.CASH)
                .amount(amount)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();

        // Update bill status
        bill.setStatus(BillStatus.PAID);
        List<BillTable> billTableList = bill.getBillTables();
        for (BillTable billTable : bill.getBillTables()) {
            RestaurantTable table = billTable.getTable();
            table.setStatus(TableStatus.AVAILABLE);
            log.info("Updated table with id:",table.getId() );
            tableRepository.save(table);
        }
        bill.setClosedAt(LocalDateTime.now());
        billRepository.save(bill);

        return payment;
    }

    /**
     * Create MoMo payment (requires payment URL)
     */
    private Payment createMoMoPayment(Bill bill, CreatePaymentRequest request) {
        String orderId = "BILL_" + bill.getId() + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        BigDecimal amount = bill.getFinalPrice();

        // Call MoMo service to get payment URL
        MoMoPaymentService.MoMoPaymentResult momoResult = moMoPaymentService.createPayment(
                orderId,
                requestId,
                amount,
                "Payment for Bill #" + bill.getId(),
                request.getReturnUrl()
        );

        if (!momoResult.isSuccess()) {
            throw new RuntimeException("Failed to create MoMo payment: " + momoResult.getMessage());
        }

        return Payment.builder()
                .bill(bill)
                .method(PaymentMethod.MOMO)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .momoOrderId(orderId)
                .momoRequestId(requestId)
                .paymentUrl(momoResult.getPayUrl())
                .build();
    }

    /**
     * Handle MoMo payment callback/IPN
     */
    @Transactional
    public void handleMoMoCallback(String orderId, String transId, String resultCode, String message) {
        Payment payment = paymentRepository.findByMomoOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

        payment.setMomoTransId(transId);
        payment.setGatewayResponse(message);

        if ("0".equals(resultCode)) {
            // Payment successful
            payment.markAsPaid();
            
            // Update bill
            Bill bill = payment.getBill();
            bill.setStatus(BillStatus.PAID);
            for (BillTable billTable : bill.getBillTables()) {
                RestaurantTable table = billTable.getTable();
                table.setStatus(TableStatus.AVAILABLE);
                log.info("Updated table with id:",table.getId() );
                tableRepository.save(table);
            }
            bill.setClosedAt(LocalDateTime.now());
            billRepository.save(bill);

            log.info("MoMo payment completed for order: {}, Bill ID: {}", orderId, bill.getId());
        } else {
            // Payment failed
            payment.markAsFailed("MoMo payment failed: " + message);
            log.error("MoMo payment failed for order: {}, Code: {}, Message: {}", 
                    orderId, resultCode, message);
        }

        paymentRepository.save(payment);
    }

    /**
     * Process refund
     */
    @Transactional
    public PaymentResponse processRefund(Long paymentId, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Can only refund completed payments");
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed payment amount");
        }

        if (PaymentMethod.MOMO.equals(payment.getMethod())) {
            // Call MoMo refund API
            moMoPaymentService.refundPayment(
                    payment.getMomoTransId(),
                    refundAmount,
                    reason
            );
        }

        payment.processRefund(refundAmount, reason);

        // Update bill status
        Bill bill = payment.getBill();
        bill.setStatus(BillStatus.CANCELLED);
        billRepository.save(bill);

        Payment saved = paymentRepository.save(payment);

        log.info("Processed refund for payment ID: {}, Amount: {}", paymentId, refundAmount);

        return mapToResponse(saved);
    }

    /**
     * Get payment by Bill ID
     */
    public PaymentResponse getPaymentByBillId(Long billId) {
        Payment payment = paymentRepository.findByBillId(billId)
                .orElseThrow(() -> new RuntimeException("Payment not found for bill"));
        return mapToResponse(payment);
    }

    /**
     * Check payment status
     */
    @Transactional
    public PaymentResponse checkPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // If MoMo and pending, query MoMo for status
        if (PaymentMethod.MOMO.equals(payment.getMethod()) ) {

            MoMoPaymentService.MoMoPaymentStatus status =
                    moMoPaymentService.checkPaymentStatus(payment.getMomoOrderId(), payment.getMomoRequestId());

            PaymentStatus newStatus = status.toPaymentStatus();

            payment.setStatus(newStatus);

            if (newStatus == PaymentStatus.COMPLETED) {
                handleMoMoCallback(
                        payment.getMomoOrderId(),
                        status.getTransId(),
                        "0",
                        "Payment completed"
                );
            }

            paymentRepository.save(payment);
        }

        return mapToResponse(payment);
    }

    /**
     * Verify MoMo signature for IPN callback
     * @param params MoMo callback parameters
     * @param signature Signature from MoMo
     * @return true if signature is valid
     */
    public boolean verifyMoMoSignature(java.util.Map<String, String> params, String signature) {
        return moMoPaymentService.verifySignature(params, signature);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .billId(payment.getBill().getId())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentUrl(payment.getPaymentUrl())
                .momoOrderId(payment.getMomoOrderId())
                .momoRequestId(payment.getMomoRequestId())
                .errorMessage(payment.getErrorMessage())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
