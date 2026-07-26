package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.UserLikeSong;
import org.apache.ibatis.annotations.Mapper;

/** 用户喜欢歌曲表 Mapper */
@Mapper
public interface UserLikeSongMapper extends BaseMapper<UserLikeSong> {
}
