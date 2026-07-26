package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Album;
import org.apache.ibatis.annotations.Mapper;

/** 专辑表 Mapper */
@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
}
