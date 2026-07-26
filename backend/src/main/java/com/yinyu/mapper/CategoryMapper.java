package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/** 音乐分类表 Mapper */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
