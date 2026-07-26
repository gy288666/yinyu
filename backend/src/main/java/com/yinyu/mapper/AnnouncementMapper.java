package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/** 公告表 Mapper */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
