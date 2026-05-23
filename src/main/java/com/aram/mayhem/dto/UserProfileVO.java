package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料响应对象 —— 个人中心页展示用
 *
 * 这个 VO 包含了用户的所有可公开信息，不包含密码等敏感字段。
 * 前端个人中心页用这些数据展示用户的个人资料。
 *
 * 与 AuthResponse 的区别：
 * - AuthResponse：登录/注册时返回，包含 Token
 * - UserProfileVO：个人中心页返回，包含更多详细信息（攻略数、收藏数）
 *
 * 数据流向：
 * UserService 查询用户信息 → 组装为 UserProfileVO → 返回给前端
 *
 * 关联类：
 * - User：数据库实体类
 * - UserService：查询用户信息
 */
@Data
@Builder // 建造者模式
@NoArgsConstructor // Jackson 反序列化需要
@AllArgsConstructor // Builder 需要
public class UserProfileVO {

    /** 用户 ID */
    private Long id;

    /** 登录邮箱 */
    private String email;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像 URL */
    private String avatarUrl;

    /** 显示模式（0=浅色, 1=深色） */
    private Integer displayMode;

    /** 是否启用通知（1=启用, 0=关闭） */
    private Integer notificationEnabled;

    /** 用户角色（USER / ADMIN） */
    private String role;

    /** 发布的攻略数量 —— 统计查询得到，不是 User 表的字段 */
    private Integer strategyCount;

    /** 收藏数量 —— 统计查询得到 */
    private Integer favoriteCount;
}
