package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Album;
import com.yinyu.entity.Singer;
import com.yinyu.entity.Song;
import com.yinyu.mapper.AlbumMapper;
import com.yinyu.mapper.SingerMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.AlbumVO;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户专辑：分页/详情/曲目（api.md 5.x）
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final SingerMapper singerMapper;
    private final SongMapper songMapper;
    private final SongAssembler songAssembler;
    private final MinioUtil minioUtil;

    public PageResult<AlbumVO> page(long pageNum, long pageSize, Long singerId) {
        Page<Album> page = albumMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getStatus, 1)
                        .eq(singerId != null, Album::getSingerId, singerId)
                        .orderByDesc(Album::getPublishDate));
        return PageResult.from(page, list -> list.stream().map(a -> toVO(a, false, null)).toList());
    }

    public AlbumVO detail(Long id, Long userId) {
        Album album = albumMapper.selectById(id);
        if (album == null || album.getStatus() == null || album.getStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        // 本期无专辑收藏表，collected 登录后恒 false，二期实现
        return toVO(album, true, userId == null ? null : false);
    }

    /** 专辑曲目（不分页，按上架时间） */
    public List<SongVO> songs(Long id) {
        List<Song> songs = songMapper.selectList(new LambdaQueryWrapper<Song>()
                .eq(Song::getAlbumId, id)
                .eq(Song::getAuditStatus, 1).eq(Song::getStatus, 1)
                .orderByAsc(Song::getId));
        return songAssembler.toVOs(songs);
    }

    public AlbumVO toVO(Album album, boolean detail, Boolean collected) {
        AlbumVO vo = new AlbumVO();
        vo.setId(album.getId());
        vo.setName(album.getName());
        vo.setCover(minioUtil.publicImageUrl(album.getCover()));
        vo.setSingerId(album.getSingerId());
        Singer singer = album.getSingerId() == null ? null : singerMapper.selectById(album.getSingerId());
        vo.setSingerName(singer == null ? null : singer.getName());
        vo.setPublishDate(album.getPublishDate());
        vo.setSongCount(album.getSongCount());
        if (detail) {
            vo.setIntro(album.getIntroduction());
            vo.setCollected(collected);
        }
        return vo;
    }
}
