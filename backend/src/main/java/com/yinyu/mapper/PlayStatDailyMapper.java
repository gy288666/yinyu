package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.PlayStatDaily;
import org.apache.ibatis.annotations.Mapper;

/** 歌曲每日统计表 Mapper */
@Mapper
public interface PlayStatDailyMapper extends BaseMapper<PlayStatDaily> {
}
