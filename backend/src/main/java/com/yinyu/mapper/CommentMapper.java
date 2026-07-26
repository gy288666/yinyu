package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/** 评论表 Mapper */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
