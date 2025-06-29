package ru.utlc.financialmanagementservice.exception;

public abstract class AbstractNotFoundException extends RuntimeException {

    private final String message;
    private final Object[] args;

    public AbstractNotFoundException(String msgKey, Object... args) {
        super(msgKey);
        this.message = msgKey;
        this.args = args;
    }

    // This constructor allows subclasses to provide a default message key
    protected AbstractNotFoundException(Object... args) {
        super(); // super() is needed, but it will be overridden by subclasses
        this.message = getDefaultMessageKey(); // Fetch default key from subclass
        this.args = args;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }

    // Subclasses should override this method to provide their own message key
    protected abstract String getDefaultMessageKey();
}
