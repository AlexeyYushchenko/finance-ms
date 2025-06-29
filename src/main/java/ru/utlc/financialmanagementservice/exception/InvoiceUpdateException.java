package ru.utlc.financialmanagementservice.exception;

public class InvoiceUpdateException extends RuntimeException {

    public InvoiceUpdateException(String message) {
        super(message);
    }

    public InvoiceUpdateException() {
        super("error.invoice.update");
    }
}