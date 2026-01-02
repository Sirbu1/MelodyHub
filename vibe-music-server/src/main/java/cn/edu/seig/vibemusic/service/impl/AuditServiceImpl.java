package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.mapper.*;
import cn.edu.seig.vibemusic.model.entity.Artist;
import cn.edu.seig.vibemusic.model.entity.Comment;
import cn.edu.seig.vibemusic.model.entity.ForumPost;
import cn.edu.seig.vibemusic.model.entity.ForumReply;
import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.model.entity.User;
import cn.edu.seig.vibemusic.model.vo.CommentVO;
import cn.edu.seig.vibemusic.model.vo.ForumPostVO;
import cn.edu.seig.vibemusic.model.vo.ForumReplyVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IAuditService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 审核服务实现类
 */
@Slf4j
@Service
public class AuditServiceImpl implements IAuditService {

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private ForumPostMapper forumPostMapper;

    @Autowired
    private ForumReplyMapper forumReplyMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"songCache", "artistCache"}, allEntries = true)
    public Result approveSong(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        song.setAuditStatus(1); // 已通过
        if (songMapper.updateById(song) == 0) {
            return Result.error("审核失败");
        }

        // 如果是原创歌曲，确保创建或更新歌手记录
        if (song.getIsOriginal() != null && song.getIsOriginal() && song.getCreatorId() != null) {
            try {
                log.info("审核通过：开始处理原创歌手记录，songId: {}, creatorId: {}", songId, song.getCreatorId());
                // 获取创建者信息
                User user = userMapper.selectById(song.getCreatorId());
                if (user == null) {
                    log.warn("审核通过：未找到创建者信息，creatorId: {}", song.getCreatorId());
                } else {
                    log.info("审核通过：找到创建者信息，userId: {}, username: {}, birth: {}, area: {}, introduction: {}", 
                            user.getUserId(), user.getUsername(), user.getBirth(), user.getArea(), 
                            user.getIntroduction() != null ? (user.getIntroduction().length() > 20 ? 
                            user.getIntroduction().substring(0, 20) + "..." : user.getIntroduction()) : "null");
                    
                    // 查询是否已存在同名歌手（原创歌手）
                    QueryWrapper<Artist> artistQueryWrapper = new QueryWrapper<>();
                    artistQueryWrapper.eq("name", user.getUsername())
                                      .eq("gender", 3); // 原创歌手类型为3
                    Artist existingArtist = artistMapper.selectOne(artistQueryWrapper);
                    log.info("审核通过：查询已存在歌手，username: {}, 结果: {}", 
                            user.getUsername(), existingArtist != null ? "存在" : "不存在");

                    if (existingArtist != null) {
                        // 更新现有歌手信息
                        log.info("审核通过：更新现有原创歌手信息，userId: {}, artistId: {}", 
                                user.getUserId(), existingArtist.getArtistId());
                        existingArtist.setGender(3); // 确保类型为原创歌手
                        existingArtist.setBirth(user.getBirth());
                        existingArtist.setArea(user.getArea());
                        existingArtist.setIntroduction(user.getIntroduction());
                        // 如果用户有头像，也更新歌手头像
                        if (user.getUserAvatar() != null) {
                            existingArtist.setAvatar(user.getUserAvatar());
                        }
                        int updateResult = artistMapper.updateById(existingArtist);
                        if (updateResult > 0) {
                            log.info("审核通过：成功更新原创歌手信息，userId: {}, artistId: {}", 
                                    user.getUserId(), existingArtist.getArtistId());
                        } else {
                            log.error("审核通过：更新原创歌手信息失败，userId: {}, artistId: {}", 
                                    user.getUserId(), existingArtist.getArtistId());
                        }
                    } else {
                        // 创建新歌手记录（默认类型为原创歌手）
                        log.info("审核通过：创建新原创歌手记录，userId: {}, username: {}", 
                                user.getUserId(), user.getUsername());
                        Artist artist = new Artist();
                        artist.setArtistName(user.getUsername());
                        artist.setGender(3); // 原创歌手类型为3
                        artist.setBirth(user.getBirth());
                        artist.setArea(user.getArea());
                        artist.setIntroduction(user.getIntroduction());
                        // 如果用户有头像，也设置歌手头像
                        if (user.getUserAvatar() != null) {
                            artist.setAvatar(user.getUserAvatar());
                        }
                        
                        log.info("审核通过：准备插入歌手记录，artistName: {}, gender: {}, birth: {}, area: {}, introduction: {}", 
                                artist.getArtistName(), artist.getGender(), artist.getBirth(), 
                                artist.getArea(), artist.getIntroduction() != null ? 
                                (artist.getIntroduction().length() > 20 ? 
                                artist.getIntroduction().substring(0, 20) + "..." : artist.getIntroduction()) : "null");
                        
                        try {
                            int insertResult = artistMapper.insert(artist);
                            if (insertResult > 0) {
                                log.info("审核通过：成功创建原创歌手记录，userId: {}, username: {}, artistId: {}", 
                                        user.getUserId(), user.getUsername(), artist.getArtistId());
                            } else {
                                log.error("审核通过：创建原创歌手记录失败，userId: {}, username: {}, insertResult: {}", 
                                        user.getUserId(), user.getUsername(), insertResult);
                            }
                        } catch (Exception insertException) {
                            log.error("审核通过：插入歌手记录时发生异常，userId: {}, username: {}, 错误: {}", 
                                    user.getUserId(), user.getUsername(), insertException.getMessage(), insertException);
                        }
                    }
                    // 清除歌手缓存，确保新创建的歌手能立即显示
                    try {
                        Set<String> keys = redisTemplate.keys("artistCache::*");
                        if (keys != null && !keys.isEmpty()) {
                            redisTemplate.delete(keys);
                            log.info("审核通过：已清除歌手缓存，清除数量: {}", keys.size());
                        } else {
                            log.info("审核通过：没有找到需要清除的歌手缓存");
                        }
                    } catch (Exception cacheException) {
                        log.error("审核通过：清除歌手缓存失败", cacheException);
                    }
                }
            } catch (Exception e) {
                // 记录错误但不影响审核通过
                log.error("审核通过：创建或更新歌手记录失败，songId: {}, creatorId: {}, 错误信息: {}", 
                        songId, song.getCreatorId(), e.getMessage(), e);
            }
        } else {
            log.info("审核通过：不是原创歌曲或没有创建者，songId: {}, isOriginal: {}, creatorId: {}", 
                    songId, song.getIsOriginal(), song.getCreatorId());
        }

        return Result.success("审核通过");
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result rejectSong(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        song.setAuditStatus(2); // 未通过
        if (songMapper.updateById(song) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核拒绝");
    }

    @Override
    @Transactional
    public Result approvePost(Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        post.setAuditStatus(1); // 已通过
        if (forumPostMapper.updateById(post) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核通过");
    }

    @Override
    @Transactional
    public Result rejectPost(Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        post.setAuditStatus(2); // 未通过
        if (forumPostMapper.updateById(post) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核拒绝");
    }

    @Override
    @Transactional
    public Result approveReply(Long replyId) {
        ForumReply reply = forumReplyMapper.selectById(replyId);
        if (reply == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        reply.setAuditStatus(1); // 已通过
        if (forumReplyMapper.updateById(reply) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核通过");
    }

    @Override
    @Transactional
    public Result rejectReply(Long replyId) {
        ForumReply reply = forumReplyMapper.selectById(replyId);
        if (reply == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        reply.setAuditStatus(2); // 未通过
        if (forumReplyMapper.updateById(reply) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核拒绝");
    }

    @Override
    @Transactional
    public Result approveComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        comment.setAuditStatus(1); // 已通过
        if (commentMapper.updateById(comment) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核通过");
    }

    @Override
    @Transactional
    public Result rejectComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        comment.setAuditStatus(2); // 未通过
        if (commentMapper.updateById(comment) == 0) {
            return Result.error("审核失败");
        }
        return Result.success("审核拒绝");
    }

    @Override
    public Result<PageResult> getPendingSongs(Integer pageNum, Integer pageSize) {
        try {
            log.info("获取待审核歌曲列表，pageNum: {}, pageSize: {}", pageNum, pageSize);
            Page<Song> page = new Page<>(pageNum, pageSize);
            QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_original", true)
                    .eq("audit_status", 0)
                    .orderByDesc("create_time");
            IPage<Song> songPage = songMapper.selectPage(page, queryWrapper);
            
            log.info("查询到 {} 条待审核歌曲", songPage.getTotal());
            
            // 转换为VO（需要查询创建者信息）
            List<SongVO> songVOs = songPage.getRecords().stream().map(song -> {
                SongVO vo = new SongVO();
                vo.setSongId(song.getSongId());
                vo.setSongName(song.getSongName());
                vo.setStyle(song.getStyle());
                vo.setDuration(song.getDuration());
                vo.setCoverUrl(song.getCoverUrl());
                vo.setAudioUrl(song.getAudioUrl());
                vo.setReleaseTime(song.getReleaseTime());
                vo.setIsOriginal(song.getIsOriginal());
                vo.setIsRewardEnabled(song.getIsRewardEnabled());
                vo.setRewardQrUrl(song.getRewardQrUrl());
                vo.setCreatorId(song.getCreatorId());
                vo.setAuditStatus(song.getAuditStatus());
                vo.setCreateTime(song.getCreateTime());
                // 查询创建者名称
                if (song.getCreatorId() != null) {
                    User user = userMapper.selectById(song.getCreatorId());
                    if (user != null) {
                        vo.setCreatorName(user.getUsername());
                    }
                }
                return vo;
            }).collect(java.util.stream.Collectors.toList());

            PageResult<SongVO> pageResult = new PageResult<>();
            pageResult.setTotal(songPage.getTotal());
            pageResult.setItems(songVOs);
            log.info("返回待审核歌曲列表，总数: {}, 当前页: {}", pageResult.getTotal(), songVOs.size());
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取待审核歌曲失败", e);
            return Result.error("获取待审核歌曲失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResult> getPendingPosts(Integer pageNum, Integer pageSize) {
        try {
            Page<ForumPost> page = new Page<>(pageNum, pageSize);
            QueryWrapper<ForumPost> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("audit_status", 0)
                    .orderByDesc("create_time");
            IPage<ForumPost> postPage = forumPostMapper.selectPage(page, queryWrapper);
            
            // 转换为VO（需要查询用户信息）
            List<ForumPostVO> postVOs = postPage.getRecords().stream().map(post -> {
                ForumPostVO vo = new ForumPostVO();
                vo.setPostId(post.getPostId());
                vo.setUserId(post.getUserId());
                vo.setTitle(post.getTitle());
                vo.setContent(post.getContent());
                vo.setViewCount(post.getViewCount());
                vo.setReplyCount(post.getReplyCount());
                vo.setLikeCount(post.getLikeCount());
                vo.setIsTop(post.getIsTop());
                vo.setType(post.getType());
                vo.setRequirementType(post.getRequirementType());
                vo.setTimeRequirement(post.getTimeRequirement());
                vo.setBudget(post.getBudget());
                vo.setStyleDescription(post.getStyleDescription());
                vo.setReferenceAttachment(post.getReferenceAttachment());
                vo.setAuditStatus(post.getAuditStatus());
                vo.setCreateTime(post.getCreateTime());
                // 查询用户信息
                if (post.getUserId() != null) {
                    User user = userMapper.selectById(post.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setUserAvatar(user.getUserAvatar());
                    }
                }
                return vo;
            }).collect(java.util.stream.Collectors.toList());

            PageResult<ForumPostVO> pageResult = new PageResult<>();
            pageResult.setTotal(postPage.getTotal());
            pageResult.setItems(postVOs);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取待审核帖子失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResult> getPendingReplies(Integer pageNum, Integer pageSize) {
        try {
            Page<ForumReply> page = new Page<>(pageNum, pageSize);
            QueryWrapper<ForumReply> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("audit_status", 0)
                    .orderByDesc("create_time");
            IPage<ForumReply> replyPage = forumReplyMapper.selectPage(page, queryWrapper);
            
            // 转换为VO（需要查询用户信息）
            List<ForumReplyVO> replyVOs = replyPage.getRecords().stream().map(reply -> {
                ForumReplyVO vo = new ForumReplyVO();
                vo.setReplyId(reply.getReplyId());
                vo.setPostId(reply.getPostId());
                vo.setUserId(reply.getUserId());
                vo.setContent(reply.getContent());
                vo.setParentId(reply.getParentId());
                vo.setLikeCount(reply.getLikeCount());
                vo.setAuditStatus(reply.getAuditStatus());
                vo.setCreateTime(reply.getCreateTime());
                // 查询用户信息
                if (reply.getUserId() != null) {
                    User user = userMapper.selectById(reply.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setUserAvatar(user.getUserAvatar());
                    }
                }
                return vo;
            }).collect(java.util.stream.Collectors.toList());

            PageResult<ForumReplyVO> pageResult = new PageResult<>();
            pageResult.setTotal(replyPage.getTotal());
            pageResult.setItems(replyVOs);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取待审核回复失败：" + e.getMessage());
        }
    }

    @Override
    public Result<PageResult> getPendingComments(Integer pageNum, Integer pageSize) {
        try {
            Page<Comment> page = new Page<>(pageNum, pageSize);
            QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("audit_status", 0)
                    .orderByDesc("create_time");
            IPage<Comment> commentPage = commentMapper.selectPage(page, queryWrapper);
            
            // 转换为VO（需要查询用户信息）
            List<CommentVO> commentVOs = commentPage.getRecords().stream().map(comment -> {
                CommentVO vo = new CommentVO();
                vo.setCommentId(comment.getCommentId());
                vo.setUserId(comment.getUserId());
                vo.setSongId(comment.getSongId());
                vo.setPlaylistId(comment.getPlaylistId());
                vo.setContent(comment.getContent());
                vo.setType(comment.getType());
                vo.setLikeCount(comment.getLikeCount());
                vo.setAuditStatus(comment.getAuditStatus());
                vo.setCreateTime(comment.getCreateTime());
                // 查询用户信息
                if (comment.getUserId() != null) {
                    User user = userMapper.selectById(comment.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setUserAvatar(user.getUserAvatar());
                    }
                }
                return vo;
            }).collect(java.util.stream.Collectors.toList());

            PageResult<CommentVO> pageResult = new PageResult<>();
            pageResult.setTotal(commentPage.getTotal());
            pageResult.setItems(commentVOs);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取待审核评论失败：" + e.getMessage());
        }
    }
}

