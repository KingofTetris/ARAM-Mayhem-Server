package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.StrategyAugment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略-符文关联数据访问层
 *
 * 对应表：tb_strategy_augment
 * 功能：攻略与符文的多对多关联关系管理
 * 关联实体：StrategyAugment
 * 继承：BaseMapper<StrategyAugment>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface StrategyAugmentMapper extends BaseMapper<StrategyAugment> {
}
