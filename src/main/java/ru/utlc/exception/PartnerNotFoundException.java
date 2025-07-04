package ru.utlc.exception;

public class PartnerNotFoundException extends AbstractNotFoundException {
    public PartnerNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
    public PartnerNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.partner.notFound";
    }
}