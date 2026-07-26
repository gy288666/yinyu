package com.yinyu.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.common.result.Result;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongCopyright;
import com.yinyu.mapper.SongCopyrightMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.security.RequireAdmin;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台版权管理（api.md 18.4.20-18.4.23，二期新增 song_copyright 表）
 */
@RestController
@RequestMapping("/api/admin/copyrights")
@RequiredArgsConstructor
public class AdminCopyrightController {

    private static final List<String> LICENSE_TYPES = List.of("BUYOUT", "LICENSE", "ORIGINAL");

    private final SongCopyrightMapper songCopyrightMapper;
    private final SongMapper songMapper;

    /** 版权分页：songId/owner/到期日（expireBefore）筛选，expiringSoon=30 天内到期 */
    @GetMapping
    @RequireAdmin(permission = "copyright:list")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize,
                                                        @RequestParam(required = false) Long songId,
                                                        @RequestParam(required = false) String owner,
                                                        @RequestParam(required = false) String expireBefore) {
        LambdaQueryWrapper<SongCopyright> wrapper = new LambdaQueryWrapper<SongCopyright>()
                .eq(songId != null, SongCopyright::getSongId, songId)
                .like(owner != null && !owner.isEmpty(), SongCopyright::getOwner, owner)
                .orderByAsc(SongCopyright::getEndDate);
        if (expireBefore != null && !expireBefore.isBlank()) {
            wrapper.le(SongCopyright::getEndDate, LocalDate.parse(expireBefore.trim()));
        }
        Page<SongCopyright> page = songCopyrightMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Long> songIds = page.getRecords().stream()
                .map(SongCopyright::getSongId).distinct().toList();
        Map<Long, String> songNames = songIds.isEmpty() ? Map.of()
                : songMapper.selectBatchIds(songIds).stream()
                        .collect(Collectors.toMap(Song::getId, Song::getName));
        return Result.success(PageResult.from(page,
                list -> list.stream().map(c -> toVO(c, songNames.get(c.getSongId()))).toList()));
    }

    @PostMapping
    @RequireAdmin(permission = "copyright:add")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long songId = Params.requireLng(body, "songId");
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "歌曲不存在");
        }
        SongCopyright entity = new SongCopyright();
        entity.setSongId(songId);
        apply(entity, body, true);
        songCopyrightMapper.insert(entity);
        return Result.success(toVO(entity, song.getName()));
    }

    @PutMapping("/{id}")
    @RequireAdmin(permission = "copyright:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SongCopyright entity = songCopyrightMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long songId = Params.lng(body, "songId");
        if (songId != null) {
            if (songMapper.selectById(songId) == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "歌曲不存在");
            }
            entity.setSongId(songId);
        }
        apply(entity, body, false);
        songCopyrightMapper.updateById(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin(permission = "copyright:delete")
    public Result<Void> delete(@PathVariable Long id) {
        if (songCopyrightMapper.deleteById(id) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return Result.success();
    }

    private void apply(SongCopyright entity, Map<String, Object> body, boolean create) {
        String owner = Params.str(body, "owner");
        if (owner != null) {
            entity.setOwner(owner);
        } else if (create) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: owner 不能为空");
        }
        String licenseType = Params.str(body, "licenseType");
        if (licenseType != null) {
            if (!LICENSE_TYPES.contains(licenseType.toUpperCase())) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "参数校验失败: licenseType 须为 BUYOUT/LICENSE/ORIGINAL");
            }
            entity.setLicenseType(licenseType.toUpperCase());
        } else if (create) {
            entity.setLicenseType("LICENSE");
        }
        if (body.containsKey("startDate")) {
            entity.setStartDate(Params.date(body, "startDate"));
        }
        if (body.containsKey("endDate")) {
            entity.setEndDate(Params.date(body, "endDate"));
        }
        if (body.containsKey("fileUrl")) {
            entity.setFileUrl(Params.str(body, "fileUrl"));
        }
        if (body.containsKey("remark")) {
            entity.setRemark(Params.str(body, "remark"));
        }
    }

    private Map<String, Object> toVO(SongCopyright c, String songName) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("songId", c.getSongId());
        vo.put("songName", songName);
        vo.put("owner", c.getOwner());
        vo.put("licenseType", c.getLicenseType());
        vo.put("startDate", c.getStartDate());
        vo.put("endDate", c.getEndDate());
        vo.put("fileUrl", c.getFileUrl());
        vo.put("remark", c.getRemark());
        vo.put("expiringSoon", c.getEndDate() != null
                && !c.getEndDate().isBefore(LocalDate.now())
                && c.getEndDate().isBefore(LocalDate.now().plusDays(30)));
        vo.put("expired", c.getEndDate() != null && c.getEndDate().isBefore(LocalDate.now()));
        return vo;
    }
}
