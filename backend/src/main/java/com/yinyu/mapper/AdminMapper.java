package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/** 管理员表 Mapper */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
