package ru.utlc.exception;

// Base exception for allocation validation errors
public class AllocationValidationException extends RuntimeException {
    public AllocationValidationException(String message) {
        super(message);
    }
}

