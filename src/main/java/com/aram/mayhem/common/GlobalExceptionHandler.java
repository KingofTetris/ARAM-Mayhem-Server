package com.aram.mayhem.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 这个类是整个后端的"急救中心"。
 * 不管哪个 Controller 或 Service 抛出了异常，最终都会被这个类接住，
 * 转换成前端能理解的统一格式（Result<Void>），而不是返回一堆乱七八糟的错误页面。
 *
 * 就像医院的急诊科，不管病人是什么病，都先统一挂号、分类处理：
 * - 参数校验失败 → 400（你填的信息有问题）
 * - 业务逻辑错误 → 400（你做了不允许的操作）
 * - 权限不足 → 403（你没有权限做这件事）
 * - 未知异常 → 500（服务器出了问题，不是你的错）
 *
 * @RestControllerAdvice 注解的作用：
 * - @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * - @ControllerAdvice：告诉 Spring 这是一个全局的 Controller 增强类
 * - @ResponseBody：返回值自动序列化为 JSON
 *
 * 异常处理优先级：
 * Spring 会匹配最具体的异常类型。比如 BusinessException 比 Exception 更具体，
 * 所以如果抛出的是 BusinessException，会优先被 handleBusiness 方法处理。
 *
 * 关联类：
 * - Result：统一的 API 响应格式
 * - BusinessException：自定义的业务异常
 */
@RestControllerAdvice // 声明这是一个全局异常处理器，自动拦截所有 Controller 抛出的异常
public class GlobalExceptionHandler {

    // 日志记录器，用于记录异常信息到日志文件
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常 —— @Valid 注解校验失败时抛出
     *
     * 当 Controller 方法的参数上标注了 @Valid 注解，Spring 会自动校验参数。
     * 如果校验失败（如 @NotBlank 字段为空），会抛出 MethodArgumentNotValidException。
     *
     * 我们在这里提取所有校验错误信息，拼接成一个字符串返回给前端，
     * 让前端知道具体哪些字段有问题。
     *
     * 示例返回：{ "code": 400, "message": "email: 不能为空; password: 长度至少6位" }
     *
     * @param ex 参数校验异常
     * @return 包含详细校验错误信息的 Result
     */
    @ExceptionHandler(MethodArgumentNotValidException.class) // 指定要处理的异常类型
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置 HTTP 响应状态码为 400
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        // 从异常中提取所有字段错误，拼接成 "字段名: 错误信息" 的格式
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()) // 格式：email: 不能为空
                .reduce((a, b) -> a + "; " + b) // 用分号连接多个错误
                .orElse("parameter validation failed"); // 如果没有具体错误信息，使用默认提示
        // 记录警告日志（参数校验失败不是 Bug，是用户输入问题）
        log.warn("Validation error: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理约束违反异常 —— @Validated 注解在路径变量/请求参数上校验失败时抛出
     *
     * 与 MethodArgumentNotValidException 的区别：
     * - MethodArgumentNotValidException：校验 @RequestBody（请求体）中的参数
     * - ConstraintViolationException：校验 @RequestParam、@PathVariable 等参数
     *
     * @param ex 约束违反异常
     * @return 包含错误信息的 Result
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return Result.error(400, ex.getMessage());
    }

    /**
     * 处理业务异常 —— 我们自己抛出的 BusinessException
     *
     * 这是处理最多的一类异常，涵盖所有业务逻辑错误：
     * - 用户不存在（404）
     * - 邮箱已注册（409）
     * - 策略不存在（404）
     * - 等等...
     *
     * BusinessException 携带了自定义的错误码和信息，我们直接使用即可。
     *
     * @param ex 业务异常
     * @return 包含业务错误码和信息的 Result
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusiness(BusinessException ex) {
        // 记录警告日志，包含错误码和错误信息
        log.warn("Business error: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理权限不足异常 —— Spring Security 抛出的 AccessDeniedException
     *
     * 当用户尝试访问没有权限的资源时（如普通用户访问管理员接口），
     * Spring Security 会抛出此异常。
     *
     * 返回 403 Forbidden，表示"服务器理解你的请求，但拒绝执行"。
     *
     * @param ex 权限不足异常
     * @return 403 错误的 Result
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // HTTP 403 状态码
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        // 不返回具体的权限信息，防止泄露系统内部结构
        return Result.error(403, "access denied");
    }

    /**
     * 处理所有未捕获的异常 —— 兜底处理器
     *
     * 这是最后一道防线，处理所有上面没有匹配到的异常。
     * 通常是代码 Bug 导致的异常（如空指针、数组越界等）。
     *
     * 安全考虑：
     * - 不向前端暴露具体的异常信息（可能包含数据库结构、代码路径等敏感信息）
     * - 只返回 "internal server error" 的通用提示
     * - 在服务端日志中记录完整的异常堆栈，方便开发人员排查
     *
     * @param ex 未捕获的异常
     * @return 500 错误的 Result
     */
    @ExceptionHandler(Exception.class) // 匹配所有异常类型（最宽泛的匹配）
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // HTTP 500 状态码
    public Result<Void> handleUnknown(Exception ex) {
        // 记录 ERROR 级别日志，包含完整的异常堆栈（第二个参数 ex 会让 SLF4J 打印堆栈）
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        // 向前端返回通用的错误提示，不暴露内部细节
        return Result.error(500, "internal server error");
    }
}
