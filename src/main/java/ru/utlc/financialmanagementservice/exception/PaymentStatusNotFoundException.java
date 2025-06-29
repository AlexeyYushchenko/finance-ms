package ru.utlc.financialmanagementservice.exception;

public class PaymentStatusNotFoundException extends AbstractNotFoundException {
    public PaymentStatusNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    public PaymentStatusNotFoundException(Object... args) {
        super(args);
    }

    @Override
    protected String getDefaultMessageKey() {
        return "error.paymentStatus.notFound";
    }
}