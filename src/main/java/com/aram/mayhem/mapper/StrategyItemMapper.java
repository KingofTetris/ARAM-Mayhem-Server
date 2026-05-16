package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.StrategyItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 攻略-装备关联数据访问层，对应表 tb_strategy_item */
@Mapper
public interface StrategyItemMapper extends BaseMapper<StrategyItem> {
}
