package com.acms.mail;

/**
 * Published when a user receives a new one-time password (and, if needed, a verification link).
 * The plain password exists ONLY in this object in memory: it is never stored or logged.
 *
 * @param verificationToken null when the email address is already verified
 */
public record CredentialsIssuedEvent(
        String username,
        String email,
        String temporaryPassword,
        String verificationToken,
        boolean firstAdministrator) {

    /** Overridden so the password can never end up in a log by accident. */
    @Override
    public String toString() {
        return "CredentialsIssuedEvent[username=" + username + "]";
    }
}
