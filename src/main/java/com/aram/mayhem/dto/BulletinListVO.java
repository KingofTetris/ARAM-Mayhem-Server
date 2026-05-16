package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告列表视图对象
 *
 * 数据流向：BulletinService → BulletinController → 前端公告列表页
 * 用途：公告列表和轮播展示
 */
@Data
public class BulletinListVO {

    private Long id;

    /** 公告类型（version/event/notice） */
    private String type;

    /** 公告标题 */
    private String title;

    /** 公告摘要内容 */
    private String content;

    /** 封面图 URL */
    private String imageUrl;

    /** 是否置顶（1=置顶, 0=普通） */
    private Integer isPinned;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
