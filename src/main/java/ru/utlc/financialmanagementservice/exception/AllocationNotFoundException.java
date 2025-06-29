package ru.utlc.financialmanagementservice.exception;

public class AllocationNotFoundException extends AbstractNotFoundException {
    public AllocationNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }

    public AllocationNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.paymentInvoice.allocationNotFound";
    }
}

