package com.aram.mayhem.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应包装类
 *
 * 格式：{ code, message, data, timestamp }
 * 用途：所有 Controller 返回值统一包装，前端根据 code 判断成功/失败
 * 成功：code=200
 * 失败：code=业务错误码（401/404/409等）
 *
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
