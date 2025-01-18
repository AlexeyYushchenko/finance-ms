package ru.utlc.financialmanagementservice.exception;

public class PaymentTypeNotFoundException extends AbstractNotFoundException {
    public PaymentTypeNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}