package cn.edu.seig.vibemusic.service;

import cn.edu.seig.vibemusic.model.dto.ForumReplyAddDTO;
import cn.edu.seig.vibemusic.model.dto.ForumReplyDTO;
import cn.edu.seig.vibemusic.model.entity.ForumReply;
import cn.edu.seig.vibemusic.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 论坛回复 服务类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
public interface IForumReplyService extends IService<ForumReply> {

    /**
     * 分页查询帖子的回复列表
     *
     * @param forumReplyDTO 查询条件
     * @return 回复列表
     */
    Result getReplyList(ForumReplyDTO forumReplyDTO);

    /**
     * 发布回复
     *
     * @param forumReplyAddDTO 回复信息
     * @return 结果
     */
    Result addReply(ForumReplyAddDTO forumReplyAddDTO);

    /**
     * 删除回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    Result deleteReply(Long replyId);

    /**
     * 点赞回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    Result likeReply(Long replyId);

    /**
     * 取消点赞回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    Result cancelLikeReply(Long replyId);

    /**
     * 获取用户回复列表（按状态筛选）
     *
     * @param userId      用户ID
     * @param pageNum     页码
     * @param pageSize    页大小
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 回复列表
     */
    Result getUserReplies(Long userId, Integer pageNum, Integer pageSize, Integer auditStatus);

}

