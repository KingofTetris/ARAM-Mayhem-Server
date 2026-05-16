package com.aram.mayhem.common;

/**
 * 业务异常
 *
 * 用途：Service 层抛出业务逻辑错误（如用户不存在、邮箱已注册）
 * 默认错误码：400（未指定时）
 * 处理：由 GlobalExceptionHandler 统一捕获并转为 Result 响应
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public int getCode() {
        return code;
    }
}
