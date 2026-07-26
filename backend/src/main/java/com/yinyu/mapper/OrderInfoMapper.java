package com.yinyu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yinyu.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

/** 订单表（会员/单曲购买） Mapper */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}
