package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Send mail when payment is successfully completed (no refund scenario).
     */
    public void sendPaymentSuccessMail(String recipientEmail, PaymentResponse payment) {
        String subject = "Payment Successful - Thank You!";

        String content = buildPaymentTemplate(
                "Payment Successful",
                "Your payment has been received and confirmed. Thank you for dining with us!",
                payment,
                "#16a34a"  // green for success
        );

        sendMail(recipientEmail, subject, content);
    }

    /**
     * Send mail when payment / bill is cancelled.
     */
    public void sendPaymentCancelledMail(String recipientEmail, PaymentResponse payment) {
        String subject = "Payment Cancelled";

        String content = buildPaymentTemplate(
                "Payment Cancelled",
                "Your payment has been cancelled. If you believe this is a mistake, please contact us.",
                payment,
                "#dc2626"  // red for cancelled
        );

        sendMail(recipientEmail, subject, content);
    }

    /**
     * Core send mail function.
     * Skips silently when recipient is blank.
     * Logs errors instead of throwing — email failure must not roll back the transaction.
     */
    private void sendMail(String to, String subject, String htmlContent) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email '{}': recipient address is empty", subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent email '{}' to {}", subject, to);

        } catch (MessagingException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    /**
     * HTML template for payment emails.
     *
     * @param title      Heading shown in the body
     * @param message    Subtitle / description line
     * @param payment    PaymentResponse DTO
     * @param accentColor Hex color used for the status badge / title
     */
    private String buildPaymentTemplate(
            String title,
            String message,
            PaymentResponse payment,
            String accentColor
    ) {
        String paidAt = payment.getPaidAt() != null
                ? payment.getPaidAt().format(TIME_FORMAT)
                : "—";

        String transactionId = payment.getTransactionId() != null
                ? payment.getTransactionId()
                : (payment.getMomoOrderId() != null ? payment.getMomoOrderId() : "—");

        return """
        <html>
        <body style="font-family: Arial, sans-serif; background:#f3f4f6; padding:30px">
 
        <div style="
            max-width:600px;
            margin:auto;
            background:white;
            border-radius:12px;
            overflow:hidden;
            box-shadow:0 8px 20px rgba(0,0,0,0.08);
        ">
 
        <!-- HEADER -->
        <div style="
            background:#0f172a;
            color:white;
            padding:20px 30px;
        ">
            <h2 style="margin:0">TERMINAL</h2>
            <p style="margin:0;font-size:12px;color:#d1d5db">
                Restaurant Payment System
            </p>
        </div>
 
        <!-- BODY -->
        <div style="padding:30px">
 
            <h2 style="color:%s;margin-top:0">%s</h2>
 
            <p style="color:#374151;font-size:14px">
                %s
            </p>
 
            <hr style="margin:20px 0">
 
            <table style="width:100%%;font-size:14px">
 
                <tr>
                    <td style="padding:8px 0;color:#6b7280">Bill ID</td>
                    <td style="font-weight:bold">#%d</td>
                </tr>
 
                <tr>
                    <td style="padding:8px 0;color:#6b7280">Amount</td>
                    <td style="font-weight:bold">%s VND</td>
                </tr>
 
                <tr>
                    <td style="padding:8px 0;color:#6b7280">Payment Method</td>
                    <td>%s</td>
                </tr>
 
                <tr>
                    <td style="padding:8px 0;color:#6b7280">Transaction ID</td>
                    <td style="font-family:monospace;font-size:13px">%s</td>
                </tr>
 
                <tr>
                    <td style="padding:8px 0;color:#6b7280">Date &amp; Time</td>
                    <td>%s</td>
                </tr>
 
                <tr>
                    <td style="padding:8px 0;color:#6b7280">Status</td>
                    <td style="color:%s;font-weight:bold">%s</td>
                </tr>
 
            </table>
 
            <div style="
                margin-top:30px;
                padding:15px;
                background:#f9fafb;
                border-radius:8px;
                font-size:12px;
                color:#6b7280
            ">
                If you have any questions about this payment, please contact the restaurant directly.
            </div>
 
        </div>
 
        <!-- FOOTER -->
        <div style="
            background:#0f172a;
            color:#9ca3af;
            text-align:center;
            padding:15px;
            font-size:12px
        ">
            © 2026 TERMINAL Restaurant System
        </div>
 
        </div>
 
        </body>
        </html>
        """.formatted(
                accentColor,
                title,
                message,
                payment.getBillId(),
                payment.getAmount().toPlainString(),
                payment.getMethod(),
                transactionId,
                paidAt,
                accentColor,
                payment.getStatus()
        );
    }
}
