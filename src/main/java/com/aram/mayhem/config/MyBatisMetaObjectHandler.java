package com.aram.mayhem.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * 这个类的作用是：在往数据库插入或更新数据时，自动填充"创建时间"和"更新时间"字段。
 * 就像你在纸上写日记，每次写新日记会自动写上日期，修改旧日记也会更新日期。
 *
 * 为什么要自动填充？
 * - 如果每次手动设置时间，容易忘记，而且代码重复
 * - 自动填充确保所有表的时间字段格式一致，不会出现有的有、有的没有的情况
 *
 * 工作原理：
 * 1. 在实体类的字段上加 @TableField(fill = FieldFill.INSERT) 或 @TableField(fill = FieldFill.INSERT_UPDATE)
 * 2. MyBatis-Plus 在执行 INSERT 或 UPDATE 时，会自动调用这个处理器
 * 3. 处理器根据填充类型（插入/更新），设置对应的时间值
 *
 * 关联实体：
 * - 所有包含 createdAt 和 updatedAt 字段的实体类（Hero、Augment、Strategy、Bulletin 等）
 *
 * @see com.baomidou.mybatisplus.annotation.FieldFill
 */
@Component // 将这个类注册为 Spring 组件，MyBatis-Plus 会自动发现并使用它
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充 —— 当执行 INSERT 操作时自动调用
     *
     * 同时填充 createdAt（创建时间）和 updatedAt（更新时间），
     * 因为新创建的记录，创建时间和更新时间应该是同一个时刻。
     *
     * @param metaObject MyBatis 提供的元对象，可以理解为"正在被插入的那条数据的描述"
     *                   通过它可以读取和设置实体对象的字段值
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // strictInsertFill：严格模式填充，只有当字段为 null 时才填充
        // 第一个参数：元对象
        // 第二个参数：字段名（必须和实体类中的字段名一致）
        // 第三个参数：字段类型
        // 第四个参数：要填充的值（当前时间）
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充 —— 当执行 UPDATE 操作时自动调用
     *
     * 只填充 updatedAt（更新时间），不修改 createdAt（创建时间不应该变）。
     * 就像你修改了一篇文章，文章的"发布时间"不会变，但"最后修改时间"会更新。
     *
     * @param metaObject MyBatis 提供的元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // strictUpdateFill：严格模式更新填充，只有当字段为 null 时才填充
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
