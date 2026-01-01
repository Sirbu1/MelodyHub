package cn.edu.seig.vibemusic.service;

import cn.edu.seig.vibemusic.model.dto.ForumPostAddDTO;
import cn.edu.seig.vibemusic.model.dto.ForumPostDTO;
import cn.edu.seig.vibemusic.model.entity.ForumPost;
import cn.edu.seig.vibemusic.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 论坛帖子 服务类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
public interface IForumPostService extends IService<ForumPost> {

    /**
     * 分页查询帖子列表
     *
     * @param forumPostDTO 查询条件
     * @return 帖子列表
     */
    Result getPostList(ForumPostDTO forumPostDTO);

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    Result getPostDetail(Long postId);

    /**
     * 发布帖子
     *
     * @param forumPostAddDTO 帖子信息
     * @param referenceAttachmentFile 参考附件文件（可选，仅需求时使用）
     * @return 结果
     */
    Result addPost(ForumPostAddDTO forumPostAddDTO, MultipartFile referenceAttachmentFile);

    /**
     * 删除帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    Result deletePost(Long postId);

    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    Result likePost(Long postId);

    /**
     * 取消点赞帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    Result cancelLikePost(Long postId);

    /**
     * 获取用户帖子列表（按状态筛选）
     *
     * @param userId      用户ID
     * @param pageNum     页码
     * @param pageSize    页大小
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 帖子列表
     */
    Result getUserPosts(Long userId, Integer pageNum, Integer pageSize, Integer auditStatus);

    /**
     * 更新需求帖子的接单状态（仅需求发布者可以操作）
     *
     * @param postId     帖子ID
     * @param isAccepted 接单状态：0-未接单，1-已接单
     * @return 结果
     */
    Result updateAcceptStatus(Long postId, Integer isAccepted);

    /**
     * 更新帖子并重新提交审核
     *
     * @param forumPostAddDTO 帖子信息（包含postId）
     * @param referenceAttachmentFile 参考附件文件（可选）
     * @return 结果
     */
    Result updatePost(ForumPostAddDTO forumPostAddDTO, MultipartFile referenceAttachmentFile);

}

