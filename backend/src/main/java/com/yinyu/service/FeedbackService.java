package com.yinyu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinyu.common.constant.ErrorCode;
import com.yinyu.common.exception.BizException;
import com.yinyu.common.result.PageResult;
import com.yinyu.dto.FeedbackRequest;
import com.yinyu.entity.Feedback;
import com.yinyu.entity.User;
import com.yinyu.mapper.FeedbackMapper;
import com.yinyu.mapper.UserMapper;
import com.yinyu.util.MinioUtil;
import com.yinyu.util.Params;
import com.yinyu.util.SensitiveWordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户反馈：门户提交/我的列表（api.md 17.x）+ 后台处理（18.5.9/18.5.10）
 * feedback.status：0-待处理(PENDING) 1-已回复(REPLIED) 2-已关闭(CLOSED)
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final Map<String, Integer> TYPE_CODE = Map.of(
            "BUG", 1, "SUGGEST", 2, "COPYRIGHT", 3, "OTHER", 4);
    private static final String[] TYPE_NAMES = {"", "BUG", "SUGGEST", "COPYRIGHT", "OTHER"};
    private static final String[] STATUS_NAMES = {"PENDING", "REPLIED", "CLOSED"};

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final MinioUtil minioUtil;

    /** 提交反馈（17.1）：敏感词 40001 */
    public Map<String, Object> create(FeedbackRequest req, Long userId) {
        Integer type = TYPE_CODE.get(req.getType());
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: type 须为 BUG/SUGGEST/COPYRIGHT/OTHER");
        }
        SensitiveWordUtil.check(req.getContent());
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setContent(req.getContent());
        feedback.setImages(req.getImages() == null || req.getImages().isEmpty() ? null
                : String.join(",", req.getImages()));
        feedback.setContact(req.getContact());
        feedback.setStatus(0);
        feedbackMapper.insert(feedback);
        return toVO(feedback);
    }

    /** 我的反馈（17.2） */
    public PageResult<Map<String, Object>> myPage(Long userId, long pageNum, long pageSize) {
        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, userId)
                        .orderByDesc(Feedback::getCreateTime));
        return PageResult.from(page, list -> list.stream().map(this::toVO).toList());
    }

    private Map<String, Object> toVO(Feedback f) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", f.getId());
        vo.put("type", TYPE_NAMES[Math.min(f.getType() == null ? 4 : f.getType(), 4)]);
        vo.put("content", f.getContent());
        vo.put("images", f.getImages() == null ? List.of()
                : Arrays.stream(f.getImages().split(","))
                        .map(minioUtil::publicImageUrl).toList());
        vo.put("contact", f.getContact());
        int status = f.getStatus() == null ? 0 : Math.min(f.getStatus(), 2);
        vo.put("status", STATUS_NAMES[status]);
        vo.put("reply", f.getReply());
        vo.put("replyTime", f.getHandleTime());
        vo.put("createTime", f.getCreateTime());
        return vo;
    }

    // ---------------- 后台（api.md 18.5.9/18.5.10） ----------------

    /** 反馈分页（type/status 筛选） */
    public PageResult<Map<String, Object>> adminPage(long pageNum, long pageSize,
                                                     String type, String status) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .orderByAsc(Feedback::getStatus)
                .orderByDesc(Feedback::getCreateTime);
        if (type != null && !type.isEmpty()) {
            Integer typeCode = TYPE_CODE.get(type);
            if (typeCode == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: type 不合法");
            }
            wrapper.eq(Feedback::getType, typeCode);
        }
        if (status != null && !status.isEmpty()) {
            int statusCode = switch (status) {
                case "PENDING" -> 0;
                case "REPLIED" -> 1;
                case "CLOSED" -> 2;
                default -> throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 不合法");
            };
            wrapper.eq(Feedback::getStatus, statusCode);
        }
        Page<Feedback> page = feedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Long> userIds = page.getRecords().stream().map(Feedback::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        return PageResult.from(page, list -> list.stream().map(f -> {
            Map<String, Object> vo = toVO(f);
            User user = users.get(f.getUserId());
            vo.put("userId", f.getUserId());
            vo.put("username", user == null ? null : user.getUsername());
            vo.put("nickname", user == null ? null : user.getNickname());
            return vo;
        }).toList());
    }

    /** 回复 / 关闭（18.5.10）：body 传 reply 或 status=CLOSED */
    public void handle(Long id, Map<String, Object> body, Long adminId) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String reply = Params.str(body, "reply");
        String status = Params.str(body, "status");
        if (reply == null && status == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: reply 与 status 至少传一项");
        }
        if (reply != null) {
            feedback.setReply(reply);
            feedback.setStatus(1);
        }
        if ("CLOSED".equals(status)) {
            feedback.setStatus(2);
        } else if (status != null && !"REPLIED".equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数校验失败: status 须为 REPLIED/CLOSED");
        }
        feedback.setHandleAdminId(adminId);
        feedback.setHandleTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
    }
}
