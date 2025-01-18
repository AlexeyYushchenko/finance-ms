package ru.utlc.financialmanagementservice.exception;

public class CurrencyNotFoundException extends AbstractNotFoundException {
    public CurrencyNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
}
