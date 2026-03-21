package com.example.demo.demos.common.error;

/**
 * 业务异常基类 — 所有业务模块的异常均继承此类。
 * 包含统一错误码，便于全局异常处理器统一处理。
 */
public class BizException extends RuntimeException {

    /** 错误码（对应 ErrorCode 中的常量） */
    private final int code;

    public BizException(int code) {
        super(ErrorCode.getMessage(code));
        this.code = code;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BizException(int code, Throwable cause) {
        super(ErrorCode.getMessage(code), cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
