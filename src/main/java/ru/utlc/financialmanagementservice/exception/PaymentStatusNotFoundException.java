package ru.utlc.financialmanagementservice.exception;

public class InvoiceStatusNotFoundException extends AbstractNotFoundException {
    public InvoiceStatusNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    @Override
    protected String getDefaultMessageKey() {
        return "error.invoiceType.notFound";
    }
}