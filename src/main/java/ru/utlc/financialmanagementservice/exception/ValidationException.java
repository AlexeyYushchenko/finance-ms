package ru.utlc.financialmanagementservice.exception;

public class ValidationException extends RuntimeException {

    private final String message;
    private final Object[] args;

    public ValidationException(String message, Object... args) {
        super(message);
        this.message = message;
        this.args = args;
    }

    public String getMessageKey() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }
}

