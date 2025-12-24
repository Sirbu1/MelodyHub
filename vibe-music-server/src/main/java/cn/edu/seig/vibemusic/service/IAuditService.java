package cn.edu.seig.vibemusic.service;

import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;

/**
 * 审核服务接口
 */
public interface IAuditService {

    /**
     * 审核通过歌曲
     *
     * @param songId 歌曲ID
     * @return 结果
     */
    Result approveSong(Long songId);

    /**
     * 审核拒绝歌曲
     *
     * @param songId 歌曲ID
     * @return 结果
     */
    Result rejectSong(Long songId);

    /**
     * 审核通过帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    Result approvePost(Long postId);

    /**
     * 审核拒绝帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    Result rejectPost(Long postId);

    /**
     * 审核通过回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    Result approveReply(Long replyId);

    /**
     * 审核拒绝回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    Result rejectReply(Long replyId);

    /**
     * 审核通过评论
     *
     * @param commentId 评论ID
     * @return 结果
     */
    Result approveComment(Long commentId);

    /**
     * 审核拒绝评论
     *
     * @param commentId 评论ID
     * @return 结果
     */
    Result rejectComment(Long commentId);

    /**
     * 获取待审核歌曲列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 待审核歌曲列表
     */
    Result<PageResult> getPendingSongs(Integer pageNum, Integer pageSize);

    /**
     * 获取待审核帖子列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 待审核帖子列表
     */
    Result<PageResult> getPendingPosts(Integer pageNum, Integer pageSize);

    /**
     * 获取待审核回复列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 待审核回复列表
     */
    Result<PageResult> getPendingReplies(Integer pageNum, Integer pageSize);

    /**
     * 获取待审核评论列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 待审核评论列表
     */
    Result<PageResult> getPendingComments(Integer pageNum, Integer pageSize);
}

