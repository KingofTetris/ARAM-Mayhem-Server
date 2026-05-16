package com.aram.mayhem.mapper;

import com.aram.mayhem.entity.Bulletin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 公告数据访问层，对应表 tb_bulletin */
@Mapper
public interface BulletinMapper extends BaseMapper<Bulletin> {
}
