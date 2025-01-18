package ru.utlc.financialmanagementservice.exception;

public class ExchangeRateRetrievalFailedException extends RuntimeException {

    private final String message;
    private final Object[] args;
    public ExchangeRateRetrievalFailedException(String message, Object... args) {
        super(message);
        this.message = message;
        this.args = args;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }
}
