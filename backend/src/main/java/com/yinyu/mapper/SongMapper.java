package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Song;
import org.apache.ibatis.annotations.Mapper;

/** 歌曲表 Mapper */
@Mapper
public interface SongMapper extends BaseMapper<Song> {
}
