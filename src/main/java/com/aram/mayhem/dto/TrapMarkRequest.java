package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 版本陷阱标记请求
 *
 * 数据流向：前端 Admin → AdminController → Hero/Augment Entity
 * 用途：管理员标记/取消英雄或符文的版本陷阱状态
 */
@Data
public class TrapMarkRequest {

    /** 是否标记为版本陷阱（true=标记, false=取消） */
    @NotNull(message = "isVersionTrap 不能为空")
    private Boolean isVersionTrap;
}
