package com.cybermanager.application.services.shared;

public class BusinessException extends RuntimeException {
    private final BusinessErrorType type;
    private final String code;

    public BusinessException(BusinessErrorType type, String code, String message) {
        super(message);
        this.type = type;
        this.code = code;
    }

    public BusinessErrorType type() {
        return type;
    }

    public String code() {
        return code;
    }
}
