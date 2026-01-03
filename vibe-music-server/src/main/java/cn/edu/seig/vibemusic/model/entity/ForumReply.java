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
 * 论坛回复实体类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_forum_reply")
public class ForumReply implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 回复ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long replyId;

    /**
     * 帖子ID
     */
    @TableField("post_id")
    private Long postId;

    /**
     * 回复用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 回复内容
     */
    @TableField("content")
    private String content;

    /**
     * 父回复ID（用于楼中楼）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 点赞数量
     */
    @TableField("like_count")
    private Long likeCount;

    /**
     * 状态：0-正常，1-已删除
     */
    @TableField("status")
    private Integer status;

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

}

