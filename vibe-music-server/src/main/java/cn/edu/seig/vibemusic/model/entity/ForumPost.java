package cn.edu.seig.vibemusic.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 论坛帖子实体类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_forum_post")
public class ForumPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long postId;

    /**
     * 发帖用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 帖子标题
     */
    @TableField("title")
    private String title;

    /**
     * 帖子内容
     */
    @TableField("content")
    private String content;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Long viewCount;

    /**
     * 回复数量
     */
    @TableField("reply_count")
    private Long replyCount;

    /**
     * 点赞数量
     */
    @TableField("like_count")
    private Long likeCount;

    /**
     * 是否置顶：0-否，1-是
     */
    @TableField("is_top")
    private Integer isTop;

    /**
     * 状态：0-正常，1-已删除
     */
    @TableField("status")
    private Integer status;

    /**
     * 帖子类型：0-交流，1-需求
     */
    @TableField("type")
    private Integer type;

    /**
     * 需求类型（如：作曲、编曲、混音、作词等）
     */
    @TableField("requirement_type")
    private String requirementType;

    /**
     * 时间要求
     */
    @TableField("time_requirement")
    private String timeRequirement;

    /**
     * 预算
     */
    @TableField("budget")
    private String budget;

    /**
     * 风格描述
     */
    @TableField("style_description")
    private String styleDescription;

    /**
     * 参考附件URL
     */
    @TableField("reference_attachment")
    private String referenceAttachment;

    /**
     * 是否已接单：0-未接单，1-已接单（仅需求类型使用）
     */
    @TableField("is_accepted")
    private Integer isAccepted;

    /**
     * 审核状态：0-待审核，1-已通过，2-未通过
     */
    @TableField("audit_status")
    private Integer auditStatus;

    /**
     * 审核未通过原因
     */
    @TableField("audit_reason")
    private String auditReason;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

}

