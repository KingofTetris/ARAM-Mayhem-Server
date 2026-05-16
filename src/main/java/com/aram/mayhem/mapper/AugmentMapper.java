package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Augment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 强化符文数据访问层
 *
 * 对应表：tb_augment
 * 功能：符文信息的增删改查操作
 * 关联实体：Augment
 * 继承：BaseMapper<Augment>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface AugmentMapper extends BaseMapper<Augment> {
}
