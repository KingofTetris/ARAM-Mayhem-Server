package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.HeroModifier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 英雄属性修正器数据访问层
 *
 * 对应表：tb_hero_modifier
 * 功能：英雄属性修正数据的增删改查操作
 * 关联实体：HeroModifier
 * 继承：BaseMapper<HeroModifier>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface HeroModifierMapper extends BaseMapper<HeroModifier> {
}
