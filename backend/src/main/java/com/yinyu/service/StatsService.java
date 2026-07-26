package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.entity.OrderInfo;
import com.yinyu.entity.PlayStatDaily;
import com.yinyu.entity.User;
import com.yinyu.mapper.OrderInfoMapper;
import com.yinyu.mapper.PlayStatDailyMapper;
import com.yinyu.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台统计查询与导出（api.md 18.1.6/18.1.7）
 * metric：play（播放量）/ register（注册数）/ revenue（收益）
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayStatDailyMapper playStatDailyMapper;
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;

    /** 统计查询：按日/周/月聚合，跨度 ≤1 年 */
    public List<Map<String, Object>> query(String metric, String granularity,
                                           LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: startDate/endDate 不合法");
        }
        if (startDate.plusYears(1).isBefore(endDate)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: 查询跨度不能超过 1 年");
        }
        String fmt = switch (granularity == null ? "day" : granularity) {
            case "day" -> "%Y-%m-%d";
            case "week" -> "%x-W%v";
            case "month" -> "%Y-%m";
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: granularity 须为 day/week/month");
        };
        List<Map<String, Object>> rows = switch (metric == null ? "" : metric) {
            case "play" -> {
                QueryWrapper<PlayStatDaily> w = new QueryWrapper<>();
                w.select("DATE_FORMAT(stat_date, '" + fmt + "') AS period", "SUM(play_count) AS value")
                        .between("stat_date", startDate, endDate)
                        .groupBy("period").orderByAsc("period");
                yield playStatDailyMapper.selectMaps(w);
            }
            case "register" -> {
                QueryWrapper<User> w = new QueryWrapper<>();
                w.select("DATE_FORMAT(create_time, '" + fmt + "') AS period", "COUNT(*) AS value")
                        .eq("deleted", 0)
                        .ge("create_time", startDate.atStartOfDay())
                        .lt("create_time", endDate.plusDays(1).atStartOfDay())
                        .groupBy("period").orderByAsc("period");
                yield userMapper.selectMaps(w);
            }
            case "revenue" -> {
                QueryWrapper<OrderInfo> w = new QueryWrapper<>();
                w.select("DATE_FORMAT(pay_time, '" + fmt + "') AS period", "SUM(amount) AS value")
                        .eq("deleted", 0).eq("status", 1)
                        .ge("pay_time", startDate.atStartOfDay())
                        .lt("pay_time", endDate.plusDays(1).atStartOfDay())
                        .groupBy("period").orderByAsc("period");
                yield orderInfoMapper.selectMaps(w);
            }
            default -> throw new BizException(ErrorCode.PARAM_INVALID,
                    "参数校验失败: metric 须为 play/register/revenue");
        };
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("period", row.get("period"));
            Object value = row.get("value");
            if ("revenue".equals(metric)) {
                item.put("value", value == null ? "0.00"
                        : new BigDecimal(String.valueOf(value)).setScale(2).toPlainString());
            } else {
                item.put("value", value == null ? 0 : ((Number) value).longValue());
            }
            result.add(item);
        }
        return result;
    }

    /** CSV 导出（18.1.7） */
    public String exportCsv(String metric, String granularity, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = query(metric, granularity, startDate, endDate);
        StringBuilder sb = new StringBuilder("period,").append(metric).append("\n");
        for (Map<String, Object> row : rows) {
            sb.append(row.get("period")).append(",").append(row.get("value")).append("\n");
        }
        return sb.toString();
    }
}
