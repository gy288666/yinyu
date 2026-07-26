package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/** 角色表 Mapper */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
