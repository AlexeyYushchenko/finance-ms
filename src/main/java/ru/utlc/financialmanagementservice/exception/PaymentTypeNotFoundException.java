package ru.utlc.financialmanagementservice.exception;

public class PaymentTypeNotFoundException extends AbstractNotFoundException {
    public PaymentTypeNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
    public PaymentTypeNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.paymentType.notFound";
    }
}