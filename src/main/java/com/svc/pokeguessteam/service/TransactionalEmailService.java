package com.svc.pokeguessteam.service;

import com.svc.pokeguessteam.config.AppMailProperties;
import com.svc.pokeguessteam.model.auth.AuthCodePurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmailService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalEmailService.class);

    private final AppMailProperties mailProperties;
    private final ResendEmailSender resendEmailSender;
    private final JavaMailSender mailSender;

    public TransactionalEmailService(
            AppMailProperties mailProperties,
            ResendEmailSender resendEmailSender,
            @Autowired(required = false) JavaMailSender mailSender
    ) {
        this.mailProperties = mailProperties;
        this.resendEmailSender = resendEmailSender;
        this.mailSender = mailSender;
    }

    public void sendAuthCode(String toEmail, AuthCodePurpose purpose, String plainCode, int expiryMinutes) {
        String subject = switch (purpose) {
            case EMAIL_VERIFICATION -> "PokeTeamGuess — confirme seu e-mail";
            case PASSWORD_RESET -> "PokeTeamGuess — redefinição de senha";
        };
        String body = switch (purpose) {
            case EMAIL_VERIFICATION -> """
                    Olá!

                    Seu código de verificação de e-mail é: %s

                    Ele expira em %d minutos. Se você não criou uma conta, ignore esta mensagem.
                    """.formatted(plainCode, expiryMinutes);
            case PASSWORD_RESET -> """
                    Olá!

                    Seu código para redefinir a senha é: %s

                    Ele expira em %d minutos. Se você não solicitou a redefinição, ignore esta mensagem.
                    """.formatted(plainCode, expiryMinutes);
        };

        if (resendEmailSender.isConfigured()) {
            resendEmailSender.sendTextEmail(mailProperties.getFrom(), toEmail, subject, body.trim());
            return;
        }

        if (mailSender != null) {
            sendViaSmtp(toEmail, subject, body.trim());
            return;
        }

        log.warn("[DEV] E-mail não configurado (defina RESEND_API_KEY no .env). Destino={} assunto={} código={}",
                toEmail, subject, plainCode);
    }

    private void sendViaSmtp(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail SMTP para {}", toEmail, ex);
            throw ex;
        }
    }
}
