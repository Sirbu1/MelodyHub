package cn.edu.seig.vibemusic.controller;

import cn.edu.seig.vibemusic.model.dto.ForumPostAddDTO;
import cn.edu.seig.vibemusic.model.dto.ForumPostDTO;
import cn.edu.seig.vibemusic.model.dto.ForumReplyAddDTO;
import cn.edu.seig.vibemusic.model.dto.ForumReplyDTO;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IForumPostService;
import cn.edu.seig.vibemusic.service.IForumReplyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 论坛 前端控制器
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/forum")
public class ForumController {

    @Autowired
    private IForumPostService forumPostService;

    @Autowired
    private IForumReplyService forumReplyService;

    // ==================== 帖子相关接口 ====================

    /**
     * 获取帖子列表
     *
     * @param forumPostDTO 查询条件
     * @return 帖子列表
     */
    @PostMapping("/posts")
    public Result getPostList(@RequestBody ForumPostDTO forumPostDTO) {
        return forumPostService.getPostList(forumPostDTO);
    }

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/postDetail/{id}")
    public Result getPostDetail(@PathVariable("id") Long postId) {
        return forumPostService.getPostDetail(postId);
    }

    /**
     * 发布帖子（支持文件上传的需求发布）
     *
     * @param title 帖子标题
     * @param content 帖子内容
     * @param type 帖子类型：0-交流，1-需求
     * @param requirementType 需求类型（仅需求时使用）
     * @param timeRequirement 时间要求（仅需求时使用）
     * @param budget 预算（仅需求时使用）
     * @param styleDescription 风格描述（仅需求时使用）
     * @param referenceAttachmentFile 参考附件文件（仅需求时使用）
     * @return 结果
     */
    @PostMapping("/post")
    public Result addPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("type") Integer type,
            @RequestParam(value = "requirementType", required = false) String requirementType,
            @RequestParam(value = "timeRequirement", required = false) String timeRequirement,
            @RequestParam(value = "budget", required = false) String budget,
            @RequestParam(value = "styleDescription", required = false) String styleDescription,
            @RequestParam(value = "referenceAttachmentFile", required = false) MultipartFile referenceAttachmentFile) {
        
        ForumPostAddDTO forumPostAddDTO = new ForumPostAddDTO();
        forumPostAddDTO.setTitle(title);
        forumPostAddDTO.setContent(content);
        forumPostAddDTO.setType(type);
        forumPostAddDTO.setRequirementType(requirementType);
        forumPostAddDTO.setTimeRequirement(timeRequirement);
        forumPostAddDTO.setBudget(budget);
        forumPostAddDTO.setStyleDescription(styleDescription);
        
        return forumPostService.addPost(forumPostAddDTO, referenceAttachmentFile);
    }

    /**
     * 删除帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    @DeleteMapping("/post/{id}")
    public Result deletePost(@PathVariable("id") Long postId) {
        return forumPostService.deletePost(postId);
    }

    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    @PatchMapping("/post/like/{id}")
    public Result likePost(@PathVariable("id") Long postId) {
        return forumPostService.likePost(postId);
    }

    /**
     * 取消点赞帖子
     *
     * @param postId 帖子ID
     * @return 结果
     */
    @PatchMapping("/post/cancelLike/{id}")
    public Result cancelLikePost(@PathVariable("id") Long postId) {
        return forumPostService.cancelLikePost(postId);
    }

    /**
     * 更新需求帖子的接单状态（仅需求发布者可以操作）
     *
     * @param postId     帖子ID
     * @param isAccepted 接单状态：0-未接单，1-已接单
     * @return 结果
     */
    @PatchMapping("/post/acceptStatus/{id}")
    public Result updateAcceptStatus(
            @PathVariable("id") Long postId,
            @RequestParam("isAccepted") Integer isAccepted) {
        return forumPostService.updateAcceptStatus(postId, isAccepted);
    }

    // ==================== 回复相关接口 ====================

    /**
     * 获取帖子的回复列表
     *
     * @param forumReplyDTO 查询条件
     * @return 回复列表
     */
    @PostMapping("/replies")
    public Result getReplyList(@RequestBody ForumReplyDTO forumReplyDTO) {
        return forumReplyService.getReplyList(forumReplyDTO);
    }

    /**
     * 发布回复
     *
     * @param forumReplyAddDTO 回复信息
     * @return 结果
     */
    @PostMapping("/reply")
    public Result addReply(@Valid @RequestBody ForumReplyAddDTO forumReplyAddDTO) {
        return forumReplyService.addReply(forumReplyAddDTO);
    }

    /**
     * 删除回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    @DeleteMapping("/reply/{id}")
    public Result deleteReply(@PathVariable("id") Long replyId) {
        return forumReplyService.deleteReply(replyId);
    }

    /**
     * 点赞回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    @PatchMapping("/reply/like/{id}")
    public Result likeReply(@PathVariable("id") Long replyId) {
        return forumReplyService.likeReply(replyId);
    }

    /**
     * 取消点赞回复
     *
     * @param replyId 回复ID
     * @return 结果
     */
    @PatchMapping("/reply/cancelLike/{id}")
    public Result cancelLikeReply(@PathVariable("id") Long replyId) {
        return forumReplyService.cancelLikeReply(replyId);
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
    @GetMapping("/getUserPosts/{userId}")
    public Result getUserPosts(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return forumPostService.getUserPosts(userId, pageNum, pageSize, auditStatus);
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
    @GetMapping("/getUserReplies/{userId}")
    public Result getUserReplies(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return forumReplyService.getUserReplies(userId, pageNum, pageSize, auditStatus);
    }

}

