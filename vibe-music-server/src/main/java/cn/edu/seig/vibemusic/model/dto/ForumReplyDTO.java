package cn.edu.seig.vibemusic.model.dto;

import lombok.Data;

/**
 * 论坛回复查询DTO
 */
@Data
public class ForumReplyDTO {

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

}

