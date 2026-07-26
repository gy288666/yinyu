package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Album;
import com.yinyu.entity.Singer;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongSinger;
import com.yinyu.mapper.AlbumMapper;
import com.yinyu.mapper.SingerMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.SongSingerMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.AlbumVO;
import com.yinyu.vo.SingerVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户歌手：分页/详情/歌手歌曲/歌手专辑（api.md 4.x）
 */
@Service
@RequiredArgsConstructor
public class SingerService {

    private final SingerMapper singerMapper;
    private final SongSingerMapper songSingerMapper;
    private final SongMapper songMapper;
    private final AlbumMapper albumMapper;
    private final SongAssembler songAssembler;
    private final AlbumService albumService;
    private final MinioUtil minioUtil;

    public PageResult<SingerVO> page(long pageNum, long pageSize, String area, Integer type, String initial) {
        LambdaQueryWrapper<Singer> wrapper = new LambdaQueryWrapper<Singer>()
                .eq(Singer::getStatus, 1)
                .eq(area != null && !area.isEmpty(), Singer::getRegion, area)
                .eq(type != null, Singer::getType, type)
                .likeRight(initial != null && !initial.isEmpty(), Singer::getPinyin,
                        initial == null ? null : initial.toLowerCase())
                .orderByDesc(Singer::getHot);
        Page<Singer> page = singerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.from(page, list -> list.stream().map(s -> toVO(s, false)).toList());
    }

    public SingerVO detail(Long id) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null || singer.getStatus() == null || singer.getStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return toVO(singer, true);
    }

    /** 歌手歌曲分页（仅已上架） */
    public PageResult<SongVO> songs(Long id, long pageNum, long pageSize) {
        List<Long> songIds = songSingerMapper.selectList(
                        new LambdaQueryWrapper<SongSinger>().eq(SongSinger::getSingerId, id))
                .stream().map(SongSinger::getSongId).toList();
        if (songIds.isEmpty()) {
            return PageResult.of(pageNum, pageSize, 0, List.of());
        }
        Page<Song> page = songMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Song>()
                        .in(Song::getId, songIds)
                        .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1)
                        .orderByDesc(Song::getPlayCount));
        return PageResult.from(page, songAssembler::toVOs);
    }

    /** 歌手专辑分页 */
    public PageResult<AlbumVO> albums(Long id, long pageNum, long pageSize) {
        Page<Album> page = albumMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getSingerId, id).eq(Album::getStatus, 1)
                        .orderByDesc(Album::getPublishDate));
        return PageResult.from(page, list -> list.stream().map(a -> albumService.toVO(a, false, null)).toList());
    }

    private SingerVO toVO(Singer singer, boolean detail) {
        SingerVO vo = new SingerVO();
        vo.setId(singer.getId());
        vo.setName(singer.getName());
        vo.setAvatar(minioUtil.publicImageUrl(singer.getAvatar()));
        vo.setArea(singer.getRegion());
        vo.setType(singer.getType());
        vo.setSongCount(songSingerMapper.selectCount(
                new LambdaQueryWrapper<SongSinger>().eq(SongSinger::getSingerId, singer.getId())));
        if (detail) {
            vo.setIntro(singer.getIntroduction());
            vo.setAlbumCount(albumMapper.selectCount(
                    new LambdaQueryWrapper<Album>().eq(Album::getSingerId, singer.getId())));
        }
        return vo;
    }
}
