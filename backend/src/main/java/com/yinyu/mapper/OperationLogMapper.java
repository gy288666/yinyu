package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志表 Mapper */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
