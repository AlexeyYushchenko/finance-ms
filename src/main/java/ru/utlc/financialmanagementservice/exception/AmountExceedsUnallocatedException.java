package ru.utlc.financialmanagementservice.exception;

// Specific exception when the allocated amount exceeds unallocated amounts
public class AmountExceedsUnallocatedException extends AllocationValidationException {
    public AmountExceedsUnallocatedException(String message) {
        super(message);
    }
}
