package cn.edu.seig.vibemusic.mapper;

import cn.edu.seig.vibemusic.model.entity.ForumPost;
import cn.edu.seig.vibemusic.model.vo.ForumPostDetailVO;
import cn.edu.seig.vibemusic.model.vo.ForumPostVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 论坛帖子 Mapper 接口
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {

    /**
     * 分页查询帖子列表
     *
     * @param page    分页对象
     * @param keyword 搜索关键词
     * @param type    帖子类型：0-交流，1-需求，null-全部
     * @return 帖子列表
     */
    IPage<ForumPostVO> selectPostPage(Page<ForumPostVO> page, @Param("keyword") String keyword, @Param("type") Integer type);

    /**
     * 查询帖子详情
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID（可选，如果提供则允许查看自己的待审核帖子）
     * @return 帖子详情
     */
    ForumPostDetailVO selectPostDetail(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 管理员查询帖子详情（可以查看所有状态的帖子）
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    ForumPostDetailVO selectPostDetailForAdmin(@Param("postId") Long postId);

    /**
     * 增加浏览次数
     *
     * @param postId 帖子ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 增加回复数量
     *
     * @param postId 帖子ID
     * @return 影响行数
     */
    int incrementReplyCount(@Param("postId") Long postId);

    /**
     * 减少回复数量
     *
     * @param postId 帖子ID
     * @return 影响行数
     */
    int decrementReplyCount(@Param("postId") Long postId);

    /**
     * 获取用户帖子列表（按状态筛选）
     *
     * @param page        分页对象
     * @param userId      用户ID
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 帖子列表
     */
    IPage<ForumPostVO> getUserPosts(Page<ForumPostVO> page, @Param("userId") Long userId, @Param("auditStatus") Integer auditStatus);

    /**
     * 统计用户当日发布帖子数量
     *
     * @param userId 用户ID
     * @return 当日发布帖子数量
     */
    Integer countUserTodayPosts(@Param("userId") Long userId);

}

