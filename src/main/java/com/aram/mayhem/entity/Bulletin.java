package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体类
 *
 * 对应表：tb_bulletin
 * 数据流向：MyBatis-Plus ↔ BulletinService ↔ BulletinController → BulletinListVO/BulletinDetailVO
 * 关联：BulletinController, BulletinService, BulletinMapper
 */
@Data
@TableName("tb_bulletin")
public class Bulletin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告类型（version=版本更新, event=活动, notice=通知） */
    private String type;

    /** 公告标题 */
    private String title;

    /** 公告正文内容（支持 HTML） */
    private String content;

    /** 公告封面图 URL */
    private String imageUrl;

    /** 是否置顶（1=置顶, 0=普通） */
    private Integer isPinned;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除标记（1=已删除, 0=正常） */
    @TableLogic
    private Integer deleted;
}
