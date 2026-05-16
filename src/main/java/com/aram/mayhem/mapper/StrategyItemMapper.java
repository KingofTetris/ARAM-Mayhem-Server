package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.StrategyItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略-装备关联数据访问层
 *
 * 对应表：tb_strategy_item
 * 功能：攻略与推荐装备的关联关系管理
 * 关联实体：StrategyItem
 * 继承：BaseMapper<StrategyItem>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface StrategyItemMapper extends BaseMapper<StrategyItem> {
}
