package ru.utlc.financialmanagementservice.exception;

public class PaymentNotFoundException extends AbstractNotFoundException {

    public PaymentNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    public PaymentNotFoundException(Object... args) {
        super(args);
    }

    @Override
    protected String getDefaultMessageKey() {
        return "error.payment.notFound";
    }
}
