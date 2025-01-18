package ru.utlc.financialmanagementservice.exception;

// Exception for invalid exchange rates
public class InvalidExchangeRateException extends AllocationValidationException {
    public InvalidExchangeRateException(String message) {
        super(message);
    }
}
