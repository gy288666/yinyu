package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinyu.entity.Album;
import com.yinyu.entity.Category;
import com.yinyu.entity.Singer;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongSinger;
import com.yinyu.mapper.AlbumMapper;
import com.yinyu.mapper.CategoryMapper;
import com.yinyu.mapper.SingerMapper;
import com.yinyu.mapper.SongSingerMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Song -> SongVO 组装器：批量补齐歌手/专辑/分类冗余字段，供各模块复用
 */
@Service
@RequiredArgsConstructor
public class SongAssembler {

    private final SongSingerMapper songSingerMapper;
    private final SingerMapper singerMapper;
    private final AlbumMapper albumMapper;
    private final CategoryMapper categoryMapper;
    private final MinioUtil minioUtil;

    public SongVO toVO(Song song) {
        return toVOs(Collections.singletonList(song)).get(0);
    }

    public List<SongVO> toVOs(List<Song> songs) {
        List<SongVO> result = new ArrayList<>();
        if (songs == null || songs.isEmpty()) {
            return result;
        }
        List<Long> songIds = songs.stream().map(Song::getId).toList();

        // 歌手：song_singer 按署名顺序
        List<SongSinger> relations = songSingerMapper.selectList(
                new LambdaQueryWrapper<SongSinger>().in(SongSinger::getSongId, songIds)
                        .orderByAsc(SongSinger::getSort));
        Map<Long, Singer> singerMap = new HashMap<>();
        if (!relations.isEmpty()) {
            List<Long> singerIds = relations.stream().map(SongSinger::getSingerId).distinct().toList();
            singerMap = singerMapper.selectBatchIds(singerIds).stream()
                    .collect(Collectors.toMap(Singer::getId, Function.identity()));
        }
        Map<Long, List<SongSinger>> songSingers = relations.stream()
                .collect(Collectors.groupingBy(SongSinger::getSongId));

        // 专辑 / 分类
        List<Long> albumIds = songs.stream().map(Song::getAlbumId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Album> albumMap = albumIds.isEmpty() ? Map.of()
                : albumMapper.selectBatchIds(albumIds).stream()
                        .collect(Collectors.toMap(Album::getId, Function.identity()));
        List<Long> categoryIds = songs.stream().map(Song::getCategoryId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity()));

        for (Song song : songs) {
            SongVO vo = new SongVO();
            vo.setId(song.getId());
            vo.setName(song.getName());
            vo.setDuration(song.getDuration());
            List<SongSinger> ss = songSingers.getOrDefault(song.getId(), List.of());
            if (!ss.isEmpty()) {
                vo.setSingerId(ss.get(0).getSingerId());
                Map<Long, Singer> finalSingerMap = singerMap;
                vo.setSingerName(ss.stream()
                        .map(r -> finalSingerMap.get(r.getSingerId()))
                        .filter(java.util.Objects::nonNull)
                        .map(Singer::getName)
                        .collect(Collectors.joining("/")));
            }
            Album album = song.getAlbumId() == null ? null : albumMap.get(song.getAlbumId());
            if (album != null) {
                vo.setAlbumId(album.getId());
                vo.setAlbumName(album.getName());
            }
            String cover = song.getCover() != null ? song.getCover()
                    : (album != null ? album.getCover() : null);
            vo.setCover(minioUtil.publicImageUrl(cover));
            Category category = song.getCategoryId() == null ? null : categoryMap.get(song.getCategoryId());
            if (category != null) {
                vo.setCategoryId(category.getId());
                vo.setCategoryName(category.getName());
            }
            vo.setVip(song.getPayType() != null && song.getPayType() == 1);
            vo.setPrice(song.getPrice() == null ? "0.00"
                    : song.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
            vo.setQuality(quality(song.getFilePath()));
            vo.setPlayCount(song.getPlayCount());
            vo.setPublishTime(song.getPublishTime());
            result.add(vo);
        }
        return result;
    }

    /** 音质按音频后缀推断：flac/wav -> lossless */
    public static String quality(String filePath) {
        if (filePath == null) {
            return "standard";
        }
        String lower = filePath.toLowerCase();
        return (lower.endsWith(".flac") || lower.endsWith(".wav")) ? "lossless" : "standard";
    }
}
