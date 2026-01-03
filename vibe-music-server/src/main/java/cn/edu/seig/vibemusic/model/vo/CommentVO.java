package cn.edu.seig.vibemusic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论 id
     */
    private Long commentId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

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
     * 评论类型：0-歌曲评论，1-歌单评论
     */
    private Integer type;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 歌曲ID
     */
    private Long songId;

    /**
     * 歌单ID
     */
    private Long playlistId;

}
