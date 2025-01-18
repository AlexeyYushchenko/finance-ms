package ru.utlc.financialmanagementservice.exception;

public abstract class AbstractNotFoundException extends RuntimeException {

    private final String message;
    private final Object[] args;

    public AbstractNotFoundException(String msgKey, Object... args) {
        super(msgKey);
        this.message = msgKey;
        this.args = args;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }
}
