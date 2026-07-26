package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.DownloadRecord;
import com.yinyu.entity.Song;
import com.yinyu.entity.User;
import com.yinyu.entity.UserSongPurchase;
import com.yinyu.mapper.DownloadRecordMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.mapper.UserSongPurchaseMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 下载：下载地址（权益 + 每日配额）与下载记录（api.md 14.x）
 */
@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final int URL_EXPIRE_SECONDS = 600;

    private final SongMapper songMapper;
    private final UserMapper userMapper;
    private final UserSongPurchaseMapper userSongPurchaseMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final SystemConfigService systemConfigService;
    private final SongAssembler songAssembler;
    private final MinioUtil minioUtil;

    /** 下载地址（api.md 14.1）：权益同播放 + 每日配额（普通 10 / VIP 100，超限 30003）+ 记下载 */
    @Transactional
    public Map<String, Object> downloadUrl(Long songId, Long userId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (song.getAuditStatus() == null || song.getAuditStatus() != 1
                || song.getStatus() == null || song.getStatus() != 1) {
            throw new BizException(ErrorCode.SONG_UNAVAILABLE, "歌曲已下架或不可用");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "token 无效或已过期");
        }
        boolean vip = AuthService.isVipActive(user);
        int payType = song.getPayType() == null ? 0 : song.getPayType();
        if (payType == 1 && !vip) {
            throw new BizException(ErrorCode.VIP_REQUIRED, "VIP 专享，请开通会员");
        }
        if (payType == 2 && userSongPurchaseMapper.selectCount(new LambdaQueryWrapper<UserSongPurchase>()
                .eq(UserSongPurchase::getUserId, userId)
                .eq(UserSongPurchase::getSongId, songId)) == 0) {
            throw new BizException(ErrorCode.PURCHASE_REQUIRED, "付费单曲，请先购买");
        }
        // 每日配额
        int quota = vip ? systemConfigService.getInt("download.quota.vip", 100)
                : systemConfigService.getInt("download.quota.normal", 10);
        long usedToday = downloadRecordMapper.selectCount(new LambdaQueryWrapper<DownloadRecord>()
                .eq(DownloadRecord::getUserId, userId)
                .ge(DownloadRecord::getCreateTime, LocalDate.now().atStartOfDay()));
        if (usedToday >= quota) {
            throw new BizException(ErrorCode.DOWNLOAD_QUOTA_EXCEEDED, "今日下载配额已用完");
        }

        String ext = song.getFilePath() != null && song.getFilePath().contains(".")
                ? song.getFilePath().substring(song.getFilePath().lastIndexOf('.')) : ".mp3";
        String url = minioUtil.presignedGetUrl("music", song.getFilePath(),
                URL_EXPIRE_SECONDS, song.getName() + ext);

        DownloadRecord record = new DownloadRecord();
        record.setUserId(userId);
        record.setSongId(songId);
        downloadRecordMapper.insert(record);
        song.setDownloadCount((song.getDownloadCount() == null ? 0 : song.getDownloadCount()) + 1);
        songMapper.updateById(song);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);
        data.put("expiresIn", URL_EXPIRE_SECONDS);
        data.put("quotaLeft", quota - usedToday - 1);
        return data;
    }

    /** 下载记录（api.md 14.2） */
    public PageResult<Map<String, Object>> records(Long userId, long pageNum, long pageSize) {
        Page<DownloadRecord> page = downloadRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<DownloadRecord>()
                        .eq(DownloadRecord::getUserId, userId)
                        .orderByDesc(DownloadRecord::getCreateTime));
        List<Long> songIds = page.getRecords().stream().map(DownloadRecord::getSongId).distinct().toList();
        List<Song> songs = songIds.isEmpty() ? List.of() : songMapper.selectBatchIds(songIds);
        Map<Long, Long> sizeMap = songs.stream().collect(Collectors.toMap(Song::getId,
                s -> s.getFileSize() == null ? 0L : s.getFileSize()));
        Map<Long, SongVO> voMap = songAssembler.toVOs(songs).stream()
                .collect(Collectors.toMap(SongVO::getId, Function.identity()));
        return PageResult.from(page, list -> list.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("song", voMap.get(r.getSongId()));
            item.put("fileSize", sizeMap.getOrDefault(r.getSongId(), 0L));
            item.put("downloadAt", r.getCreateTime());
            return item;
        }).toList());
    }
}
