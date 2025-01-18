package ru.utlc.financialmanagementservice.exception;

// Exception for mismatched amounts when currencies are the same
public class AmountMismatchException extends AllocationValidationException {
    public AmountMismatchException(String message) {
        super(message);
    }
}
