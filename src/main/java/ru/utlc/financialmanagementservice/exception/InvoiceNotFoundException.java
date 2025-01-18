package ru.utlc.financialmanagementservice.exception;

public class InvoiceNotFoundException extends AbstractNotFoundException {
    public InvoiceNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}

