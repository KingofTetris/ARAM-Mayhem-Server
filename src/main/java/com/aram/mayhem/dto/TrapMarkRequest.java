package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 版本陷阱标记请求对象 —— 管理员标记/取消英雄或符文的版本陷阱状态
 *
 * 版本陷阱（Version Trap）的含义：
 * 某个英雄或符文在当前版本被大幅削弱，但玩家可能还根据旧印象选择它。
 * 管理员可以标记这些英雄/符文为"版本陷阱"，前端会用红色警告提醒用户。
 *
 * 使用场景：
 * - 标记：isVersionTrap=true，同时记录标记时间
 * - 取消：isVersionTrap=false，同时清除标记时间
 *
 * 数据流向：
 * 前端管理页面 → PUT /api/admin/heroes/{id}/trap 或 /api/admin/augments/{id}/trap
 * → AdminController → HeroService/AugmentService
 *
 * 关联类：
 * - AdminController：管理员 API 接口
 * - HeroService：更新英雄的版本陷阱标记
 * - AugmentService：更新符文的版本陷阱标记
 */
@Data
public class TrapMarkRequest {

    /**
     * 是否标记为版本陷阱
     *
     * @NotNull(message = "isVersionTrap 不能为空")：必须明确指定 true 或 false
     * - true：标记为版本陷阱，前端显示红色警告
     * - false：取消版本陷阱标记，恢复正常显示
     */
    @NotNull(message = "isVersionTrap 不能为空")
    private Boolean isVersionTrap;
}
