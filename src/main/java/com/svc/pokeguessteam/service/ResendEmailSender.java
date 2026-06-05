package com.svc.pokeguessteam.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.svc.pokeguessteam.config.AppResendProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResendEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final AppResendProperties resendProperties;
    private final Resend resendClient;

    public ResendEmailSender(AppResendProperties resendProperties) {
        this.resendProperties = resendProperties;
        this.resendClient = resendProperties.isConfigured() ? new Resend(resendProperties.getApiKey()) : null;
    }

    public boolean isConfigured() {
        return resendClient != null;
    }

    public String sendTextEmail(String from, String toEmail, String subject, String textBody) {
        if (!isConfigured()) {
            throw new IllegalStateException("Resend API key não configurada.");
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
            log.info("E-mail enviado via Resend para {} (id={})", toEmail, emailId);
            return emailId;
        } catch (ResendException ex) {
            log.error("Falha ao enviar e-mail via Resend para {}", toEmail, ex);
            throw new IllegalStateException("Falha ao enviar e-mail via Resend.", ex);
        }
    }
}
