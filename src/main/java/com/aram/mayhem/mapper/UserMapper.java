package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 用户数据访问层，对应表 tb_user */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
