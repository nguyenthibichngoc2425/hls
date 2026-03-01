package com.rin.hlsserver.exception;

public class AppException extends BaseException {
    
    public AppException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public AppException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
