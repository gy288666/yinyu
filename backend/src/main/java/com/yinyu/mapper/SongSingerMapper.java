package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.SongSinger;
import org.apache.ibatis.annotations.Mapper;

/** 歌曲-歌手关联表（合唱多对多） Mapper */
@Mapper
public interface SongSingerMapper extends BaseMapper<SongSinger> {
}
