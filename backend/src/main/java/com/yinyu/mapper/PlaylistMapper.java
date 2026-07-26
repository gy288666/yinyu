package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Playlist;
import org.apache.ibatis.annotations.Mapper;

/** 歌单表 Mapper */
@Mapper
public interface PlaylistMapper extends BaseMapper<Playlist> {
}
