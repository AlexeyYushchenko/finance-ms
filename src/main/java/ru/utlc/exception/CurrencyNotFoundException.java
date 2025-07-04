package ru.utlc.exception;

public class CurrencyNotFoundException extends AbstractNotFoundException {
    public CurrencyNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    public CurrencyNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.currency.notFound";
    }
}
