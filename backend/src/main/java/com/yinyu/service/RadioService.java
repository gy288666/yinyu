package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.constant.RedisKeys;
import com.yinyu.common.exception.BizException;
import com.yinyu.entity.Radio;
import com.yinyu.entity.RadioProgram;
import com.yinyu.entity.Song;
import com.yinyu.mapper.RadioMapper;
import com.yinyu.mapper.RadioProgramMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 电台：列表/详情/节目列表/随机下一曲（api.md 10.6/10.7）
 */
@Service
@RequiredArgsConstructor
public class RadioService {

    private final RadioMapper radioMapper;
    private final RadioProgramMapper radioProgramMapper;
    private final SongMapper songMapper;
    private final SongAssembler songAssembler;
    private final MinioUtil minioUtil;
    private final StringRedisTemplate redis;

    /** 电台列表（10.6） */
    public List<Map<String, Object>> list() {
        return radioMapper.selectList(new LambdaQueryWrapper<Radio>()
                        .eq(Radio::getStatus, 1).orderByAsc(Radio::getSort))
                .stream().map(this::toVO).toList();
    }

    /** 电台详情（自定义补充）：含节目数 */
    public Map<String, Object> detail(Long id) {
        Radio radio = requireRadio(id);
        Map<String, Object> vo = toVO(radio);
        vo.put("playCount", radio.getPlayCount());
        vo.put("programCount", radioProgramMapper.selectCount(new LambdaQueryWrapper<RadioProgram>()
                .eq(RadioProgram::getRadioId, id).eq(RadioProgram::getStatus, 1)));
        return vo;
    }

    /** 节目列表（自定义补充） */
    public List<Map<String, Object>> programs(Long id) {
        requireRadio(id);
        return radioProgramMapper.selectList(new LambdaQueryWrapper<RadioProgram>()
                        .eq(RadioProgram::getRadioId, id).eq(RadioProgram::getStatus, 1)
                        .orderByAsc(RadioProgram::getSort))
                .stream().map(p -> {
                    Map<String, Object> vo = new LinkedHashMap<>();
                    vo.put("id", p.getId());
                    vo.put("title", p.getTitle());
                    vo.put("songId", p.getSongId());
                    vo.put("duration", p.getDuration());
                    vo.put("publishTime", p.getPublishTime());
                    return vo;
                }).toList();
    }

    /** 电台随机曲目（10.7）：一轮内不重复（Redis 已播集合） */
    public SongVO next(Long id, Long userId, String clientKey) {
        requireRadio(id);
        List<RadioProgram> programs = radioProgramMapper.selectList(new LambdaQueryWrapper<RadioProgram>()
                .eq(RadioProgram::getRadioId, id).eq(RadioProgram::getStatus, 1)
                .isNotNull(RadioProgram::getSongId));
        if (programs.isEmpty()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "该电台暂无节目");
        }
        String playedKey = RedisKeys.RADIO_PLAYED_PREFIX + id + ":"
                + (userId != null ? "u" + userId : "g" + clientKey);
        Set<String> played = redis.opsForSet().members(playedKey);
        List<RadioProgram> remaining = programs.stream()
                .filter(p -> played == null || !played.contains(String.valueOf(p.getSongId())))
                .toList();
        if (remaining.isEmpty()) {
            redis.delete(playedKey); // 一轮播完，重新开始
            remaining = programs;
        }
        RadioProgram pick = remaining.get(ThreadLocalRandom.current().nextInt(remaining.size()));
        redis.opsForSet().add(playedKey, String.valueOf(pick.getSongId()));
        redis.expire(playedKey, Duration.ofHours(12));
        Song song = songMapper.selectById(pick.getSongId());
        if (song == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "节目曲目不存在");
        }
        return songAssembler.toVO(song);
    }

    private Radio requireRadio(Long id) {
        Radio radio = radioMapper.selectById(id);
        if (radio == null || radio.getStatus() == null || radio.getStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return radio;
    }

    private Map<String, Object> toVO(Radio radio) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", radio.getId());
        vo.put("name", radio.getName());
        vo.put("cover", minioUtil.publicImageUrl(radio.getCover()));
        vo.put("desc", radio.getIntroduction());
        return vo;
    }
}
