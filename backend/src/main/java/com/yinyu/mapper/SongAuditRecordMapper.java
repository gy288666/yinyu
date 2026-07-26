package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.SongAuditRecord;
import org.apache.ibatis.annotations.Mapper;

/** 歌曲审核记录表 Mapper */
@Mapper
public interface SongAuditRecordMapper extends BaseMapper<SongAuditRecord> {
}
