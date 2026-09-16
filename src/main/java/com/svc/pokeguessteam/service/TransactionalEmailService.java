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
            case EMAIL_CHANGE -> "PokeTeamGuess — confirme seu novo e-mail";
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
            case EMAIL_CHANGE -> """
                    Olá!

                    Seu código para confirmar o novo e-mail é: %s

                    Ele expira em %d minutos. Se você não solicitou esta alteração, ignore esta mensagem.
                    """.formatted(plainCode, expiryMinutes);
        };

        deliver(toEmail, subject, body.trim(), plainCode);
    }

    private void deliver(String intendedTo, String subject, String body, String plainCode) {
        if (mailProperties.isDevLogOnly()) {
            logDevFallback(intendedTo, subject, plainCode, "modo dev — e-mail não enviado");
            return;
        }

        if (sendTo(intendedTo, subject, body)) {
            return;
        }

        if (mailProperties.hasDevRedirectTo()) {
            String redirectTo = mailProperties.getDevRedirectTo().trim();
            String devSubject = "[DEV] " + subject;
            String devBody = """
                    (E-mail de desenvolvimento — destinatário original: %s)

                    %s
                    """.formatted(intendedTo, body);
            if (sendTo(redirectTo, devSubject, devBody)) {
                log.info("E-mail entregue via redirecionamento de dev para {} (destino original: {})",
                        redirectTo, intendedTo);
                return;
            }
        }

        logDevFallback(intendedTo, subject, plainCode,
                "não foi possível entregar (configure RESEND_API_KEY, SMTP ou APP_MAIL_DEV_REDIRECT_TO)");
    }

    private boolean sendTo(String toEmail, String subject, String body) {
        if (resendEmailSender.isConfigured() && resendEmailSender.sendTextEmail(mailProperties.getFrom(), toEmail, subject, body)) {
            return true;
        }
        return mailSender != null && sendViaSmtp(toEmail, subject, body);
    }

    private boolean sendViaSmtp(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("E-mail enviado via SMTP para {}", toEmail);
            return true;
        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail SMTP para {}", toEmail, ex);
            return false;
        }
    }

    private void logDevFallback(String toEmail, String subject, String plainCode, String reason) {
        log.warn("[DEV] {} — destino={} assunto={} código={}", reason, toEmail, subject, plainCode);
    }
}
