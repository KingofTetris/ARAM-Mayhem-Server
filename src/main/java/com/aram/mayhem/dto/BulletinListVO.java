package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告列表视图对象 —— 公告列表页和轮播图展示用
 *
 * 列表页只需要公告的摘要信息，不需要完整的正文内容。
 * content 字段在这里只存储摘要（前几十个字），完整内容在 BulletinDetailVO 中。
 *
 * 数据流向：
 * BulletinService 查询公告列表 → 映射为 BulletinListVO → 返回给前端
 *
 * 关联类：
 * - Bulletin：数据库实体类
 * - BulletinDetailVO：公告详情视图对象（包含完整正文）
 */
@Data
public class BulletinListVO {

    /** 公告 ID */
    private Long id;

    /** 公告类型（version/event/notice）—— 前端根据类型显示不同图标 */
    private String type;

    /** 公告标题 */
    private String title;

    /** 公告摘要内容 —— 列表页显示的简短内容 */
    private String content;

    /** 封面图 URL —— 列表页/轮播图显示的图片 */
    private String imageUrl;

    /** 是否置顶（1=置顶, 0=普通）—— 置顶公告排在最前面 */
    private Integer isPinned;

    /** 发布时间 —— 显示"发布于 X 天前" */
    private LocalDateTime publishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
