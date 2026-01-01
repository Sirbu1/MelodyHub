package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.mapper.ForumPostMapper;
import cn.edu.seig.vibemusic.model.dto.ForumPostAddDTO;
import cn.edu.seig.vibemusic.model.dto.ForumPostDTO;
import cn.edu.seig.vibemusic.model.entity.ForumPost;
import cn.edu.seig.vibemusic.model.vo.ForumPostDetailVO;
import cn.edu.seig.vibemusic.model.vo.ForumPostVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IForumPostService;
import cn.edu.seig.vibemusic.service.MinioService;
import cn.edu.seig.vibemusic.util.ThreadLocalUtil;
import cn.edu.seig.vibemusic.util.TypeConversionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 论坛帖子 服务实现类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Slf4j
@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements IForumPostService {

    @Autowired
    private ForumPostMapper forumPostMapper;

    @Autowired
    private MinioService minioService;

    /**
     * 分页查询帖子列表
     *
     * @param forumPostDTO 查询条件
     * @return 帖子列表
     */
    @Override
    public Result getPostList(ForumPostDTO forumPostDTO) {
        Page<ForumPostVO> page = new Page<>(forumPostDTO.getPageNum(), forumPostDTO.getPageSize());
        IPage<ForumPostVO> postPage = forumPostMapper.selectPostPage(page, forumPostDTO.getKeyword(), forumPostDTO.getType());

        PageResult<ForumPostVO> pageResult = new PageResult<>();
        pageResult.setTotal(postPage.getTotal());
        pageResult.setItems(postPage.getRecords());

        return Result.success(pageResult);
    }

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    @Override
    public Result getPostDetail(Long postId) {
        // 获取当前登录用户ID和角色（如果已登录）
        Long currentUserId = null;
        String currentRole = null;
        try {
            Map<String, Object> map = ThreadLocalUtil.get();
            if (map != null) {
                Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
                if (userIdObj != null) {
                    currentUserId = TypeConversionUtil.toLong(userIdObj);
                }
                Object roleObj = map.get(JwtClaimsConstant.ROLE);
                if (roleObj != null) {
                    currentRole = roleObj.toString();
                }
            }
        } catch (Exception e) {
            // 如果获取用户ID失败，继续执行（允许未登录用户查看已审核的帖子）
            // currentUserId 保持为 null
        }
        
        // 如果是管理员，允许查看所有状态的帖子（包括待审核的）
        // 否则，如果是帖子所有者，允许查看自己的待审核帖子
        // 否则，只能查看已审核通过的帖子
        boolean isAdmin = "ROLE_ADMIN".equals(currentRole);
        Long userIdForQuery = (currentUserId != null && currentUserId > 0) ? currentUserId : 0L;
        
        ForumPostDetailVO postDetail;
        if (isAdmin) {
            // 管理员可以查看所有状态的帖子，直接查询，不限制audit_status
            postDetail = forumPostMapper.selectPostDetailForAdmin(postId);
        } else {
            postDetail = forumPostMapper.selectPostDetail(postId, userIdForQuery);
        }
        
        if (postDetail == null) {
            // 如果查询结果为空，可能是帖子不存在或用户无权查看
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 增加浏览次数（仅对已审核通过的帖子增加浏览次数）
        if (postDetail.getAuditStatus() != null && postDetail.getAuditStatus() == 1) {
            forumPostMapper.incrementViewCount(postId);
            postDetail.setViewCount(postDetail.getViewCount() + 1);
        }

        return Result.success(postDetail);
    }

    /**
     * 发布帖子
     *
     * @param forumPostAddDTO 帖子信息
     * @param referenceAttachmentFile 参考附件文件（可选，仅需求时使用）
     * @return 结果
     */
    @Override
    public Result addPost(ForumPostAddDTO forumPostAddDTO, MultipartFile referenceAttachmentFile) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        ForumPost forumPost = new ForumPost();
        forumPost.setUserId(userId)
                .setTitle(forumPostAddDTO.getTitle())
                .setContent(forumPostAddDTO.getContent())
                .setViewCount(0L)
                .setReplyCount(0L)
                .setLikeCount(0L)
                .setIsTop(0)
                .setStatus(0)
                .setAuditStatus(0) // 设置审核状态为待审核
                .setType(forumPostAddDTO.getType() != null ? forumPostAddDTO.getType() : 0) // 默认为交流类型
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        // 如果是需求类型，设置需求相关字段
        if (forumPostAddDTO.getType() != null && forumPostAddDTO.getType() == 1) {
            forumPost.setRequirementType(forumPostAddDTO.getRequirementType());
            forumPost.setTimeRequirement(forumPostAddDTO.getTimeRequirement());
            forumPost.setBudget(forumPostAddDTO.getBudget());
            forumPost.setStyleDescription(forumPostAddDTO.getStyleDescription());
            forumPost.setIsAccepted(0); // 默认未接单
        }

        // 上传参考附件（交流和需求都可以上传）
        if (referenceAttachmentFile != null && !referenceAttachmentFile.isEmpty()) {
            try {
                String folder = forumPostAddDTO.getType() != null && forumPostAddDTO.getType() == 1 
                    ? "requirement-attachments" 
                    : "post-attachments";
                String attachmentUrl = minioService.uploadFile(referenceAttachmentFile, folder);
                forumPost.setReferenceAttachment(attachmentUrl);
            } catch (Exception e) {
                return Result.error("附件上传失败：" + e.getMessage());
            }
        }

        if (forumPostMapper.insert(forumPost) == 0) {
            return Result.error(MessageConstant.ADD + MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.ADD + MessageConstant.SUCCESS);
    }

    /**
     * 删除帖子（软删除）
     *
     * @param postId 帖子ID
     * @return 结果
     */
    @Override
    public Result deletePost(Long postId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        ForumPost forumPost = forumPostMapper.selectById(postId);
        if (forumPost == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 检查是否是帖子作者
        if (!Objects.equals(forumPost.getUserId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }

        // 软删除
        forumPost.setStatus(1);
        forumPost.setUpdateTime(LocalDateTime.now());

        if (forumPostMapper.updateById(forumPost) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    @Override
    public Result likePost(Long postId) {
        ForumPost forumPost = forumPostMapper.selectById(postId);
        if (forumPost == null || forumPost.getStatus() == 1) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        forumPost.setLikeCount(forumPost.getLikeCount() + 1);

        if (forumPostMapper.updateById(forumPost) == 0) {
            return Result.error(MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.SUCCESS);
    }

    /**
     * 取消点赞帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    @Override
    public Result cancelLikePost(Long postId) {
        ForumPost forumPost = forumPostMapper.selectById(postId);
        if (forumPost == null || forumPost.getStatus() == 1) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        if (forumPost.getLikeCount() > 0) {
            forumPost.setLikeCount(forumPost.getLikeCount() - 1);
        }

        if (forumPostMapper.updateById(forumPost) == 0) {
            return Result.error(MessageConstant.FAILED);
        }
        return Result.success(MessageConstant.SUCCESS);
    }

    /**
     * 获取用户帖子列表（按状态筛选）
     *
     * @param userId      用户ID
     * @param pageNum     页码
     * @param pageSize    页大小
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 帖子列表
     */
    @Override
    public Result getUserPosts(Long userId, Integer pageNum, Integer pageSize, Integer auditStatus) {
        Page<ForumPostVO> page = new Page<>(pageNum, pageSize);
        IPage<ForumPostVO> postPage = forumPostMapper.getUserPosts(page, userId, auditStatus);

        PageResult<ForumPostVO> pageResult = new PageResult<>();
        pageResult.setTotal(postPage.getTotal());
        pageResult.setItems(postPage.getRecords());

        return Result.success(pageResult);
    }

    /**
     * 更新需求帖子的接单状态（仅需求发布者可以操作）
     *
     * @param postId     帖子ID
     * @param isAccepted 接单状态：0-未接单，1-已接单
     * @return 结果
     */
    @Override
    public Result updateAcceptStatus(Long postId, Integer isAccepted) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        ForumPost forumPost = forumPostMapper.selectById(postId);
        if (forumPost == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 检查是否是帖子作者
        if (!Objects.equals(forumPost.getUserId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }

        // 检查是否是需求类型的帖子
        if (forumPost.getType() == null || forumPost.getType() != 1) {
            return Result.error("只有需求类型的帖子可以设置接单状态");
        }

        // 验证状态值
        if (isAccepted == null || (isAccepted != 0 && isAccepted != 1)) {
            return Result.error("接单状态值无效，必须为0（未接单）或1（已接单）");
        }

        // 更新接单状态
        forumPost.setIsAccepted(isAccepted);
        forumPost.setUpdateTime(LocalDateTime.now());

        if (forumPostMapper.updateById(forumPost) == 0) {
            return Result.error("更新接单状态失败");
        }

        return Result.success("更新接单状态成功");
    }

    /**
     * 更新帖子并重新提交审核
     *
     * @param forumPostAddDTO 帖子信息（包含postId）
     * @param referenceAttachmentFile 参考附件文件（可选）
     * @return 结果
     */
    @Override
    public Result updatePost(ForumPostAddDTO forumPostAddDTO, MultipartFile referenceAttachmentFile) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map == null) {
            return Result.error("用户未登录");
        }
        
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);
        
        // 检查postId是否存在
        if (forumPostAddDTO.getPostId() == null) {
            return Result.error("帖子ID不能为空");
        }
        
        // 查询帖子信息
        ForumPost forumPost = forumPostMapper.selectById(forumPostAddDTO.getPostId());
        if (forumPost == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        
        // 检查是否是帖子作者
        if (!Objects.equals(forumPost.getUserId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }
        
        try {
            // 验证必填字段
            if (forumPostAddDTO.getTitle() == null || forumPostAddDTO.getTitle().trim().isEmpty()) {
                return Result.error("帖子标题不能为空");
            }
            if (forumPostAddDTO.getContent() == null || forumPostAddDTO.getContent().trim().isEmpty()) {
                return Result.error("帖子内容不能为空");
            }
            
            // 更新帖子基本信息
            forumPost.setTitle(forumPostAddDTO.getTitle().trim());
            forumPost.setContent(forumPostAddDTO.getContent().trim());
            if (forumPostAddDTO.getType() != null) {
                forumPost.setType(forumPostAddDTO.getType());
            }
            
            // 如果是需求类型，更新需求相关字段
            if (forumPostAddDTO.getType() != null && forumPostAddDTO.getType() == 1) {
                forumPost.setRequirementType(forumPostAddDTO.getRequirementType());
                forumPost.setTimeRequirement(forumPostAddDTO.getTimeRequirement());
                forumPost.setBudget(forumPostAddDTO.getBudget());
                forumPost.setStyleDescription(forumPostAddDTO.getStyleDescription());
            }
            
            // 更新参考附件（如果提供了新附件）
            if (referenceAttachmentFile != null && !referenceAttachmentFile.isEmpty()) {
                // 删除旧附件
                String oldAttachment = forumPost.getReferenceAttachment();
                if (oldAttachment != null && !oldAttachment.isEmpty()) {
                    try {
                        minioService.deleteFile(oldAttachment);
                    } catch (Exception e) {
                        log.warn("删除旧附件文件失败: " + oldAttachment, e);
                    }
                }
                // 上传新附件
                String folder = forumPostAddDTO.getType() != null && forumPostAddDTO.getType() == 1 
                    ? "requirement-attachments" 
                    : "post-attachments";
                String attachmentUrl = minioService.uploadFile(referenceAttachmentFile, folder);
                forumPost.setReferenceAttachment(attachmentUrl);
            }
            
            // 重置审核状态为待审核
            forumPost.setAuditStatus(0);
            forumPost.setUpdateTime(LocalDateTime.now());
            
            // 更新数据库
            if (forumPostMapper.updateById(forumPost) == 0) {
                return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
            }
            
            return Result.success("帖子更新成功，已重新提交审核");
        } catch (Exception e) {
            log.error("更新帖子失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

}

