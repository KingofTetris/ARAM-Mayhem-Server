package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 *
 * 对应表：tb_user
 * 功能：用户信息的增删改查操作（含认证相关查询）
 * 关联实体：User
 * 继承：BaseMapper<User>（MyBatis-Plus 基础 CRUD）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
