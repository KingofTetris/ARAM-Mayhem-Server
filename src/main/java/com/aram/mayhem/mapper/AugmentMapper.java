package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Augment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 强化符文数据访问层，对应表 tb_augment */
@Mapper
public interface AugmentMapper extends BaseMapper<Augment> {
}
