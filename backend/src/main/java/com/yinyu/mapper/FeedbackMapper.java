package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

/** 用户反馈表 Mapper */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
