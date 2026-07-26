package com.yinyu.controller.admin;

import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.Result;
import com.yinyu.job.RankSnapshotJob;
import com.yinyu.security.RequireAdmin;
import com.yinyu.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 后台统计查询/导出 + 榜单快照手动触发（api.md 18.1.6/18.1.7）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;
    private final RankSnapshotJob rankSnapshotJob;

    @GetMapping("/stats")
    @RequireAdmin(permission = "stats:view")
    public Result<List<Map<String, Object>>> stats(@RequestParam String metric,
                                                   @RequestParam(defaultValue = "day") String granularity,
                                                   @RequestParam String startDate,
                                                   @RequestParam String endDate) {
        return Result.success(statsService.query(metric, granularity, parse(startDate), parse(endDate)));
    }

    /** CSV 导出：支持 Authorization 头或 ?token= 查询参数鉴权（window.open 下载场景） */
    @GetMapping("/stats/export")
    @RequireAdmin(permission = "stats:export")
    public ResponseEntity<byte[]> export(@RequestParam String metric,
                                         @RequestParam(defaultValue = "day") String granularity,
                                         @RequestParam String startDate,
                                         @RequestParam String endDate) {
        String csv = statsService.exportCsv(metric, granularity, parse(startDate), parse(endDate));
        String filename = "stats-" + metric + "-" + startDate + "_" + endDate + ".csv";
        // 加 UTF-8 BOM，Excel 打开中文不乱码
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }

    /** 榜单快照手动触发（自定义补充，api.md 未覆盖） */
    @PostMapping("/ranks/generate")
    @RequireAdmin(permission = "rank:generate")
    public Result<Map<String, Integer>> generateRanks() {
        return Result.success(rankSnapshotJob.generate(LocalDate.now()));
    }

    private LocalDate parse(String v) {
        try {
            return LocalDate.parse(v.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: 日期须为 yyyy-MM-dd");
        }
    }
}
