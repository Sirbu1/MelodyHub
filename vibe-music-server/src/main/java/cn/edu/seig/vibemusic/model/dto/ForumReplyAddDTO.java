package cn.edu.seig.vibemusic.model.dto;

import cn.edu.seig.vibemusic.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增论坛回复DTO
 */
@Data
public class ForumReplyAddDTO {

    /**
     * 帖子ID
     */
    @NotNull(message = "帖子ID" + MessageConstant.NOT_NULL)
    private Long postId;

    /**
     * 回复内容
     */
    @NotBlank(message = "回复内容" + MessageConstant.NOT_NULL)
    @Size(max = 2000, message = "回复内容长度不能超过2000字")
    private String content;

    /**
     * 父回复ID（用于楼中楼，可为空）
     */
    private Long parentId;

    /**
     * 回复ID（更新时使用）
     */
    private Long replyId;

}

