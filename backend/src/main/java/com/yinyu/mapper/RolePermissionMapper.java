package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/** 角色-权限关联表 Mapper */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
