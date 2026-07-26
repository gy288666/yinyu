package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/** 权限表 Mapper */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
