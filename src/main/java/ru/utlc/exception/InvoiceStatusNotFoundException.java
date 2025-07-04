package ru.utlc.exception;

public class InvoiceStatusNotFoundException extends AbstractNotFoundException {
    public InvoiceStatusNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    public InvoiceStatusNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.invoiceType.notFound";
    }
}