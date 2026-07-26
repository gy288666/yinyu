package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.dto.AlbumRequest;
import com.yinyu.dto.CategoryRequest;
import com.yinyu.dto.SingerRequest;
import com.yinyu.entity.Album;
import com.yinyu.entity.Category;
import com.yinyu.entity.Singer;
import com.yinyu.entity.Song;
import com.yinyu.entity.SongSinger;
import com.yinyu.mapper.AlbumMapper;
import com.yinyu.mapper.CategoryMapper;
import com.yinyu.mapper.SingerMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.SongSingerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台内容资源 CRUD：歌手/专辑/分类（api.md 18.4）
 */
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final SingerMapper singerMapper;
    private final AlbumMapper albumMapper;
    private final CategoryMapper categoryMapper;
    private final SongMapper songMapper;
    private final SongSingerMapper songSingerMapper;

    // ---------------- 歌手 ----------------

    public PageResult<Singer> singerPage(long pageNum, long pageSize, String name) {
        Page<Singer> page = singerMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Singer>()
                        .like(name != null && !name.isEmpty(), Singer::getName, name)
                        .orderByDesc(Singer::getId));
        return PageResult.from(page, list -> list);
    }

    public Singer singerCreate(SingerRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: name 必填");
        }
        Singer singer = new Singer();
        singer.setName(req.getName());
        singer.setAvatar(req.getAvatar());
        singer.setRegion(req.getArea());
        singer.setType(req.getType() == null ? 1 : req.getType());
        singer.setIntroduction(req.getIntro());
        singer.setHot(0L);
        singer.setStatus(1);
        singerMapper.insert(singer);
        return singer;
    }

    public void singerUpdate(Long id, SingerRequest req) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            singer.setName(req.getName());
        }
        if (req.getAvatar() != null) {
            singer.setAvatar(req.getAvatar());
        }
        if (req.getArea() != null) {
            singer.setRegion(req.getArea());
        }
        if (req.getType() != null) {
            singer.setType(req.getType());
        }
        if (req.getIntro() != null) {
            singer.setIntroduction(req.getIntro());
        }
        singerMapper.updateById(singer);
    }

    /** 删除歌手：有歌曲/专辑引用返回 20005 */
    public void singerDelete(Long id) {
        if (singerMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long songRefs = songSingerMapper.selectCount(
                new LambdaQueryWrapper<SongSinger>().eq(SongSinger::getSingerId, id));
        Long albumRefs = albumMapper.selectCount(
                new LambdaQueryWrapper<Album>().eq(Album::getSingerId, id));
        if (songRefs > 0 || albumRefs > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "存在歌曲或专辑引用，不可删除");
        }
        singerMapper.deleteById(id);
    }

    // ---------------- 专辑 ----------------

    public PageResult<Album> albumPage(long pageNum, long pageSize, String name, Long singerId) {
        Page<Album> page = albumMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Album>()
                        .like(name != null && !name.isEmpty(), Album::getName, name)
                        .eq(singerId != null, Album::getSingerId, singerId)
                        .orderByDesc(Album::getId));
        return PageResult.from(page, list -> list);
    }

    public Album albumCreate(AlbumRequest req) {
        if (req.getName() == null || req.getName().isBlank() || req.getSingerId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: name/singerId 必填");
        }
        if (singerMapper.selectById(req.getSingerId()) == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "歌手不存在");
        }
        Album album = new Album();
        album.setName(req.getName());
        album.setSingerId(req.getSingerId());
        album.setCover(req.getCover());
        album.setPublishDate(req.getPublishDate());
        album.setCompany(req.getCompany());
        album.setIntroduction(req.getIntro());
        album.setSongCount(0);
        album.setStatus(1);
        albumMapper.insert(album);
        return album;
    }

    public void albumUpdate(Long id, AlbumRequest req) {
        Album album = albumMapper.selectById(id);
        if (album == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            album.setName(req.getName());
        }
        if (req.getSingerId() != null) {
            album.setSingerId(req.getSingerId());
        }
        if (req.getCover() != null) {
            album.setCover(req.getCover());
        }
        if (req.getPublishDate() != null) {
            album.setPublishDate(req.getPublishDate());
        }
        if (req.getCompany() != null) {
            album.setCompany(req.getCompany());
        }
        if (req.getIntro() != null) {
            album.setIntroduction(req.getIntro());
        }
        albumMapper.updateById(album);
    }

    public void albumDelete(Long id) {
        if (albumMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long refs = songMapper.selectCount(new LambdaQueryWrapper<Song>().eq(Song::getAlbumId, id));
        if (refs > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "存在歌曲引用，不可删除");
        }
        albumMapper.deleteById(id);
    }

    // ---------------- 分类 ----------------

    /** 后台分类树（含停用项） */
    public List<Map<String, Object>> categoryTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Category root : all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0).toList()) {
            Map<String, Object> node = categoryNode(root);
            node.put("children", all.stream()
                    .filter(c -> root.getId().equals(c.getParentId()))
                    .map(this::categoryNode).toList());
            roots.add(node);
        }
        return roots;
    }

    private Map<String, Object> categoryNode(Category c) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", c.getId());
        node.put("name", c.getName());
        node.put("parentId", c.getParentId());
        node.put("sort", c.getSort());
        node.put("enabled", c.getStatus() != null && c.getStatus() == 1);
        return node;
    }

    public Category categoryCreate(CategoryRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: name 必填");
        }
        Category category = new Category();
        category.setName(req.getName());
        category.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        category.setSort(req.getSort() == null ? 0 : req.getSort());
        category.setStatus(req.getEnabled() == null || req.getEnabled() ? 1 : 0);
        categoryMapper.insert(category);
        return category;
    }

    public void categoryUpdate(Long id, CategoryRequest req) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            category.setName(req.getName());
        }
        if (req.getParentId() != null) {
            category.setParentId(req.getParentId());
        }
        if (req.getSort() != null) {
            category.setSort(req.getSort());
        }
        if (req.getEnabled() != null) {
            category.setStatus(req.getEnabled() ? 1 : 0);
        }
        categoryMapper.updateById(category);
    }

    /** 删除分类：被歌曲或子分类引用返回 20005 */
    public void categoryDelete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        Long songRefs = songMapper.selectCount(
                new LambdaQueryWrapper<Song>().eq(Song::getCategoryId, id));
        Long childRefs = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (songRefs > 0 || childRefs > 0) {
            throw new BizException(ErrorCode.REFERENCED_CANNOT_DELETE, "存在歌曲或子分类引用，不可删除");
        }
        categoryMapper.deleteById(id);
    }
}
