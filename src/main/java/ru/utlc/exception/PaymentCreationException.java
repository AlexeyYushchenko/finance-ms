package ru.utlc.exception;

public class PaymentCreationException extends RuntimeException {

    public PaymentCreationException(String message) {
        super(message);
    }
}
