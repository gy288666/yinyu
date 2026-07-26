package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.UserSongPurchase;
import org.apache.ibatis.annotations.Mapper;

/** 用户已购单曲表 Mapper */
@Mapper
public interface UserSongPurchaseMapper extends BaseMapper<UserSongPurchase> {
}
