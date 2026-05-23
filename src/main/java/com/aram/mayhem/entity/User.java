package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类 —— 对应数据库中的 tb_user 表
 *
 * 这个类存储了用户的基本信息，包括登录凭证、个人资料和偏好设置。
 * 用户可以通过邮箱注册和登录，登录后可以发布攻略、投票等。
 *
 * 安全设计：
 * - 密码使用 BCrypt 加密存储，即使数据库泄露也无法还原原始密码
 * - 密码字段不会出现在任何 API 响应中（DTO 层过滤）
 * - 角色字段用于权限控制（普通用户 vs 管理员）
 *
 * 逻辑删除说明：
 * 使用 @TableLogic 注解实现逻辑删除，删除用户时不会真正删除记录，
 * 而是将 deleted 字段设为 1。这样可以保留用户发布的内容，也方便恢复账号。
 *
 * 数据流向：
 * 数据库 tb_user 表 → MyBatis-Plus 映射为 User 对象
 * → AuthService 处理注册/登录逻辑
 * → CustomUserDetailsService 为 Spring Security 提供用户信息
 * → AuthController 返回认证响应
 *
 * 关联类：
 * - AuthController：认证相关的 API 接口（注册/登录/刷新Token）
 * - AuthService/AuthServiceImpl：认证的业务逻辑
 * - CustomUserDetailsService：Spring Security 的用户加载服务
 * - UserMapper：用户的数据库操作接口
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_user") // 对应数据库表 tb_user
public class User {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录邮箱 —— 唯一标识
     * 用户使用邮箱作为登录名，邮箱在系统中必须唯一
     * 注册时会检查邮箱是否已被使用
     */
    private String email;

    /**
     * BCrypt 加密后的密码 —— 绝对不能存储明文密码！
     * BCrypt 是一种安全的密码哈希算法，特点：
     * - 每次加密结果不同（自带盐值），防止彩虹表攻击
     * - 可以验证密码是否正确，但无法从哈希值反推出原始密码
     * - 示例："$2a$10$N9qo8uLOickgx2ZMRZoMye..."（60 个字符）
     */
    private String password;

    /** 用户昵称 —— 显示在攻略作者名、评论等位置 */
    private String nickname;

    /** 用户头像 URL —— 前端用这个地址加载用户头像 */
    private String avatarUrl;

    /**
     * 显示模式 —— 控制应用的浅色/深色主题
     * - 0：浅色模式（默认）
     * - 1：深色模式
     */
    private Integer displayMode;

    /**
     * 是否启用通知 —— 控制是否接收推送通知
     * - 1：启用通知
     * - 0：关闭通知
     */
    private Integer notificationEnabled;

    /**
     * 用户角色 —— 用于权限控制
     * - "ROLE_USER"：普通用户，可以发布攻略、投票
     * - "ROLE_ADMIN"：管理员，可以管理所有数据
     *
     * Spring Security 要求角色名以 "ROLE_" 前缀开头
     */
    private String role;

    /**
     * 创建时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT) 表示只在插入时自动填充
     * 注册时间不应该被修改
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT_UPDATE) 表示在插入和更新时都自动填充
     * 修改个人信息时，这个时间会自动更新
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记 —— 1=已删除, 0=正常
     *
     * @TableLogic 注解告诉 MyBatis-Plus 这是逻辑删除字段：
     * - 执行 delete 操作时，实际执行 UPDATE SET deleted=1
     * - 执行 select 操作时，自动加上 WHERE deleted=0 条件
     */
    @TableLogic
    private Integer deleted;
}
