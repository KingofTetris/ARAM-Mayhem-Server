package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Vote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoteMapper extends BaseMapper<Vote> {
}
