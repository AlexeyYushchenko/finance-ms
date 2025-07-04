package ru.utlc.exception;

public class PaymentUpdateException extends RuntimeException {

    public PaymentUpdateException(String message) {
        super(message);
    }

    public PaymentUpdateException() {
        super("error.payment.update");
    }
}
