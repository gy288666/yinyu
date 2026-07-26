package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.dto.CommentRequest;
import com.yinyu.entity.Comment;
import com.yinyu.entity.CommentLike;
import com.yinyu.entity.Playlist;
import com.yinyu.entity.Song;
import com.yinyu.entity.User;
import com.yinyu.mapper.CommentLikeMapper;
import com.yinyu.mapper.CommentMapper;
import com.yinyu.mapper.PlaylistMapper;
import com.yinyu.mapper.SongMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.util.SensitiveWordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论：列表（一级 + 回复列表）/发表/删除/点赞（api.md 13.x）
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Map<String, Integer> TARGET_TYPE = Map.of(
            "SONG", 1, "PLAYLIST", 2, "ALBUM", 3);

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final SongMapper songMapper;
    private final PlaylistMapper playlistMapper;
    private final MinioUtil minioUtil;

    private int targetTypeCode(String targetType) {
        Integer code = TARGET_TYPE.get(targetType);
        if (code == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: targetType 须为 SONG/PLAYLIST/ALBUM");
        }
        return code;
    }

    /** 评论分页（api.md 13.1）：一级评论分页，回复整体挂在 replies */
    public PageResult<Map<String, Object>> page(String targetType, Long targetId, String sort,
                                                long pageNum, long pageSize, Long currentUserId) {
        int typeCode = targetTypeCode(targetType);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, typeCode)
                .eq(Comment::getTargetId, targetId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1);
        if ("latest".equals(sort)) {
            wrapper.orderByDesc(Comment::getCreateTime);
        } else {
            wrapper.orderByDesc(Comment::getLikeCount).orderByDesc(Comment::getCreateTime);
        }
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Comment> roots = page.getRecords();

        // 回复：按根评论批量取
        Map<Long, List<Comment>> replyMap = Map.of();
        if (!roots.isEmpty()) {
            replyMap = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                            .in(Comment::getParentId, roots.stream().map(Comment::getId).toList())
                            .eq(Comment::getStatus, 1)
                            .orderByAsc(Comment::getCreateTime))
                    .stream().collect(Collectors.groupingBy(Comment::getParentId));
        }
        // 用户信息批量取
        Set<Long> userIds = new java.util.HashSet<>();
        roots.forEach(c -> userIds.add(c.getUserId()));
        replyMap.values().forEach(list -> list.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyUserId() != null) {
                userIds.add(c.getReplyUserId());
            }
        }));
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        // 当前用户点赞集合
        Set<Long> likedIds = Set.of();
        if (currentUserId != null && !roots.isEmpty()) {
            List<Long> allIds = new ArrayList<>(roots.stream().map(Comment::getId).toList());
            replyMap.values().forEach(l -> l.forEach(c -> allIds.add(c.getId())));
            likedIds = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                            .eq(CommentLike::getUserId, currentUserId)
                            .in(CommentLike::getCommentId, allIds))
                    .stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }

        Map<Long, List<Comment>> finalReplyMap = replyMap;
        Set<Long> finalLikedIds = likedIds;
        return PageResult.from(page, list -> list.stream().map(root -> {
            Map<String, Object> item = toVO(root, users, finalLikedIds);
            item.put("replies", finalReplyMap.getOrDefault(root.getId(), List.of()).stream()
                    .map(reply -> {
                        Map<String, Object> r = toVO(reply, users, finalLikedIds);
                        User replyTo = reply.getReplyUserId() == null ? null : users.get(reply.getReplyUserId());
                        r.put("replyTo", replyTo == null ? null : replyTo.getNickname());
                        return r;
                    }).toList());
            return item;
        }).toList());
    }

    private Map<String, Object> toVO(Comment c, Map<Long, User> users, Set<Long> likedIds) {
        User user = users.get(c.getUserId());
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", c.getId());
        vo.put("userId", c.getUserId());
        vo.put("nickname", user == null ? "已注销用户" : user.getNickname());
        vo.put("avatar", user == null ? null : minioUtil.publicImageUrl(user.getAvatar()));
        vo.put("level", user == null ? null : user.getLevelId());
        vo.put("vip", user != null && AuthService.isVipActive(user));
        vo.put("content", c.getContent());
        vo.put("likeCount", c.getLikeCount() == null ? 0 : c.getLikeCount());
        vo.put("liked", likedIds.contains(c.getId()));
        vo.put("createTime", c.getCreateTime());
        return vo;
    }

    /** 发表评论/回复（api.md 13.2）：敏感词 40001 */
    @Transactional
    public Map<String, Object> create(CommentRequest req, Long userId) {
        int typeCode = targetTypeCode(req.getTargetType());
        SensitiveWordUtil.check(req.getContent());
        checkTargetExists(typeCode, req.getTargetId());

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetType(typeCode);
        comment.setTargetId(req.getTargetId());
        comment.setContent(req.getContent());
        comment.setLikeCount(0);
        comment.setStatus(1);
        if (req.getParentId() != null && req.getParentId() > 0) {
            Comment parent = commentMapper.selectById(req.getParentId());
            if (parent == null || parent.getStatus() == null || parent.getStatus() != 1) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "被回复的评论不存在");
            }
            // 楼层简化为一级 + 回复列表：回复的回复也挂到根评论下
            comment.setParentId(parent.getParentId() != null && parent.getParentId() > 0
                    ? parent.getParentId() : parent.getId());
            comment.setReplyUserId(parent.getUserId());
        } else {
            comment.setParentId(0L);
        }
        commentMapper.insert(comment);
        adjustCommentCount(typeCode, req.getTargetId(), 1);

        User user = userMapper.selectById(userId);
        Map<String, Object> vo = toVO(comment, user == null ? Map.of() : Map.of(userId, user), Set.of());
        if (comment.getParentId() > 0) {
            vo.put("parentId", comment.getParentId());
        }
        return vo;
    }

    /** 删除评论（api.md 13.3）：本人或管理员；根评论级联删除回复 */
    @Transactional
    public void delete(Long id, Long userId, boolean admin) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        if (!admin && !comment.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无操作权限");
        }
        long removed = 1;
        if (comment.getParentId() == null || comment.getParentId() == 0) {
            List<Comment> replies = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
            for (Comment reply : replies) {
                commentMapper.deleteById(reply.getId());
            }
            removed += replies.size();
        }
        commentMapper.deleteById(id);
        adjustCommentCount(comment.getTargetType(), comment.getTargetId(), -removed);
    }

    /** 点赞（api.md 13.4）：幂等 */
    @Transactional
    public Map<String, Object> like(Long id, Long userId) {
        Comment comment = requireComment(id);
        Long exists = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, id));
        if (exists == 0) {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(id);
            commentLikeMapper.insert(like);
            comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
            commentMapper.updateById(comment);
        }
        return Map.of("likeCount", comment.getLikeCount() == null ? 0 : comment.getLikeCount(), "liked", true);
    }

    /** 取消点赞（api.md 13.4）：幂等 */
    @Transactional
    public Map<String, Object> unlike(Long id, Long userId) {
        Comment comment = requireComment(id);
        int deleted = commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, id));
        if (deleted > 0) {
            comment.setLikeCount(Math.max(0,
                    (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
            commentMapper.updateById(comment);
        }
        return Map.of("likeCount", comment.getLikeCount() == null ? 0 : comment.getLikeCount(), "liked", false);
    }

    private Comment requireComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null || comment.getStatus() == null || comment.getStatus() != 1) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        return comment;
    }

    private void checkTargetExists(int typeCode, Long targetId) {
        boolean exists = switch (typeCode) {
            case 1 -> {
                Song song = songMapper.selectById(targetId);
                yield song != null && song.getAuditStatus() != null && song.getAuditStatus() == 1;
            }
            case 2 -> {
                Playlist playlist = playlistMapper.selectById(targetId);
                yield playlist != null && playlist.getStatus() != null && playlist.getStatus() == 1;
            }
            default -> true;
        };
        if (!exists) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "评论目标不存在");
        }
    }

    /** 维护 song.comment_count 冗余计数 */
    private void adjustCommentCount(Integer typeCode, Long targetId, long delta) {
        if (typeCode == null || typeCode != 1) {
            return;
        }
        Song song = songMapper.selectById(targetId);
        if (song != null) {
            song.setCommentCount(Math.max(0,
                    (song.getCommentCount() == null ? 0 : song.getCommentCount()) + delta));
            songMapper.updateById(song);
        }
    }
}
