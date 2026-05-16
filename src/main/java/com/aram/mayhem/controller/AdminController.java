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

    @Operation(summary = "标记/取消英雄版本陷阱", description = "管理员标记英雄为版本陷阱，标记后前端显示红色警告横幅")
    @PutMapping("/heroes/{id}/trap-mark")
    public Result<Void> markHeroTrap(
            @PathVariable Long id,
            @RequestBody TrapMarkRequest request) {
        log.info("Admin marking hero trap: heroId={}, isVersionTrap={}", id, request.getIsVersionTrap());

        Hero hero = heroMapper.selectById(id);
        if (hero == null) {
            return Result.error(404, "Hero not found with id: " + id);
        }

        hero.setIsVersionTrap(request.getIsVersionTrap());
        hero.setVersionTrapSince(request.getIsVersionTrap() ? LocalDateTime.now() : null);
        heroMapper.updateById(hero);

        log.info("Hero trap mark updated: heroId={}, isVersionTrap={}", id, request.getIsVersionTrap());
        return Result.success();
    }

    @Operation(summary = "标记/取消强化符文版本陷阱", description = "管理员标记强化符文为版本陷阱")
    @PutMapping("/augments/{id}/trap-mark")
    public Result<Void> markAugmentTrap(
            @PathVariable Long id,
            @RequestBody TrapMarkRequest request) {
        log.info("Admin marking augment trap: augmentId={}, isVersionTrap={}", id, request.getIsVersionTrap());

        Augment augment = augmentMapper.selectById(id);
        if (augment == null) {
            return Result.error(404, "Augment not found with id: " + id);
        }

        augment.setIsVersionTrap(request.getIsVersionTrap());
        augment.setVersionTrapSince(request.getIsVersionTrap() ? LocalDateTime.now() : null);
        augmentMapper.updateById(augment);

        log.info("Augment trap mark updated: augmentId={}, isVersionTrap={}", id, request.getIsVersionTrap());
        return Result.success();
    }
}
