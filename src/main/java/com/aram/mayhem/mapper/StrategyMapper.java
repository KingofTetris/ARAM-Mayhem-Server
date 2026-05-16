package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Strategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻略数据访问层
 *
 * 对应表：tb_strategy
 * 功能：攻略信息的增删改查操作
 * 关联实体：Strategy
 * 继承：BaseMapper<Strategy>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface StrategyMapper extends BaseMapper<Strategy> {
}
