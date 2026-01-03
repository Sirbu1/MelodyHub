package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.mapper.ForumPostMapper;
import cn.edu.seig.vibemusic.mapper.ForumReplyMapper;
import cn.edu.seig.vibemusic.mapper.UserMapper;
import cn.edu.seig.vibemusic.model.entity.User;
import cn.edu.seig.vibemusic.model.dto.ForumReplyAddDTO;
import cn.edu.seig.vibemusic.model.dto.ForumReplyDTO;
import cn.edu.seig.vibemusic.model.entity.ForumPost;
import cn.edu.seig.vibemusic.model.entity.ForumReply;
import cn.edu.seig.vibemusic.model.vo.ForumReplyVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IForumReplyService;
import cn.edu.seig.vibemusic.util.ThreadLocalUtil;
import cn.edu.seig.vibemusic.util.TypeConversionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 论坛回复 服务实现类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Slf4j
@Service
public class ForumReplyServiceImpl extends ServiceImpl<ForumReplyMapper, ForumReply> implements IForumReplyService {

    @Autowired
    private ForumReplyMapper forumReplyMapper;

    @Autowired
    private ForumPostMapper forumPostMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询帖子的回复列表
     *
     * @param forumReplyDTO 查询条件
     * @return 回复列表
     */
    @Override
    public Result getReplyList(ForumReplyDTO forumReplyDTO) {
        Page<ForumReplyVO> page = new Page<>(forumReplyDTO.getPageNum(), forumReplyDTO.getPageSize());
        IPage<ForumReplyVO> replyPage = forumReplyMapper.selectReplyPage(page, forumReplyDTO.getPostId());

        // 查询每个一级回复的子回复
        List<ForumReplyVO> replies = replyPage.getRecords();
        for (ForumReplyVO reply : replies) {
            List<ForumReplyVO> children = forumReplyMapper.selectChildReplies(reply.getReplyId());
            reply.setChildren(children);
        }

        PageResult<ForumReplyVO> pageResult = new PageResult<>();
        pageResult.setTotal(replyPage.getTotal());
        pageResult.setItems(replies);

        return Result.success(pageResult);
    }

    /**
     * 发布回复
     *
     * @param forumReplyAddDTO 回复信息
     * @return 结果
     */
    @Override
    @Transactional
    public Result addReply(ForumReplyAddDTO forumReplyAddDTO) {
        // 检查帖子是否存在
        ForumPost forumPost = forumPostMapper.selectById(forumReplyAddDTO.getPostId());
        if (forumPost == null || forumPost.getStatus() == 1) {
            return Result.error("帖子" + MessageConstant.NOT_EXIST);
        }

        // 如果有父回复，检查父回复是否存在
        if (forumReplyAddDTO.getParentId() != null) {
            ForumReply parentReply = forumReplyMapper.selectById(forumReplyAddDTO.getParentId());
            if (parentReply == null || parentReply.getStatus() == 1) {
                return Result.error("回复" + MessageConstant.NOT_EXIST);
            }
        }

        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        // 检查用户积分是否大于0
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        int userScore = user.getScore() != null ? user.getScore() : 100;
        if (userScore <= 0) {
            return Result.error("当前账号无发布权限，积分不足（积分为0时无法发帖、发歌、回复）");
        }

        ForumReply forumReply = new ForumReply();
        forumReply.setPostId(forumReplyAddDTO.getPostId())
                .setUserId(userId)
                .setContent(forumReplyAddDTO.getContent())
                .setParentId(forumReplyAddDTO.getParentId())
                .setLikeCount(0L)
                .setStatus(0)
                .setAuditStatus(0) // 设置审核状态为待审核
                .setCreateTime(LocalDateTime.now());

        if (forumReplyMapper.insert(forumReply) == 0) {
            return Result.error(MessageConstant.ADD + MessageConstant.FAILED);
        }

        // 增加帖子回复数
        forumPostMapper.incrementReplyCount(forumReplyAddDTO.getPostId());

        return Result.success(MessageConstant.ADD + MessageConstant.SUCCESS);
    }

    /**
     * 删除回复（软删除）
     *
     * @param replyId 回复ID
     * @return 结果
     */
    @Override
    @Transactional
    public Result deleteReply(Long replyId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        ForumReply forumReply = forumReplyMapper.selectById(replyId);
        if (forumReply == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 检查是否是回复作者
        if (!Objects.equals(forumReply.getUserId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }

        // 软删除
        forumReply.setStatus(1);

        if (forumReplyMapper.updateById(forumReply) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }

        // 减少帖子回复数
        forumPostMapper.decrementReplyCount(forumReply.getPostId());

        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

    /**
     * 点赞回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    @Override
    public Result likeReply(Long replyId) {
        ForumReply forumReply = forumReplyMapper.selectById(replyId);
        if (forumReply == null || forumReply.getStatus() == 1) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        forumReply.setLikeCount(forumReply.getLikeCount() + 1);

        if (forumReplyMapper.updateById(forumReply) == 0) {
            return Result.error(MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.SUCCESS);
    }

    /**
     * 取消点赞回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    @Override
    public Result cancelLikeReply(Long replyId) {
        ForumReply forumReply = forumReplyMapper.selectById(replyId);
        if (forumReply == null || forumReply.getStatus() == 1) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        if (forumReply.getLikeCount() > 0) {
            forumReply.setLikeCount(forumReply.getLikeCount() - 1);
        }

        if (forumReplyMapper.updateById(forumReply) == 0) {
            return Result.error(MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.SUCCESS);
    }

    /**
     * 获取用户回复列表（按状态筛选）
     *
     * @param userId      用户ID
     * @param pageNum     页码
     * @param pageSize    页大小
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 回复列表
     */
    @Override
    public Result getUserReplies(Long userId, Integer pageNum, Integer pageSize, Integer auditStatus) {
        Page<ForumReplyVO> page = new Page<>(pageNum, pageSize);
        IPage<ForumReplyVO> replyPage = forumReplyMapper.getUserReplies(page, userId, auditStatus);

        PageResult<ForumReplyVO> pageResult = new PageResult<>();
        pageResult.setTotal(replyPage.getTotal());
        pageResult.setItems(replyPage.getRecords());

        return Result.success(pageResult);
    }

    /**
     * 更新回复并重新提交审核
     *
     * @param forumReplyAddDTO 回复信息（包含replyId）
     * @return 结果
     */
    @Override
    @Transactional
    public Result updateReply(ForumReplyAddDTO forumReplyAddDTO) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map == null) {
            return Result.error("用户未登录");
        }
        
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);
        
        // 检查replyId是否存在
        if (forumReplyAddDTO.getReplyId() == null) {
            return Result.error("回复ID不能为空");
        }
        
        // 查询回复信息
        ForumReply forumReply = forumReplyMapper.selectById(forumReplyAddDTO.getReplyId());
        if (forumReply == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        
        // 检查是否是回复作者
        if (!Objects.equals(forumReply.getUserId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }
        
        try {
            // 更新回复内容
            if (forumReplyAddDTO.getContent() != null && !forumReplyAddDTO.getContent().trim().isEmpty()) {
                forumReply.setContent(forumReplyAddDTO.getContent().trim());
            }
            
            // 重置审核状态为待审核
            forumReply.setAuditStatus(0);
            
            // 更新数据库
            if (forumReplyMapper.updateById(forumReply) == 0) {
                return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
            }
            
            return Result.success("回复更新成功，已重新提交审核");
        } catch (Exception e) {
            log.error("更新回复失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

}

