package cn.edu.seig.vibemusic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛回复VO
 */
@Data
public class ForumReplyVO {

    /**
     * 回复ID
     */
    private Long replyId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 回复用户ID
     */
    private Long userId;

    /**
     * 回复用户名
     */
    private String username;

    /**
     * 回复用户头像
     */
    private String userAvatar;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 父回复ID
     */
    private Long parentId;

    /**
     * 父回复用户名（用于显示@某人）
     */
    private String parentUsername;

    /**
     * 点赞数量
     */
    private Long likeCount;

    /**
     * 审核状态：0-待审核，1-已通过，2-未通过
     */
    private Integer auditStatus;

    /**
     * 审核未通过原因
     */
    private String auditReason;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 子回复列表（楼中楼）
     */
    private List<ForumReplyVO> children;

}

