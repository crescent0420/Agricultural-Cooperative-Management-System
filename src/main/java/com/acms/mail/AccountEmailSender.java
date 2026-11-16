package com.acms.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEmailSender {

    private final JavaMailSender mailSender;

    @Value("${acms.mail.from}")
    private String from;

    /** Where the web app runs; the verification link opens its /verify-email page, which calls the API. */
    @Value("${acms.app.base-url}")
    private String baseUrl;

    /**
     * AFTER_COMMIT: the email is sent only once the account is really saved. If the database
     * transaction fails, no email goes out, so nobody receives a password for an account that does not exist.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCredentialsIssued(CredentialsIssuedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject("Your ACMS account");
        message.setText(body(event));
        try {
            mailSender.send(message);
            log.info("Account email sent to user '{}'.", event.username());
        } catch (MailException ex) {
            // The account exists; an administrator can send new credentials with
            // POST /api/users/{id}/reset-password. Only the error message is logged, never the email body.
               log.error("Could not send the account email to user '{}': {}", event.username(),
                       ex.getMostSpecificCause().getMessage());        }
    }

    private String body(CredentialsIssuedEvent event) {
        StringBuilder text = new StringBuilder();
        text.append("Hello ").append(event.username()).append(",\n\n");
        text.append("Your account in the Agricultural Cooperative Management System (ACMS) is ready.\n");
        if (event.firstAdministrator()) {
            text.append("You are the first user, so your account has System Administrator rights.\n");
        }
        text.append("\nUsername: ").append(event.username()).append('\n');
        text.append("One-time password: ").append(event.temporaryPassword()).append("\n\n");

        int step = 1;
        if (event.verificationToken() != null) {
            text.append("Step ").append(step++).append(" - Confirm your email address (the link works once, for 24 hours):\n");
            text.append(baseUrl).append("/verify-email?token=").append(event.verificationToken()).append("\n\n");
        }
        text.append("Step ").append(step).append(" - Log in with the one-time password. ")
            .append("You must choose your own password before you can use the system.\n\n");
        text.append("If you did not expect this email, you can ignore it.\n");
        return text.toString();
    }
}
