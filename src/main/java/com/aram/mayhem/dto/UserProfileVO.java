package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料响应对象
 *
 * 数据流向：UserService → UserController → 前端个人中心页
 * 用途：返回当前登录用户的个人资料信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /** 发布的攻略数量 */
    private Integer strategyCount;

    /** 收藏数量 */
    private Integer favoriteCount;
}
