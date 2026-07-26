package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.AdminRole;
import org.apache.ibatis.annotations.Mapper;

/** 管理员-角色关联表 Mapper */
@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {
}
