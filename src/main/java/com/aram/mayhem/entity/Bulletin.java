package com.aram.mayhem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体类 —— 对应数据库中的 tb_bulletin 表
 *
 * 公告是管理员发布的系统消息，用于通知用户版本更新、活动信息等。
 * 公告可以置顶显示，让重要的公告始终排在最前面。
 *
 * 公告类型：
 * - version：版本更新公告，如 "v2.0 新增海克斯强化符文推荐"
 * - event：活动公告，如 "五一限时双倍经验"
 * - notice：普通通知，如 "服务器维护通知"
 *
 * 逻辑删除说明：
 * 使用 @TableLogic 注解实现逻辑删除，删除公告时不会真正删除记录，
 * 而是将 deleted 字段设为 1。
 *
 * 数据流向：
 * 数据库 tb_bulletin 表 → MyBatis-Plus 映射为 Bulletin 对象
 * → BulletinService 处理业务逻辑
 * → BulletinController 返回给前端（转换为 BulletinListVO/BulletinDetailVO 格式）
 *
 * 关联类：
 * - BulletinController：公告相关的 API 接口
 * - BulletinService/BulletinServiceImpl：公告的业务逻辑
 * - BulletinMapper：公告的数据库操作接口
 * - BulletinDataInitializer：启动时插入初始公告数据
 */
@Data // Lombok 自动生成 getter/setter/toString/equals/hashCode
@TableName("tb_bulletin") // 对应数据库表 tb_bulletin
public class Bulletin {

    /**
     * 主键 ID —— 数据库自增
     *
     * @TableId 标记这是主键字段
     * IdType.AUTO 表示主键由数据库自动递增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公告类型 —— 区分不同类型的公告
     * - "version"：版本更新公告
     * - "event"：活动公告
     * - "notice"：普通通知
     * 前端根据类型显示不同的图标和样式
     */
    private String type;

    /** 公告标题 —— 简短描述公告内容，如 "v2.0 版本更新" */
    private String title;

    /**
     * 公告正文内容 —— 支持 HTML 格式
     * 可以包含富文本（加粗、链接、图片等），前端用 WebView 渲染
     */
    private String content;

    /** 公告封面图 URL —— 列表页显示的缩略图 */
    private String imageUrl;

    /**
     * 是否置顶 —— 控制公告的排序优先级
     * - 1：置顶，始终显示在列表最前面
     * - 0：普通，按发布时间排序
     */
    private Integer isPinned;

    /**
     * 发布时间 —— 管理员指定的公告发布时间
     * 与 createdAt 不同，这个时间可以由管理员手动设置
     * 比如提前创建好公告，但设置未来时间才发布
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间 —— 自动填充
     *
     * @TableField(fill = FieldFill.INSERT) 表示只在插入时自动填充
     * 记录公告创建的时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记 —— 1=已删除, 0=正常
     *
     * @TableLogic 注解告诉 MyBatis-Plus 这是逻辑删除字段：
     * - 执行 delete 操作时，实际执行 UPDATE SET deleted=1
     * - 执行 select 操作时，自动加上 WHERE deleted=0 条件
     */
    @TableLogic
    private Integer deleted;
}
