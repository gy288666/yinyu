package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.PlaylistSong;
import org.apache.ibatis.annotations.Mapper;

/** 歌单-歌曲关联表 Mapper */
@Mapper
public interface PlaylistSongMapper extends BaseMapper<PlaylistSong> {
}
