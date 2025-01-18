package ru.utlc.financialmanagementservice.exception;

public class AllocationNotFoundException extends AbstractNotFoundException {
    public AllocationNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}

