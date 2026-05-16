package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Hero;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 英雄数据访问层，对应表 tb_hero */
@Mapper
public interface HeroMapper extends BaseMapper<Hero> {
}
