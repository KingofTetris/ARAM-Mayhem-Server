package com.aram.mayhem.common;

/**
 * 业务异常类
 *
 * 这个类用于表示"业务逻辑上的错误"，而不是"程序代码上的 Bug"。
 *
 * 举个例子：
 * - 用户注册时邮箱已被占用 → 这是业务异常（用户做了不允许的操作）
 * - 空指针异常 → 这是代码 Bug（程序员写错了）
 *
 * 业务异常的特点：
 * - 继承 RuntimeException（非受检异常），不需要在方法签名上声明 throws
 * - 携带错误码（code）和错误信息（message）
 * - 会被 GlobalExceptionHandler 统一捕获，转换为前端能理解的 Result 格式
 *
 * 使用方式：
 * 1. 指定错误码：throw new BusinessException(404, "英雄不存在")
 * 2. 不指定错误码（默认 400）：throw new BusinessException("邮箱已被注册")
 *
 * 错误码约定：
 * - 400：通用的客户端错误（参数错误、状态不对等）
 * - 404：资源不存在（英雄/策略/用户找不到）
 * - 409：冲突（邮箱已注册、重复投票等）
 *
 * 关联类：
 * - GlobalExceptionHandler：统一捕获 BusinessException 并转为 API 响应
 *
 * @see GlobalExceptionHandler
 */
public class BusinessException extends RuntimeException {

    // 业务错误码，用于前端判断具体的错误类型
    private final int code;

    /**
     * 创建带错误码的业务异常
     *
     * 使用场景：需要返回特定错误码时，如 404（资源不存在）、409（冲突）
     *
     * @param code 错误码（如 404、409）
     * @param message 错误信息，会返回给前端展示
     */
    public BusinessException(int code, String message) {
        // 调用父类 RuntimeException 的构造函数，设置异常信息
        super(message);
        this.code = code;
    }

    /**
     * 创建默认错误码（400）的业务异常
     *
     * 使用场景：一般的客户端错误，不需要特定的错误码
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        // 默认错误码 400，表示"客户端请求有误"
        this.code = 400;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}
