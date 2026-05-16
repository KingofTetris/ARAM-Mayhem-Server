package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Strategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 攻略数据访问层，对应表 tb_strategy */
@Mapper
public interface StrategyMapper extends BaseMapper<Strategy> {
}
