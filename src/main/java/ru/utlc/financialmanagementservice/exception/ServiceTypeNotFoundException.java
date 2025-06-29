package ru.utlc.financialmanagementservice.exception;

public class ServiceTypeNotFoundException extends AbstractNotFoundException {
    public ServiceTypeNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
    public ServiceTypeNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.serviceType.notFound";
    }
}