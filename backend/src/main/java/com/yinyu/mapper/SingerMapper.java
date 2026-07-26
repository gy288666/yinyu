package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Singer;
import org.apache.ibatis.annotations.Mapper;

/** 歌手表 Mapper */
@Mapper
public interface SingerMapper extends BaseMapper<Singer> {
}
