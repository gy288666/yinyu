package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.DownloadRecord;
import org.apache.ibatis.annotations.Mapper;

/** 下载记录表 Mapper */
@Mapper
public interface DownloadRecordMapper extends BaseMapper<DownloadRecord> {
}
