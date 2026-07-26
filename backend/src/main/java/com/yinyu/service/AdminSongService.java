package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.dto.AdminSongRequest;
import com.yinyu.entity.Admin;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongAuditRecord;
import com.yinyu.entity.SongSinger;
import com.yinyu.mapper.AdminMapper;
import com.yinyu.mapper.SongAuditRecordMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.SongSingerMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.AdminSongVO;
import com.yinyu.vo.PlayUrlVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 后台音乐管理与审核（api.md 18.2/18.3）
 */
@Service
@RequiredArgsConstructor
public class AdminSongService {

    private static final Set<String> AUDIO_EXT = Set.of("mp3", "flac", "wav");

    private final SongMapper songMapper;
    private final SongSingerMapper songSingerMapper;
    private final SongAuditRecordMapper songAuditRecordMapper;
    private final AdminMapper adminMapper;
    private final SongAssembler songAssembler;
    private final MinioUtil minioUtil;

    /** 音乐分页（api.md 18.2.1），status: PENDING/ONLINE/REJECTED/OFFLINE */
    public PageResult<AdminSongVO> page(long pageNum, long pageSize, String name, Long singerId,
                                        Long albumId, Long categoryId, String status) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null && !name.isEmpty(), Song::getName, name)
                .eq(albumId != null, Song::getAlbumId, albumId)
                .eq(categoryId != null, Song::getCategoryId, categoryId);
        if (singerId != null) {
            List<Long> songIds = songSingerMapper.selectList(
                            new LambdaQueryWrapper<SongSinger>().eq(SongSinger::getSingerId, singerId))
                    .stream().map(SongSinger::getSongId).toList();
            if (songIds.isEmpty()) {
                return PageResult.of(pageNum, pageSize, 0, List.of());
            }
            wrapper.in(Song::getId, songIds);
        }
        applyStatusFilter(wrapper, status);
        wrapper.orderByDesc(Song::getCreateTime);
        Page<Song> page = songMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, this::toAdminVOs);
    }

    private void applyStatusFilter(LambdaQueryWrapper<Song> wrapper, String status) {
        if (status == null || status.isEmpty()) {
            return;
        }
        switch (status) {
            case "PENDING" -> wrapper.eq(Song::getAuditStatus, 0);
            case "REJECTED" -> wrapper.eq(Song::getAuditStatus, 2);
            case "ONLINE" -> wrapper.eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1);
            case "OFFLINE" -> wrapper.eq(Song::getAuditStatus, 1).eq(Song::getStatus, 0);
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 不合法");
        }
    }

    /** 上传音频（api.md 18.2.2）：mp3/flac/wav，存 MinIO music 桶 */
    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_INVALID, "文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "audio.mp3" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!AUDIO_EXT.contains(ext)) {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 mp3/flac/wav 音频文件");
        }
        String objectKey = "song/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        minioUtil.upload("music", objectKey, file);

        // 无音频解析库，码率按常见值估算，时长由维护端确认后在新增接口传入
        long size = file.getSize();
        int assumedBitrate = "mp3".equals(ext) ? 192 : 900;
        int estimatedDuration = (int) (size * 8 / (assumedBitrate * 1000L));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("objectKey", objectKey);
        data.put("duration", estimatedDuration);
        data.put("bitrate", assumedBitrate);
        data.put("fileSize", size);
        data.put("suggestName", original.contains(".")
                ? original.substring(0, original.lastIndexOf('.')) : original);
        return data;
    }

    /** 新增音乐（api.md 18.2.3）：创建后待审核 */
    @Transactional
    public AdminSongVO create(AdminSongRequest req, Long adminId) {
        if (req.getName() == null || req.getName().isBlank()
                || req.getObjectKey() == null || req.getObjectKey().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: name/objectKey 必填");
        }
        Song song = new Song();
        song.setName(req.getName());
        song.setAlbumId(req.getAlbumId());
        song.setCategoryId(req.getCategoryId());
        song.setCover(req.getCover());
        song.setFilePath(req.getObjectKey());
        song.setLyricPath(req.getLyric());
        song.setDuration(req.getDuration() == null ? 0 : req.getDuration());
        song.setFileSize(req.getFileSize() == null ? 0L : req.getFileSize());
        song.setIsOriginal(Boolean.TRUE.equals(req.getOriginal()) ? 1 : 0);
        // 付费类型：单曲价格>0 视为单曲购买；vip=true 会员免费；否则免费
        if (req.getPrice() != null && req.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            song.setPayType(2);
            song.setPrice(req.getPrice());
        } else if (Boolean.TRUE.equals(req.getVip())) {
            song.setPayType(1);
            song.setPrice(BigDecimal.ZERO);
        } else {
            song.setPayType(0);
            song.setPrice(BigDecimal.ZERO);
        }
        song.setPlayCount(0L);
        song.setLikeCount(0L);
        song.setCollectCount(0L);
        song.setDownloadCount(0L);
        song.setCommentCount(0L);
        song.setAuditStatus(0);
        song.setStatus(0);
        song.setUploadAdminId(adminId);
        songMapper.insert(song);
        if (req.getSingerId() != null) {
            SongSinger relation = new SongSinger();
            relation.setSongId(song.getId());
            relation.setSingerId(req.getSingerId());
            relation.setSort(0);
            songSingerMapper.insert(relation);
        }
        return toAdminVOs(List.of(song)).get(0);
    }

    /** 修改音乐（api.md 18.2.4） */
    @Transactional
    public void update(Long id, AdminSongRequest req) {
        Song song = requireSong(id);
        if (req.getName() != null && !req.getName().isBlank()) {
            song.setName(req.getName());
        }
        if (req.getAlbumId() != null) {
            song.setAlbumId(req.getAlbumId());
        }
        if (req.getCategoryId() != null) {
            song.setCategoryId(req.getCategoryId());
        }
        if (req.getCover() != null) {
            song.setCover(req.getCover());
        }
        if (req.getObjectKey() != null && !req.getObjectKey().isBlank()) {
            song.setFilePath(req.getObjectKey());
        }
        if (req.getLyric() != null) {
            song.setLyricPath(req.getLyric());
        }
        if (req.getDuration() != null) {
            song.setDuration(req.getDuration());
        }
        if (req.getOriginal() != null) {
            song.setIsOriginal(req.getOriginal() ? 1 : 0);
        }
        if (req.getPrice() != null && req.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            song.setPayType(2);
            song.setPrice(req.getPrice());
        } else if (req.getVip() != null) {
            song.setPayType(req.getVip() ? 1 : 0);
            song.setPrice(BigDecimal.ZERO);
        }
        songMapper.updateById(song);
        if (req.getSingerId() != null) {
            songSingerMapper.delete(new LambdaQueryWrapper<SongSinger>().eq(SongSinger::getSongId, id));
            SongSinger relation = new SongSinger();
            relation.setSongId(id);
            relation.setSingerId(req.getSingerId());
            relation.setSort(0);
            songSingerMapper.insert(relation);
        }
    }

    /** 上/下架（api.md 18.2.5）：仅审核通过曲目 */
    public void changeStatus(Long id, String status) {
        Song song = requireSong(id);
        if (song.getAuditStatus() == null || song.getAuditStatus() != 1) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "仅审核通过的曲目可上/下架");
        }
        if ("ONLINE".equals(status)) {
            song.setStatus(1);
            if (song.getPublishTime() == null) {
                song.setPublishTime(LocalDateTime.now());
            }
        } else if ("OFFLINE".equals(status)) {
            song.setStatus(0);
        } else {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 须为 ONLINE/OFFLINE");
        }
        songMapper.updateById(song);
    }

    /** 删除音乐（api.md 18.2.6）：逻辑删除 */
    public void delete(Long id) {
        requireSong(id);
        songMapper.deleteById(id);
    }

    /** 审核分页（api.md 18.3.1），status: PENDING/PASSED/REJECTED */
    public PageResult<AdminSongVO> auditPage(long pageNum, long pageSize, String status) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        switch (status == null || status.isEmpty() ? "PENDING" : status) {
            case "PENDING" -> wrapper.eq(Song::getAuditStatus, 0);
            case "PASSED" -> wrapper.eq(Song::getAuditStatus, 1);
            case "REJECTED" -> wrapper.eq(Song::getAuditStatus, 2);
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 不合法");
        }
        wrapper.orderByAsc(Song::getCreateTime);
        Page<Song> page = songMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, this::toAdminVOs);
    }

    /** 审核通过（api.md 18.3.2）：置上架 + 写审核记录，重复审核 20004 */
    @Transactional
    public void pass(Long songId, Long adminId) {
        Song song = requireSong(songId);
        if (song.getAuditStatus() == null || song.getAuditStatus() != 0) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "该曲目已审核，不可重复操作");
        }
        song.setAuditStatus(1);
        song.setStatus(1);
        if (song.getPublishTime() == null) {
            song.setPublishTime(LocalDateTime.now());
        }
        songMapper.updateById(song);
        insertAuditRecord(songId, 1, "内容合规，审核通过", adminId);
    }

    /** 审核驳回（api.md 18.3.3）：reason ≥5 字 */
    @Transactional
    public void reject(Long songId, String reason, Long adminId) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: reason 至少5个字");
        }
        Song song = requireSong(songId);
        if (song.getAuditStatus() == null || song.getAuditStatus() != 0) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "该曲目已审核，不可重复操作");
        }
        song.setAuditStatus(2);
        song.setStatus(0);
        songMapper.updateById(song);
        insertAuditRecord(songId, 2, reason.trim(), adminId);
    }

    /** 审核试听地址（api.md 18.3.4）：不要求上架、不埋点 */
    public PlayUrlVO auditUrl(Long songId) {
        Song song = requireSong(songId);
        String url = minioUtil.presignedGetUrl("music", song.getFilePath());
        return new PlayUrlVO(songId, url, minioUtil.getPresignExpireSeconds(), 0);
    }

    private void insertAuditRecord(Long songId, int auditStatus, String reason, Long adminId) {
        SongAuditRecord record = new SongAuditRecord();
        record.setSongId(songId);
        record.setAuditStatus(auditStatus);
        record.setReason(reason);
        record.setAdminId(adminId);
        songAuditRecordMapper.insert(record);
    }

    private Song requireSong(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return song;
    }

    /** Song -> AdminSongVO（补齐审核状态/驳回原因/上传人等） */
    private List<AdminSongVO> toAdminVOs(List<Song> songs) {
        List<SongVO> base = songAssembler.toVOs(songs);
        Map<Long, SongVO> baseMap = base.stream()
                .collect(Collectors.toMap(SongVO::getId, v -> v));
        // 驳回原因：取最近一条驳回记录
        List<Long> rejectedIds = songs.stream()
                .filter(s -> s.getAuditStatus() != null && s.getAuditStatus() == 2)
                .map(Song::getId).toList();
        Map<Long, String> rejectReasons = rejectedIds.isEmpty() ? Map.of()
                : songAuditRecordMapper.selectList(new LambdaQueryWrapper<SongAuditRecord>()
                        .in(SongAuditRecord::getSongId, rejectedIds)
                        .eq(SongAuditRecord::getAuditStatus, 2)
                        .orderByDesc(SongAuditRecord::getCreateTime))
                .stream().collect(Collectors.toMap(SongAuditRecord::getSongId,
                        SongAuditRecord::getReason, (a, b) -> a));
        List<Long> adminIds = songs.stream().map(Song::getUploadAdminId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> adminNames = adminIds.isEmpty() ? Map.of()
                : adminMapper.selectBatchIds(adminIds).stream()
                        .collect(Collectors.toMap(Admin::getId, Admin::getNickname));

        return songs.stream().map(song -> {
            AdminSongVO vo = new AdminSongVO();
            BeanUtils.copyProperties(baseMap.get(song.getId()), vo);
            int audit = song.getAuditStatus() == null ? 0 : song.getAuditStatus();
            vo.setStatus(audit == 0 ? "PENDING" : audit == 2 ? "REJECTED"
                    : (song.getStatus() != null && song.getStatus() == 1 ? "ONLINE" : "OFFLINE"));
            vo.setRejectReason(rejectReasons.get(song.getId()));
            vo.setObjectKey(song.getFilePath());
            vo.setFileSize(song.getFileSize());
            if (song.getFileSize() != null && song.getFileSize() > 0
                    && song.getDuration() != null && song.getDuration() > 0) {
                vo.setBitrate((int) (song.getFileSize() * 8 / song.getDuration() / 1000));
            }
            vo.setCreateBy(adminNames.get(song.getUploadAdminId()));
            vo.setOriginal(song.getIsOriginal() != null && song.getIsOriginal() == 1);
            return vo;
        }).toList();
    }
}
