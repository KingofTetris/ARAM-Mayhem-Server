package com.aram.mayhem.controller;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.dto.TrapMarkRequest;
import com.aram.mayhem.entity.Augment;
import com.aram.mayhem.entity.Hero;
import com.aram.mayhem.mapper.AugmentMapper;
import com.aram.mayhem.mapper.HeroMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 管理后台控制器
 *
 * 路径前缀：/api/admin
 * 权限：仅管理员（@PreAuthorize hasRole('ADMIN')）
 * 功能：版本陷阱标记管理（英雄/符文的版本陷阱标记与取消）
 * 关联：HeroMapper, AugmentMapper, TrapMarkRequest
 */
@Tag(name = "Admin", description = "管理后台接口（需管理员权限）")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final HeroMapper heroMapper;
    private final AugmentMapper augmentMapper;

    public AdminController(HeroMapper heroMapper, AugmentMapper augmentMapper) {
        this.heroMapper = heroMapper;
        this.augmentMapper = augmentMapper;
    }

    /**
     * 标记/取消英雄版本陷阱（管理模块）
     *
     * 作用：管理员标记或取消英雄的版本陷阱状态
     * 版本陷阱：当前版本中表现较差的英雄，标记后前端会显示红色警告横幅
     * 权限：仅管理员可操作（@PreAuthorize hasRole('ADMIN')）
     *
     * @param id      英雄ID
     * @param request 陷阱标记请求体（isVersionTrap: true-标记为陷阱，false-取消标记）
     * @return Result<Void> 操作成功返回空数据，失败返回错误信息
     */
    @Operation(summary = "标记/取消英雄版本陷阱", description = "管理员标记英雄为版本陷阱，标记后前端显示红色警告横幅")
    @PutMapping("/heroes/{id}/trap-mark")
    public Result<Void> markHeroTrap(
            @PathVariable Long id,
            @RequestBody TrapMarkRequest request) {
        // 记录管理员操作日志
        log.info("Admin marking hero trap: heroId={}, isVersionTrap={}", id, request.getIsVersionTrap());

        // 查询英雄是否存在
        Hero hero = heroMapper.selectById(id);
        if (hero == null) {
            return Result.error(404, "Hero not found with id: " + id);
        }

        // 更新陷阱标记状态和标记时间
        hero.setIsVersionTrap(request.getIsVersionTrap());
        hero.setVersionTrapSince(request.getIsVersionTrap() ? LocalDateTime.now() : null);
        heroMapper.updateById(hero);

        log.info("Hero trap mark updated: heroId={}, isVersionTrap={}", id, request.getIsVersionTrap());
        return Result.success();
    }

    /**
     * 标记/取消强化符文版本陷阱（管理模块）
     *
     * 作用：管理员标记或取消强化符文的版本陷阱状态
     * 版本陷阱：当前版本中表现较差的符文，标记后前端会显示红色警告横幅
     * 权限：仅管理员可操作（@PreAuthorize hasRole('ADMIN')）
     *
     * @param id      强化符文ID
     * @param request 陷阱标记请求体（isVersionTrap: true-标记为陷阱，false-取消标记）
     * @return Result<Void> 操作成功返回空数据，失败返回错误信息
     */
    @Operation(summary = "标记/取消强化符文版本陷阱", description = "管理员标记强化符文为版本陷阱")
    @PutMapping("/augments/{id}/trap-mark")
    public Result<Void> markAugmentTrap(
            @PathVariable Long id,
            @RequestBody TrapMarkRequest request) {
        // 记录管理员操作日志
        log.info("Admin marking augment trap: augmentId={}, isVersionTrap={}", id, request.getIsVersionTrap());

        // 查询符文是否存在
        Augment augment = augmentMapper.selectById(id);
        if (augment == null) {
            return Result.error(404, "Augment not found with id: " + id);
        }

        // 更新陷阱标记状态和标记时间
        augment.setIsVersionTrap(request.getIsVersionTrap());
        augment.setVersionTrapSince(request.getIsVersionTrap() ? LocalDateTime.now() : null);
        augmentMapper.updateById(augment);

        log.info("Augment trap mark updated: augmentId={}, isVersionTrap={}", id, request.getIsVersionTrap());
        return Result.success();
    }
}
