package ru.utlc.financialmanagementservice.exception;

public class ReferenceTypeNotFoundException extends AbstractNotFoundException {
    public ReferenceTypeNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}