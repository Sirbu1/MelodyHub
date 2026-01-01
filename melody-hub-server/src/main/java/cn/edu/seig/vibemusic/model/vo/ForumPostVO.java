package cn.edu.seig.vibemusic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子列表VO
 */
@Data
public class ForumPostVO {

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 发帖用户ID
     */
    private Long userId;

    /**
     * 发帖用户名
     */
    private String username;

    /**
     * 发帖用户头像
     */
    private String userAvatar;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容（列表显示时截取）
     */
    private String content;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 回复数量
     */
    private Long replyCount;

    /**
     * 点赞数量
     */
    private Long likeCount;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 帖子类型：0-交流，1-需求
     */
    private Integer type;

    /**
     * 需求类型（如：作曲、编曲、混音、作词等）
     */
    private String requirementType;

    /**
     * 时间要求
     */
    private String timeRequirement;

    /**
     * 预算
     */
    private String budget;

    /**
     * 风格描述
     */
    private String styleDescription;

    /**
     * 参考附件URL
     */
    private String referenceAttachment;

    /**
     * 是否已接单：0-未接单，1-已接单（仅需求类型使用）
     */
    private Integer isAccepted;

    /**
     * 审核状态：0-待审核，1-已通过，2-未通过
     */
    private Integer auditStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}

