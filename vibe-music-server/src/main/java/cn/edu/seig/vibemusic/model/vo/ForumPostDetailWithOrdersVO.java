package cn.edu.seig.vibemusic.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 论坛帖子详情（包含接单申请列表）VO
 */
@Data
public class ForumPostDetailWithOrdersVO {
    /**
     * 帖子详情
     */
    private ForumPostDetailVO postDetail;

    /**
     * 接单申请列表（仅需求发布者可见）
     */
    private List<ForumOrderVO> orderApplications;

    /**
     * 当前用户是否已申请接单
     */
    private Boolean hasApplied;

    /**
     * 当前用户的接单信息（如果是接单者）
     */
    private ForumOrderVO myOrder;
}

