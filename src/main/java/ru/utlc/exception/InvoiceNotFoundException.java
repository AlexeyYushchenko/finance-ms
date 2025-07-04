package ru.utlc.exception;

public class InvoiceNotFoundException extends AbstractNotFoundException {
    public InvoiceNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    public InvoiceNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.invoice.notFound";
    }
}

