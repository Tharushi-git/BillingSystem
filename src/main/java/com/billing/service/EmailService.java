package com.billing.service;

import com.billing.model.Invoice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String shopName;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String from,
                        @Value("${billing.shop-name:My Shop}") String shopName) {
        this.mailSender = mailSender;
        this.from = from;
        this.shopName = shopName;
    }

    public void sendInvoice(Invoice inv, byte[] pdf) throws MessagingException {
        if (from == null || from.isBlank()) {
            throw new IllegalStateException(
                    "Email is not set up. Set the MAIL_USER and MAIL_PASSWORD environment variables.");
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(inv.getCustomer().getEmail());
        helper.setSubject("Your bill #" + inv.getId() + " from " + shopName);
        helper.setText("Hello " + inv.getCustomer().getName() + ",\n\n"
                + "Thank you for your purchase. Your bill is attached as a PDF.\n\n"
                + shopName);
        helper.addAttachment("invoice-" + inv.getId() + ".pdf", new ByteArrayResource(pdf), "application/pdf");
        mailSender.send(message);
    }
}
