package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Vote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投票记录数据访问层
 *
 * 对应表：tb_vote
 * 功能：攻略投票记录的增删改查操作
 * 关联实体：Vote
 * 继承：BaseMapper<Vote>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface VoteMapper extends BaseMapper<Vote> {
}
