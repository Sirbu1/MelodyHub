package cn.edu.seig.vibemusic.model.dto;

import lombok.Data;

/**
 * 论坛帖子查询DTO
 */
@Data
public class ForumPostDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 搜索关键词（标题）
     */
    private String keyword;

    /**
     * 帖子类型：0-交流，1-需求，null-全部
     */
    private Integer type;

}

