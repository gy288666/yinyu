package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/** 评论点赞表 Mapper */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
}
