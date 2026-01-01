package cn.edu.seig.vibemusic.model.dto;

import cn.edu.seig.vibemusic.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增论坛帖子DTO
 */
@Data
public class ForumPostAddDTO {

    /**
     * 帖子标题
     */
    @NotBlank(message = "标题" + MessageConstant.NOT_NULL)
    @Size(max = 200, message = "标题长度不能超过200字")
    private String title;

    /**
     * 帖子内容
     */
    @NotBlank(message = "内容" + MessageConstant.NOT_NULL)
    @Size(max = 10000, message = "内容长度不能超过10000字")
    private String content;

    /**
     * 帖子类型：0-交流，1-需求
     */
    private Integer type;

    /**
     * 需求类型（如：作曲、编曲、混音、作词等）
     */
    @Size(max = 50, message = "需求类型长度不能超过50字")
    private String requirementType;

    /**
     * 时间要求
     */
    @Size(max = 100, message = "时间要求长度不能超过100字")
    private String timeRequirement;

    /**
     * 预算
     */
    @Size(max = 50, message = "预算长度不能超过50字")
    private String budget;

    /**
     * 风格描述
     */
    @Size(max = 2000, message = "风格描述长度不能超过2000字")
    private String styleDescription;

    /**
     * 参考附件URL（由后端上传后返回）
     */
    private String referenceAttachment;

    /**
     * 帖子ID（更新时使用）
     */
    private Long postId;

}

