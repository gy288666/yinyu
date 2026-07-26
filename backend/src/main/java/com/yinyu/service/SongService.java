package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongSinger;
import com.yinyu.entity.User;
import com.yinyu.entity.UserLikeSong;
import com.yinyu.entity.UserSongPurchase;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.SongSingerMapper;
import com.yinyu.mapper.UserLikeSongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.mapper.UserSongPurchaseMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.PlayUrlVO;
import com.yinyu.vo.SongDetailVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 门户歌曲：分页/详情/新歌速递/播放地址（核心）
 */
@Service
@RequiredArgsConstructor
public class SongService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SongMapper songMapper;
    private final SongSingerMapper songSingerMapper;
    private final UserMapper userMapper;
    private final UserLikeSongMapper userLikeSongMapper;
    private final UserSongPurchaseMapper userSongPurchaseMapper;
    private final SongAssembler songAssembler;
    private final MinioUtil minioUtil;
    private final StringRedisTemplate redis;
    private final RecentPlayService recentPlayService;

    /** 已上架 = 审核通过 + 上架状态 */
    private LambdaQueryWrapper<Song> onlineWrapper() {
        return new LambdaQueryWrapper<Song>()
                .eq(Song::getAuditStatus, 1)
                .eq(Song::getStatus, 1);
    }

    /** 歌曲分页（api.md 3.1） */
    public PageResult<SongVO> page(long pageNum, long pageSize, Long categoryId, Long singerId,
                                   Boolean vip, String quality, String sort) {
        LambdaQueryWrapper<Song> wrapper = onlineWrapper();
        if (categoryId != null) {
            wrapper.eq(Song::getCategoryId, categoryId);
        }
        if (singerId != null) {
            List<Long> songIds = songSingerMapper.selectList(
                            new LambdaQueryWrapper<SongSinger>().eq(SongSinger::getSingerId, singerId))
                    .stream().map(SongSinger::getSongId).toList();
            if (songIds.isEmpty()) {
                return PageResult.of(pageNum, pageSize, 0, List.of());
            }
            wrapper.in(Song::getId, songIds);
        }
        if (Boolean.TRUE.equals(vip)) {
            wrapper.eq(Song::getPayType, 1);
        }
        if ("lossless".equals(quality)) {
            wrapper.apply("(file_path LIKE '%.flac' OR file_path LIKE '%.wav')");
        } else if ("standard".equals(quality)) {
            wrapper.apply("file_path NOT LIKE '%.flac' AND file_path NOT LIKE '%.wav'");
        }
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Song::getPlayCount);
        } else {
            wrapper.orderByDesc(Song::getPublishTime);
        }
        Page<Song> page = songMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, songAssembler::toVOs);
    }

    /** 歌曲详情（api.md 3.2） */
    public SongDetailVO detail(Long id, Long userId) {
        Song song = songMapper.selectById(id);
        if (song == null || song.getAuditStatus() == null || song.getAuditStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        SongVO base = songAssembler.toVO(song);
        SongDetailVO vo = new SongDetailVO();
        BeanUtils.copyProperties(base, vo);
        // 歌词返回 LRC 文本内容（MinIO 读取 / 本地降级，文件不存在为空）
        vo.setLyric(song.getLyricPath() == null ? null
                : minioUtil.readString("music", song.getLyricPath()));
        vo.setLiked(userId != null && userLikeSongMapper.selectCount(
                new LambdaQueryWrapper<UserLikeSong>()
                        .eq(UserLikeSong::getUserId, userId)
                        .eq(UserLikeSong::getSongId, id)) > 0);
        vo.setPurchased(userId != null && purchased(userId, id));
        return vo;
    }

    /** 新歌速递（api.md 3.4）：近 30 天上架按时间倒序 */
    public List<SongVO> newest(int limit) {
        List<Song> songs = songMapper.selectList(onlineWrapper()
                .ge(Song::getPublishTime, LocalDateTime.now().minusDays(30))
                .orderByDesc(Song::getPublishTime)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 50)));
        return songAssembler.toVOs(songs);
    }

    /** 歌词（api.md 3.5）：返回歌词文本内容（MinIO 读取 / 本地降级），文件不存在时 content 为空并带提示 */
    public Object lyric(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String content = song.getLyricPath() == null ? null
                : minioUtil.readString("music", song.getLyricPath());
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("songId", id);
        data.put("content", content == null ? "" : content);
        data.put("lyric", content == null ? "" : content); // 兼容 api.md 3.5 字段名
        if (content == null) {
            data.put("hint", song.getLyricPath() == null ? "纯音乐，暂无歌词" : "歌词文件暂未上传");
        }
        return data;
    }

    /**
     * 获取播放地址（api.md 3.3，核心接口）：
     * 上架校验(20003) -> 权益校验(VIP 30001 / 单曲 30002) -> 预签名 URL（MinIO 不可用时降级路径）
     * -> Redis 埋点（30 秒去重：play:count:{songId} INCR + 日榜 ZINCRBY）-> 登录用户写最近播放
     */
    public PlayUrlVO playUrl(Long id, Long userId, String clientKey) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (song.getAuditStatus() == null || song.getAuditStatus() != 1
                || song.getStatus() == null || song.getStatus() != 1) {
            throw new BizException(ErrorCode.SONG_UNAVAILABLE, "歌曲已下架或不可用");
        }
        int payType = song.getPayType() == null ? 0 : song.getPayType();
        User user = userId == null ? null : userMapper.selectById(userId);
        int trialSeconds = 0;
        if (payType == 1) { // 会员免费
            if (user == null || !AuthService.isVipActive(user)) {
                throw new BizException(ErrorCode.VIP_REQUIRED, "VIP 专享，请开通会员");
            }
        } else if (payType == 2) { // 单曲购买
            if (user == null || !purchased(userId, id)) {
                throw new BizException(ErrorCode.PURCHASE_REQUIRED, "付费单曲，请先购买");
            }
        } else if (user == null) {
            trialSeconds = 60; // 游客播放普通曲目试听 60 秒
        }

        String url = minioUtil.presignedGetUrl("music", song.getFilePath());

        // Redis 埋点：同一用户/客户端 30 秒内重复请求不重复计数
        String dedupKey = RedisKeys.PLAY_DEDUP_PREFIX
                + (userId != null ? "u" + userId : "g" + clientKey) + ":" + id;
        Boolean first = redis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofSeconds(30));
        if (Boolean.TRUE.equals(first)) {
            redis.opsForValue().increment(RedisKeys.PLAY_COUNT_PREFIX + id);
            redis.opsForZSet().incrementScore(
                    RedisKeys.RANK_DAY_PREFIX + LocalDate.now().format(DAY), String.valueOf(id), 1);
            if (userId != null) {
                recentPlayService.record(userId, id);
            }
        }
        return new PlayUrlVO(id, url, minioUtil.getPresignExpireSeconds(), trialSeconds);
    }

    private boolean purchased(Long userId, Long songId) {
        return userSongPurchaseMapper.selectCount(new LambdaQueryWrapper<UserSongPurchase>()
                .eq(UserSongPurchase::getUserId, userId)
                .eq(UserSongPurchase::getSongId, songId)) > 0;
    }
}
