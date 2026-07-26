package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.entity.Activity;
import com.yinyu.entity.Announcement;
import com.yinyu.entity.Banner;
import com.yinyu.mapper.ActivityMapper;
import com.yinyu.mapper.AnnouncementMapper;
import com.yinyu.mapper.BannerMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.util.Params;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 运营内容：轮播图/公告/活动（门户展示 16.x + 后台管理 18.6）
 */
@Service
@RequiredArgsConstructor
public class OpsContentService {

    /** banner.link_type：0-无 1-歌曲 2-歌单 3-专辑 4-活动 5-外链 */
    private static final Map<String, Integer> TARGET_TYPE = Map.of(
            "NONE", 0, "SONG", 1, "PLAYLIST", 2, "ALBUM", 3, "ACTIVITY", 4, "URL", 5);
    private static final String[] TARGET_NAMES = {"NONE", "SONG", "PLAYLIST", "ALBUM", "ACTIVITY", "URL"};
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png");

    private final BannerMapper bannerMapper;
    private final AnnouncementMapper announcementMapper;
    private final ActivityMapper activityMapper;
    private final MinioUtil minioUtil;

    // ---------------- 门户展示（api.md 16.x） ----------------

    /** 轮播图（16.1）：启用且在有效期内，按 sort 升序 */
    public List<Map<String, Object>> banners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, 1)
                        .and(w -> w.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now))
                        .and(w -> w.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now))
                        .orderByAsc(Banner::getSort))
                .stream().map(this::bannerVO).toList();
    }

    public Map<String, Object> bannerVO(Banner b) {
        int linkType = b.getLinkType() == null ? 0 : b.getLinkType();
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", b.getId());
        vo.put("image", minioUtil.publicImageUrl(b.getImage()));
        vo.put("title", b.getTitle());
        vo.put("targetType", TARGET_NAMES[Math.min(linkType, 5)]);
        vo.put("targetId", linkType >= 1 && linkType <= 4 && b.getLinkValue() != null
                && b.getLinkValue().matches("\\d+") ? Long.valueOf(b.getLinkValue()) : null);
        vo.put("link", linkType == 5 ? b.getLinkValue() : null);
        vo.put("sort", b.getSort());
        vo.put("startTime", b.getStartTime());
        vo.put("endTime", b.getEndTime());
        vo.put("enabled", b.getStatus() != null && b.getStatus() == 1);
        return vo;
    }

    /** 公告列表（16.2）：已发布，置顶优先 */
    public List<Map<String, Object>> notices(int limit) {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .le(Announcement::getPublishTime, LocalDateTime.now())
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getPublishTime)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 50)))
                .stream().map(n -> noticeVO(n, false)).toList();
    }

    /** 公告详情（16.2） */
    public Map<String, Object> noticeDetail(Long id) {
        Announcement notice = announcementMapper.selectById(id);
        if (notice == null || notice.getStatus() == null || notice.getStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return noticeVO(notice, true);
    }

    private Map<String, Object> noticeVO(Announcement n, boolean withContent) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", n.getId());
        vo.put("title", n.getTitle());
        vo.put("top", n.getIsTop() != null && n.getIsTop() == 1);
        vo.put("publishTime", n.getPublishTime());
        if (withContent) {
            vo.put("content", n.getContent());
        }
        return vo;
    }

    /** 活动列表（16.3）：已发布，含 ONGOING/ENDED 状态 */
    public PageResult<Map<String, Object>> activities(long pageNum, long pageSize) {
        Page<Activity> page = activityMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Activity>()
                        .in(Activity::getStatus, 1, 2)
                        .orderByAsc(Activity::getSort)
                        .orderByDesc(Activity::getStartTime));
        return PageResult.from(page, list -> list.stream().map(a -> activityVO(a, false)).toList());
    }

    /** 活动详情（16.3） */
    public Map<String, Object> activityDetail(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null || activity.getStatus() == null || activity.getStatus() == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return activityVO(activity, true);
    }

    public Map<String, Object> activityVO(Activity a, boolean withContent) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", a.getId());
        vo.put("title", a.getTitle());
        vo.put("cover", minioUtil.publicImageUrl(a.getCover()));
        vo.put("startTime", a.getStartTime());
        vo.put("endTime", a.getEndTime());
        boolean online = a.getStatus() != null && a.getStatus() != 0;
        vo.put("status", !online ? "OFFLINE"
                : a.getEndTime() != null && a.getEndTime().isBefore(now) ? "ENDED" : "ONGOING");
        if (withContent) {
            vo.put("content", a.getContent());
        }
        return vo;
    }

    // ---------------- 后台管理（api.md 18.6） ----------------

    /** 轮播图分页（18.6.1） */
    public PageResult<Map<String, Object>> bannerPage(long pageNum, long pageSize) {
        Page<Banner> page = bannerMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSort));
        return PageResult.from(page, list -> list.stream().map(this::bannerVO).toList());
    }

    public Map<String, Object> bannerCreate(Map<String, Object> body) {
        Banner banner = new Banner();
        applyBanner(banner, body, true);
        bannerMapper.insert(banner);
        return bannerVO(banner);
    }

    public void bannerUpdate(Long id, Map<String, Object> body) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        applyBanner(banner, body, false);
        bannerMapper.updateById(banner);
    }

    public void bannerDelete(Long id) {
        if (bannerMapper.deleteById(id) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
    }

    private void applyBanner(Banner banner, Map<String, Object> body, boolean create) {
        String image = Params.str(body, "image");
        String title = Params.str(body, "title");
        if (create) {
            if (image == null || title == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: image/title 不能为空");
            }
        }
        if (image != null) {
            banner.setImage(stripStaticBase(image));
        }
        if (title != null) {
            banner.setTitle(title);
        }
        String targetType = Params.str(body, "targetType");
        if (targetType != null) {
            Integer linkType = TARGET_TYPE.get(targetType.toUpperCase());
            if (linkType == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: targetType 不合法");
            }
            banner.setLinkType(linkType);
            banner.setLinkValue(linkType == 5 ? Params.str(body, "link")
                    : linkType == 0 ? null : String.valueOf(Params.lng(body, "targetId")));
        }
        Integer sort = Params.integer(body, "sort");
        if (sort != null) {
            banner.setSort(sort);
        } else if (create) {
            banner.setSort(0);
        }
        if (body.containsKey("startTime")) {
            banner.setStartTime(Params.dateTime(body, "startTime"));
        }
        if (body.containsKey("endTime")) {
            banner.setEndTime(Params.dateTime(body, "endTime"));
        }
        Boolean enabled = Params.bool(body, "enabled");
        if (enabled != null) {
            banner.setStatus(enabled ? 1 : 0);
        } else if (create) {
            banner.setStatus(1);
        }
    }

    /** 公告分页（18.6.5） */
    public PageResult<Map<String, Object>> noticePage(long pageNum, long pageSize, String title) {
        Page<Announcement> page = announcementMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Announcement>()
                        .like(title != null && !title.isEmpty(), Announcement::getTitle, title)
                        .orderByDesc(Announcement::getPublishTime));
        return PageResult.from(page, list -> list.stream().map(n -> {
            Map<String, Object> vo = noticeVO(n, true);
            vo.put("status", n.getStatus() != null && n.getStatus() == 1 ? "PUBLISHED" : "DRAFT");
            return vo;
        }).toList());
    }

    public Map<String, Object> noticeCreate(Map<String, Object> body, Long adminId) {
        Announcement notice = new Announcement();
        notice.setTitle(Params.requireStr(body, "title"));
        notice.setContent(Params.requireStr(body, "content"));
        notice.setIsTop(Boolean.TRUE.equals(Params.bool(body, "top")) ? 1 : 0);
        notice.setAdminId(adminId);
        LocalDateTime publishTime = Params.dateTime(body, "publishTime");
        notice.setPublishTime(publishTime == null ? LocalDateTime.now() : publishTime);
        notice.setStatus(1);
        announcementMapper.insert(notice);
        return noticeVO(notice, true);
    }

    public void noticeUpdate(Long id, Map<String, Object> body) {
        Announcement notice = announcementMapper.selectById(id);
        if (notice == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String title = Params.str(body, "title");
        if (title != null) {
            notice.setTitle(title);
        }
        String content = Params.str(body, "content");
        if (content != null) {
            notice.setContent(content);
        }
        Boolean top = Params.bool(body, "top");
        if (top != null) {
            notice.setIsTop(top ? 1 : 0);
        }
        if (body.containsKey("publishTime")) {
            notice.setPublishTime(Params.dateTime(body, "publishTime"));
        }
        announcementMapper.updateById(notice);
    }

    public void noticeDelete(Long id) {
        if (announcementMapper.deleteById(id) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
    }

    /** 活动分页（18.6.9，含未发布） */
    public PageResult<Map<String, Object>> activityPage(long pageNum, long pageSize, String title) {
        Page<Activity> page = activityMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Activity>()
                        .like(title != null && !title.isEmpty(), Activity::getTitle, title)
                        .orderByDesc(Activity::getCreateTime));
        return PageResult.from(page, list -> list.stream().map(a -> activityVO(a, true)).toList());
    }

    public Map<String, Object> activityCreate(Map<String, Object> body) {
        Activity activity = new Activity();
        activity.setTitle(Params.requireStr(body, "title"));
        activity.setCover(stripStaticBase(Params.str(body, "cover")));
        activity.setContent(Params.str(body, "content"));
        LocalDateTime startTime = Params.dateTime(body, "startTime");
        LocalDateTime endTime = Params.dateTime(body, "endTime");
        if (startTime == null || endTime == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: startTime/endTime 不能为空");
        }
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        Integer sort = Params.integer(body, "sort");
        activity.setSort(sort == null ? 0 : sort);
        activity.setStatus(1);
        activityMapper.insert(activity);
        return activityVO(activity, true);
    }

    public void activityUpdate(Long id, Map<String, Object> body) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String title = Params.str(body, "title");
        if (title != null) {
            activity.setTitle(title);
        }
        if (body.containsKey("cover")) {
            activity.setCover(stripStaticBase(Params.str(body, "cover")));
        }
        if (body.containsKey("content")) {
            activity.setContent(Params.str(body, "content"));
        }
        if (body.containsKey("startTime")) {
            activity.setStartTime(Params.dateTime(body, "startTime"));
        }
        if (body.containsKey("endTime")) {
            activity.setEndTime(Params.dateTime(body, "endTime"));
        }
        Integer sort = Params.integer(body, "sort");
        if (sort != null) {
            activity.setSort(sort);
        }
        activityMapper.updateById(activity);
    }

    /** 活动上/下线（18.6.12） */
    public void activityStatus(Long id, String status) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if ("ONLINE".equals(status)) {
            activity.setStatus(1);
        } else if ("OFFLINE".equals(status)) {
            activity.setStatus(0);
        } else {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 须为 ONLINE/OFFLINE");
        }
        activityMapper.updateById(activity);
    }

    public void activityDelete(Long id) {
        if (activityMapper.deleteById(id) == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
    }

    /** 通用图片上传（18.6.14）：jpg/png ≤5MB，banner 桶（MinIO 不可用时落盘本地） */
    public Map<String, Object> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_INVALID, "文件不能为空");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!IMAGE_EXT.contains(ext)) {
            throw new BizException(ErrorCode.FILE_INVALID, "仅支持 jpg/png 图片");
        }
        String objectKey = "image/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        minioUtil.upload("banner", objectKey, file);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("objectKey", objectKey);
        data.put("url", minioUtil.presignedGetUrl("banner", objectKey));
        return data;
    }

    /** 前端可能回传完整降级 URL，仅保留桶内相对路径 */
    private String stripStaticBase(String path) {
        if (path == null) {
            return null;
        }
        int idx = path.indexOf("/static/");
        if (idx >= 0) {
            String rest = path.substring(idx + "/static/".length());
            int slash = rest.indexOf('/');
            return slash >= 0 ? rest.substring(slash + 1) : rest;
        }
        return path;
    }
}
