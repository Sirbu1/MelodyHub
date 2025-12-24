package cn.edu.seig.vibemusic.mapper;

import cn.edu.seig.vibemusic.model.entity.ForumReply;
import cn.edu.seig.vibemusic.model.vo.ForumReplyVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 论坛回复 Mapper 接口
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Mapper
public interface ForumReplyMapper extends BaseMapper<ForumReply> {

    /**
     * 分页查询帖子的回复列表（只查询一级回复）
     *
     * @param page   分页对象
     * @param postId 帖子ID
     * @return 回复列表
     */
    IPage<ForumReplyVO> selectReplyPage(Page<ForumReplyVO> page, @Param("postId") Long postId);

    /**
     * 查询某回复的子回复列表
     *
     * @param parentId 父回复ID
     * @return 子回复列表
     */
    List<ForumReplyVO> selectChildReplies(@Param("parentId") Long parentId);

    /**
     * 查询回复详情（包含父回复用户名）
     *
     * @param replyId 回复ID
     * @return 回复详情
     */
    ForumReplyVO selectReplyDetail(@Param("replyId") Long replyId);

    /**
     * 获取用户回复列表（按状态筛选）
     *
     * @param page        分页对象
     * @param userId      用户ID
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 回复列表
     */
    IPage<ForumReplyVO> getUserReplies(Page<ForumReplyVO> page, @Param("userId") Long userId, @Param("auditStatus") Integer auditStatus);

}

