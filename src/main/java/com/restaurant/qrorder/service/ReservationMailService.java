package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.response.ReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Send mail when reservation is confirmed
     */
    public void sendReservationConfirmedMail(ReservationResponse reservation) {
        String subject = "Reservation Confirmed - Table Booking";

        String content = buildReservationTemplate(
                "Reservation Confirmed",
                "Your table reservation has been successfully confirmed.",
                reservation
        );

        sendMail(reservation.getCustomerEmail(), subject, content);
    }

    /**
     * Send mail when reservation cancelled
     */
    public void sendReservationCancelledMail(ReservationResponse reservation) {
        String subject = "Reservation Cancelled";

        String content = buildReservationTemplate(
                "Reservation Cancelled",
                "Your reservation has been cancelled.",
                reservation
        );

        sendMail(reservation.getCustomerEmail(), subject, content);
    }

    /**
     * Send mail when reservation no-show
     */
    public void sendReservationNoShowMail(ReservationResponse reservation) {
        String subject = "Reservation Marked As No Show";

        String content = buildReservationTemplate(
                "Reservation No Show",
                "You did not arrive within the allowed time window.",
                reservation
        );

        sendMail(reservation.getCustomerEmail(), subject, content);
    }

    /**
     * Core send mail function.
     * Skips silently when recipient is blank (customerEmail is optional).
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

        } catch (MessagingException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    /**
     * HTML Template
     */
    private String buildReservationTemplate(
            String title,
            String message,
            ReservationResponse r
    ) {

        String tables = String.join(", ", r.getTableNumbers());

        String time = r.getReservationTime().format(TIME_FORMAT);

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
                Restaurant Reservation System
            </p>
        </div>

        <!-- BODY -->
        <div style="padding:30px">

            <h2 style="color:#991b1b;margin-top:0">%s</h2>

            <p style="color:#374151;font-size:14px">
                %s
            </p>

            <hr style="margin:20px 0">

            <table style="width:100%%;font-size:14px">

                <tr>
                    <td style="padding:8px 0;color:#6b7280">Customer</td>
                    <td style="font-weight:bold">%s</td>
                </tr>

                <tr>
                    <td style="padding:8px 0;color:#6b7280">Phone</td>
                    <td>%s</td>
                </tr>

                <tr>
                    <td style="padding:8px 0;color:#6b7280">Reservation Time</td>
                    <td>%s</td>
                </tr>

                <tr>
                    <td style="padding:8px 0;color:#6b7280">Party Size</td>
                    <td>%d people</td>
                </tr>

                <tr>
                    <td style="padding:8px 0;color:#6b7280">Table</td>
                    <td>%s</td>
                </tr>

                <tr>
                    <td style="padding:8px 0;color:#6b7280">Status</td>
                    <td style="
                        color:#16a34a;
                        font-weight:bold
                    ">
                        %s
                    </td>
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
                Please arrive on time. If you need to cancel your reservation,
                contact the restaurant in advance.
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
                title,
                message,
                r.getCustomerName(),
                r.getCustomerPhone(),
                time,
                r.getPartySize(),
                tables,
                r.getStatus()
        );
    }
}
