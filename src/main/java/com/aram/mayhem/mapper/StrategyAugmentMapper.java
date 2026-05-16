package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.StrategyAugment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 攻略-符文关联数据访问层，对应表 tb_strategy_augment */
@Mapper
public interface StrategyAugmentMapper extends BaseMapper<StrategyAugment> {
}
