package cn.edu.seig.vibemusic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛接单VO
 */
@Data
public class ForumOrderVO {
    /**
     * 接单ID
     */
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 帖子标题
     */
    private String postTitle;

    /**
     * 需求发布者ID
     */
    private Long posterId;

    /**
     * 需求发布者用户名
     */
    private String posterName;

    /**
     * 需求发布者头像
     */
    private String posterAvatar;

    /**
     * 接单者ID
     */
    private Long accepterId;

    /**
     * 接单者用户名
     */
    private String accepterName;

    /**
     * 接单者头像
     */
    private String accepterAvatar;

    /**
     * 状态：0-待同意，1-已接单未完成，2-已完成
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

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

