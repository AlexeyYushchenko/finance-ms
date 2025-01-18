package ru.utlc.financialmanagementservice.exception;

public class ClientNotFoundException extends AbstractNotFoundException {
    public ClientNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}