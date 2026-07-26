package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户表 Mapper */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
