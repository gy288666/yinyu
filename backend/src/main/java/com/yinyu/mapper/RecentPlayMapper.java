package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.RecentPlay;
import org.apache.ibatis.annotations.Mapper;

/** 最近播放表 Mapper */
@Mapper
public interface RecentPlayMapper extends BaseMapper<RecentPlay> {
}
