package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Hero;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 英雄数据访问层
 *
 * 对应表：tb_hero
 * 功能：英雄信息的增删改查操作
 * 关联实体：Hero
 * 继承：BaseMapper<Hero>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface HeroMapper extends BaseMapper<Hero> {
}
