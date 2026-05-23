package com.aram.mayhem.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应包装类
 *
 * 这个类是后端所有 API 返回值的"统一包装盒"。
 * 不管 API 返回的是英雄数据、用户信息还是错误提示，都装进这个盒子里，
 * 前端只需要按照统一的格式来解析就行了。
 *
 * 就像快递公司规定所有包裹都必须贴统一格式的面单：
 * - code：状态码（200 表示成功，其他表示失败）
 * - message：提示信息（成功时是 "success"，失败时是错误原因）
 * - data：实际数据（成功时才有，失败时为 null）
 * - timestamp：时间戳（请求处理完成的时间，方便调试）
 *
 * 响应格式示例：
 * 成功：{ "code": 200, "message": "success", "data": { "id": 1, "name": "盖伦" }, "timestamp": 1700000000000 }
 * 失败：{ "code": 404, "message": "hero not found", "timestamp": 1700000000000 }
 *
 * @JsonInclude(NON_NULL) 注解的作用：
 * 当 data 为 null 时，JSON 输出中不会包含 data 字段，减少数据传输量。
 *
 * 泛型 <T> 说明：
 * T 代表 data 字段的具体类型，比如 Result<HeroDetailVO> 表示 data 是英雄详情，
 * Result<List<HeroListVO>> 表示 data 是英雄列表。这样编译器会帮我们检查类型安全。
 *
 * @param <T> 响应数据的类型（如 HeroDetailVO、StrategyListVO 等）
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // 当字段值为 null 时，不输出到 JSON 中
public class Result<T> {

    // 状态码：200 表示成功，400 表示客户端错误，500 表示服务器错误
    private int code;
    // 提示信息：成功时为 "success"，失败时为具体的错误描述
    private String message;
    // 实际数据：成功时包含业务数据，失败时为 null
    private T data;
    // 时间戳：响应生成的时间（毫秒级），用于调试和日志追踪
    private long timestamp;

    /**
     * 私有无参构造函数 —— 防止外部直接 new Result()，必须通过静态工厂方法创建
     * 这是"工厂模式"的设计，确保所有 Result 对象的创建方式一致
     */
    private Result() {
    }

    /**
     * 私有全参构造函数 —— 内部使用，创建完整的 Result 对象
     *
     * @param code 状态码
     * @param message 提示信息
     * @param data 实际数据
     */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        // 自动设置时间戳为当前时间的毫秒数
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应（带数据）—— 最常用的工厂方法
     *
     * 使用示例：Result.success(heroDetailVO)
     * 返回示例：{ "code": 200, "message": "success", "data": {...}, "timestamp": 1700000000000 }
     *
     * @param data 响应数据
     * @return 包含数据的成功响应
     * @param <T> 数据类型
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 创建成功响应（无数据）—— 用于不需要返回数据的操作（如删除、更新）
     *
     * 使用示例：Result.success()
     * 返回示例：{ "code": 200, "message": "success", "timestamp": 1700000000000 }
     *
     * @return 不包含数据的成功响应
     * @param <T> 数据类型
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 创建错误响应 —— 用于业务逻辑错误
     *
     * 使用示例：Result.error(404, "hero not found")
     * 返回示例：{ "code": 404, "message": "hero not found", "timestamp": 1700000000000 }
     *
     * 常见错误码：
     * - 400：请求参数错误
     * - 401：未认证（未登录）
     * - 403：无权限
     * - 404：资源不存在
     * - 409：冲突（如邮箱已注册）
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 错误响应
     * @param <T> 数据类型（错误响应中 data 为 null）
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // 以下 getter 方法供 Jackson 序列化时使用，将对象转为 JSON 字符串

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
