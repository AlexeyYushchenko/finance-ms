package ru.utlc.financialmanagementservice.exception;

public class PaymentNotFoundException extends AbstractNotFoundException {
    public PaymentNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}

