package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Vote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 投票记录数据访问层，对应表 tb_vote */
@Mapper
public interface VoteMapper extends BaseMapper<Vote> {
}
