package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.SongTag;
import org.apache.ibatis.annotations.Mapper;

/** 歌曲-标签关联表 Mapper */
@Mapper
public interface SongTagMapper extends BaseMapper<SongTag> {
}
