package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.UserCollectPlaylist;
import org.apache.ibatis.annotations.Mapper;

/** 用户收藏歌单表 Mapper */
@Mapper
public interface UserCollectPlaylistMapper extends BaseMapper<UserCollectPlaylist> {
}
