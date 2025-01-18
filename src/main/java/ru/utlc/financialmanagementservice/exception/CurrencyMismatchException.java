package ru.utlc.financialmanagementservice.exception;

// Exception for currency mismatch errors
public class CurrencyMismatchException extends AllocationValidationException {
    public CurrencyMismatchException(String message) {
        super(message);
    }
}
