package org.certis.studyplatform.auth.application.object.query;

public record ValidateCredentialsQuery(String accountNumber,String rawPassword) {
    public static ValidateCredentialsQuery of(String accountNumber, String rawPassword) {
        return new ValidateCredentialsQuery(accountNumber, rawPassword);
    }
}
