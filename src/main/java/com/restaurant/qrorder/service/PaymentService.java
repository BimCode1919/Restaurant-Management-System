package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.*;
import com.restaurant.qrorder.domain.dto.request.CreatePaymentRequest;
import com.restaurant.qrorder.domain.dto.response.PaymentResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.repository.BillRepository;
import com.restaurant.qrorder.repository.PaymentRepository;
import com.restaurant.qrorder.repository.ReservationRepository;
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
    private final ReservationRepository reservationRepository;

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
        if (bill.getStatus() == BillStatus.MERGED) {
            throw new RuntimeException("Bill has been merged into another bill and cannot be paid directly");
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


    @Transactional
    public PaymentResponse createReservationDepositPayment(
            Long reservationId, PaymentMethod request) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        // ─── Validate reservation state ───────────────────────────────────────────
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot pay deposit for a cancelled reservation");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new InvalidOperationException("Reservation is already completed");
        }

        if (reservation.getStatus() == ReservationStatus.SEATED) {
            throw new InvalidOperationException("Reservation is seated");
        }

        if (Boolean.TRUE.equals(reservation.getDepositPaid())) {
            throw new InvalidOperationException("Deposit already paid for this reservation");
        }
        if (!Boolean.TRUE.equals(reservation.getDepositRequired())) {
            throw new InvalidOperationException("This reservation does not require a deposit");
        }

        // ─── Get the bill linked to this reservation ──────────────────────────────
        Bill bill = reservation.getBill();
        if (bill == null) {
            throw new InvalidOperationException("No bill found for this reservation");
        }

        BigDecimal depositAmount = reservation.getDepositAmount();
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Invalid deposit amount on reservation");
        }

        // ─── Check for existing payment ───────────────────────────────────────────
        paymentRepository.findByBillId(bill.getId())
                .ifPresent(existing -> {
                    if (existing.getStatus() == PaymentStatus.COMPLETED) {
                        throw new InvalidOperationException("Deposit already paid");
                    }
                    paymentRepository.delete(existing); // delete pending/failed, allow retry
                });

        // ─── Create payment by method ─────────────────────────────────────────────
        Payment payment;

        if (PaymentMethod.CASH.equals(request)) {
            payment = createDepositCashPayment(bill, reservation, depositAmount);
        } else if (PaymentMethod.MOMO.equals(request)) {
            payment = createDepositMoMoPayment(bill, reservation);
        } else {
            throw new InvalidOperationException("Payment method not supported: " + request);
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Deposit payment [ID:{}] created for reservation [ID:{}] — amount: {}, method: {}",
                saved.getId(), reservationId, depositAmount, request);

        return mapToResponse(saved);
    }

    private Payment createDepositCashPayment(Bill bill, Reservation reservation, BigDecimal amount) {
        // Mark deposit as paid on reservation
        reservation.setDepositPaid(true);
        reservationRepository.save(reservation);

        // Update bill
        bill.setStatus(BillStatus.PAID);
        bill.setClosedAt(LocalDateTime.now());
        billRepository.save(bill);

        return Payment.builder()
                .method(PaymentMethod.CASH)
                .amount(amount)
                .status(PaymentStatus.COMPLETED)
                .transactionId("DEPOSIT_CASH_" + reservation.getId() + "_" + System.currentTimeMillis())
                .paidAt(LocalDateTime.now())
                .build();
    }

    // ─── MoMo deposit — returns payment URL ──────────────────────────────────────
    private Payment createDepositMoMoPayment(Bill bill, Reservation reservation) {
        String orderId   = "DEPOSIT_" + reservation.getId() + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        BigDecimal amount = reservation.getDepositAmount();

        MoMoPaymentService.MoMoPaymentResult momoResult = moMoPaymentService.createPayment(
                orderId,
                requestId,
                amount,
                "Deposit for Reservation #" + reservation.getId(),
                ""
        );

        if (!momoResult.isSuccess()) {
            throw new InvalidOperationException("MoMo payment failed: " + momoResult.getMessage());
        }

        return Payment.builder()
                .method(PaymentMethod.MOMO)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .momoOrderId(orderId)
                .momoRequestId(requestId)
                .paymentUrl(momoResult.getPayUrl())
                .build();
    }



    @Transactional
    public PaymentResponse refundDepositOnSeated(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        // ─── Only refund if seated ────────────────────────────────────────────────
        if (reservation.getStatus() != ReservationStatus.SEATED) {
            throw new InvalidOperationException(
                    "Deposit refund only happens when reservation is SEATED, current: "
                            + reservation.getStatus());
        }

        if (!Boolean.TRUE.equals(reservation.getDepositPaid())) {
            throw new InvalidOperationException("No deposit was paid for this reservation");
        }

        Bill bill = reservation.getBill();
        Payment depositPayment = paymentRepository.findByBillId(bill.getId())
                .orElseThrow(() -> new RuntimeException("No payment found for reservation bill"));

        if (depositPayment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidOperationException("Deposit payment is not completed, cannot refund");
        }

        if (depositPayment.getStatus() == PaymentStatus.REFUNDED) {
            throw new InvalidOperationException("Deposit already refunded");
        }

        BigDecimal depositAmount = depositPayment.getAmount();

        // ─── Process refund by method ─────────────────────────────────────────────
        if (PaymentMethod.MOMO.equals(depositPayment.getMethod())) {
            boolean refunded = moMoPaymentService.refundPayment(
                    depositPayment.getMomoTransId(),
                    depositAmount,
                    "Deposit refunded — customer checked in for reservation #" + reservationId
            );

            if (!refunded) {
                throw new InvalidOperationException("MoMo refund failed — please process manually");
            }
        }
        // Cash refund — just mark it, staff handles physical cash

        depositPayment.processRefund(
                depositAmount,
                "Deposit refunded upon check-in — reservation #" + reservationId
        );

        Payment saved = paymentRepository.save(depositPayment);

        log.info("Deposit refunded [paymentId:{}] for reservation [ID:{}] — amount: {}",
                saved.getId(), reservationId, depositAmount);

        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse confirmDepositPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        // ─── Validate payment is for a reservation deposit ────────────────────────
        Bill bill = payment.getBill();
        Reservation reservation = bill.getReservation();

        if (reservation == null) {
            throw new InvalidOperationException("This payment is not linked to a reservation");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidOperationException(
                    "Payment is not completed yet, current status: " + payment.getStatus());
        }

        if (Boolean.TRUE.equals(reservation.getDepositPaid())) {
            throw new InvalidOperationException("Deposit already marked as paid for reservation #"
                    + reservation.getId());
        }

        // ─── Mark deposit as paid ─────────────────────────────────────────────────
        reservation.setDepositPaid(true);

        // ─── Auto-confirm reservation ─────────────────────────────────────────────
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            log.info("Reservation [ID:{}] auto-confirmed after deposit payment [ID:{}]",
                    reservation.getId(), paymentId);
        }

        reservationRepository.save(reservation);

        log.info("Deposit confirmed — reservation [ID:{}] depositPaid=true, status={}",
                reservation.getId(), reservation.getStatus());

        return mapToResponse(payment);
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

            Reservation reservation = bill.getReservation();
            if (reservation != null && !Boolean.TRUE.equals(reservation.getDepositPaid())) {
                reservation.setDepositPaid(true);

                if (reservation.getStatus() == ReservationStatus.PENDING) {
                    reservation.setStatus(ReservationStatus.CONFIRMED);
                    log.info("Reservation [ID:{}] auto-confirmed via MoMo IPN",
                            reservation.getId());
                }
                reservationRepository.save(reservation);
            }

            log.info("MoMo payment completed for order: {}, Bill ID: {}", orderId, bill.getId());
        } else {
            // Payment failed
            payment.markAsFailed("MoMo payment failed: " + message);
            log.error("MoMo payment failed for order: {}, Code: {}, Message: {}", 
                    orderId, resultCode, message);
        }

        paymentRepository.save(payment);
    }


    @Transactional
    public PaymentResponse checkAndConfirmDeposit(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        Bill bill = reservation.getBill();
        if (bill == null) {
            throw new InvalidOperationException("No bill linked to this reservation");
        }

        Payment payment = paymentRepository.findByBillId(bill.getId())
                .orElseThrow(() -> new RuntimeException("No payment found for reservation"));

        // ─── If MoMo and still pending — query MoMo live ─────────────────────────
        if (PaymentMethod.MOMO.equals(payment.getMethod())
                && payment.getStatus() == PaymentStatus.PENDING) {

            MoMoPaymentService.MoMoPaymentStatus momoStatus =
                    moMoPaymentService.checkPaymentStatus(
                            payment.getMomoOrderId(),
                            payment.getMomoRequestId()
                    );

            if (momoStatus.isCompleted()) {
                // Payment confirmed by MoMo — update everything
                payment.markAsPaid();
                payment.setMomoTransId(momoStatus.getTransId());
                payment.setTransactionId(momoStatus.getTransId());
                paymentRepository.save(payment);

                // ✅ Mark deposit paid + confirm reservation
                reservation.setDepositPaid(true);
                if (reservation.getStatus() == ReservationStatus.PENDING) {
                    reservation.setStatus(ReservationStatus.CONFIRMED);
                    reservation.setDepositPaid(true);
                }
                reservationRepository.save(reservation);

                log.info("Deposit confirmed via poll — reservation [ID:{}] now CONFIRMED",
                        reservationId);

            } else if (momoStatus.isFailed()) {
                payment.markAsFailed(momoStatus.getMessage());
                paymentRepository.save(payment);
                log.warn("Deposit payment FAILED for reservation [ID:{}]", reservationId);
            }
            // still pending — no change
        }

        // ─── If cash — just check current state ──────────────────────────────────
        if (PaymentMethod.CASH.equals(payment.getMethod())
                && payment.getStatus() == PaymentStatus.COMPLETED
                && !Boolean.TRUE.equals(reservation.getDepositPaid())) {

            reservation.setDepositPaid(true);
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                reservation.setStatus(ReservationStatus.CONFIRMED);
            }
            reservationRepository.save(reservation);
        }

        return mapToResponse(payment);
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
