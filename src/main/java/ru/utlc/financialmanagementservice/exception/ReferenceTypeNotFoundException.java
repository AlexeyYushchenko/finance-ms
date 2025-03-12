package ru.utlc.financialmanagementservice.exception;

public class ServiceTypeNotFoundException extends AbstractNotFoundException {
    public ServiceTypeNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}