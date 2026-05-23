package com.aram.mayhem.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料更新请求对象 —— 前端修改个人资料时发送
 *
 * 与 RegisterRequest 不同，这个对象的所有字段都是可选的。
 * 用户可以只修改昵称，不修改其他字段。
 * 后端会只更新非 null 的字段。
 *
 * 数据流向：
 * 前端个人中心页 → PUT /api/users/profile（携带 UpdateProfileRequest 的 JSON）
 * → UserController → UserService
 *
 * 关联类：
 * - UserController：接收更新请求
 * - UserService：处理更新逻辑（只更新非 null 字段）
 * - User：数据库实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /**
     * 用户昵称 —— 1~30 位
     *
     * @Size(min = 1, max = 30)：昵称长度限制
     * 可选字段，为 null 时不修改
     */
    @Size(min = 1, max = 30, message = "昵称长度必须在1-30位之间")
    private String nickname;

    /** 用户头像 URL —— 可选，为 null 时不修改 */
    private String avatarUrl;

    /** 显示模式（0=浅色, 1=深色）—— 可选，为 null 时不修改 */
    private Integer displayMode;

    /** 是否启用通知（1=启用, 0=关闭）—— 可选，为 null 时不修改 */
    private Integer notificationEnabled;
}
