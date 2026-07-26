package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/** 标签表 Mapper */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
