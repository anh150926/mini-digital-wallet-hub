package com.walletapp.common.exception;

import com.walletapp.common.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String customMessage, Object details) {
        super(customMessage != null ? customMessage : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public BusinessException(ErrorCode errorCode, Object details) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}
