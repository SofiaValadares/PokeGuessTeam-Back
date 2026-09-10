package com.svc.pokeguessteam.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.svc.pokeguessteam.config.AppResendProperties;
import com.svc.pokeguessteam.logging.AppLogger;
import org.springframework.stereotype.Component;

@Component
public class ResendEmailSender {

    private static final AppLogger log = AppLogger.create(ResendEmailSender.class);

    private final AppResendProperties resendProperties;
    private final Resend resendClient;

    public ResendEmailSender(AppResendProperties resendProperties) {
        this.resendProperties = resendProperties;
        this.resendClient = resendProperties.isConfigured() ? new Resend(resendProperties.getApiKey()) : null;
    }

    public boolean isConfigured() {
        return resendClient != null;
    }

    public boolean sendTextEmail(String from, String toEmail, String subject, String textBody) {
        if (!isConfigured()) {
            return false;
        }

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(toEmail)
                .subject(subject)
                .text(textBody)
                .build();

        try {
            CreateEmailResponse response = resendClient.emails().send(options);
            String emailId = response != null ? response.getId() : null;
            log.info("sendTextEmail", "E-mail enviado via Resend para {} (id={})", toEmail, emailId);
            return true;
        } catch (ResendException ex) {
            log.error("sendTextEmail", "Falha ao enviar e-mail via Resend para {}", ex, toEmail);
            return false;
        }
    }
}
