package ru.utlc.exception;

public class ReferenceTypeNotFoundException extends AbstractNotFoundException {
    public ReferenceTypeNotFoundException(String msgKey, Object... args) {
        super(msgKey, args);
    }
    public ReferenceTypeNotFoundException(Object... args) {
        super(args);
    }
    @Override
    protected String getDefaultMessageKey() {
        return "error.referenceType.notFound";
    }
}