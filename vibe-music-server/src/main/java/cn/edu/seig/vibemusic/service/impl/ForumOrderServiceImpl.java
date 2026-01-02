package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.mapper.ForumOrderMapper;
import cn.edu.seig.vibemusic.mapper.ForumPostMapper;
import cn.edu.seig.vibemusic.model.entity.ForumOrder;
import cn.edu.seig.vibemusic.model.entity.ForumPost;
import cn.edu.seig.vibemusic.model.vo.ForumOrderVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IForumOrderService;
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
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 论坛接单 服务实现类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-03
 */
@Slf4j
@Service
public class ForumOrderServiceImpl extends ServiceImpl<ForumOrderMapper, ForumOrder> implements IForumOrderService {

    @Autowired
    private ForumOrderMapper forumOrderMapper;

    @Autowired
    private ForumPostMapper forumPostMapper;

    /**
     * 申请接单（其他用户点击接单按钮）
     */
    @Override
    @Transactional
    public Result applyOrder(Long postId) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long accepterId = TypeConversionUtil.toLong(userIdObj);

        // 查询帖子信息
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 检查是否是需求类型的帖子
        if (post.getType() == null || post.getType() != 1) {
            return Result.error("只有需求类型的帖子可以接单");
        }

        // 检查是否是帖子作者（不能接自己的单）
        if (Objects.equals(post.getUserId(), accepterId)) {
            return Result.error("不能接自己的需求单");
        }

        // 检查是否已经申请过接单（排除已拒绝的状态3）
        // 如果之前被拒绝，允许重新申请
        ForumOrderVO existingOrder = forumOrderMapper.selectOrderByPostIdAndAccepterId(postId, accepterId);
        log.info("检查已存在的接单记录 - postId: {}, accepterId: {}, existingOrder: {}", 
                postId, accepterId, existingOrder != null ? 
                (existingOrder.getId() + ", status: " + existingOrder.getStatus() + ", status类型: " + 
                 (existingOrder.getStatus() != null ? existingOrder.getStatus().getClass().getName() : "null")) : "null");
        
        // 检查状态：如果存在且状态不是3（已拒绝），不允许重复申请
        if (existingOrder != null && existingOrder.getStatus() != null) {
            Integer status = existingOrder.getStatus();
            log.info("检查接单状态 - orderId: {}, status: {}, status == 3: {}, status != 3: {}", 
                    existingOrder.getId(), status, status.equals(3), !status.equals(3));
            
            if (!status.equals(3)) {
                // 如果存在非拒绝状态的接单记录，不允许重复申请
                log.warn("不允许重复申请 - orderId: {}, status: {}", existingOrder.getId(), status);
                return Result.error("您已经申请过接单，请勿重复申请");
            }
        }

        // 检查帖子是否已经有接单者（状态为1或2）
        int acceptedCount = forumOrderMapper.countAcceptedOrders(postId);
        if (acceptedCount > 0) {
            return Result.error("该需求已被接单");
        }

        // 如果之前被拒绝（状态3），删除旧记录以便重新申请
        if (existingOrder != null && existingOrder.getStatus() != null && existingOrder.getStatus().equals(3)) {
            log.info("删除已拒绝的接单记录，orderId: {}, postId: {}, accepterId: {}", 
                    existingOrder.getId(), postId, accepterId);
            int deleteResult = forumOrderMapper.deleteById(existingOrder.getId());
            log.info("删除已拒绝接单记录结果: {}, orderId: {}", deleteResult, existingOrder.getId());
        }

        // 创建接单申请
        ForumOrder order = new ForumOrder();
        order.setPostId(postId);
        order.setPosterId(post.getUserId());
        order.setAccepterId(accepterId);
        order.setStatus(0); // 待同意
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        if (forumOrderMapper.insert(order) > 0) {
            return Result.success("接单申请已提交，等待需求发布者同意");
        }

        return Result.error("申请接单失败");
    }

    /**
     * 分页查询需求发布者的接单申请列表
     */
    @Override
    public Result getOrderApplicationsByPoster(Integer pageNum, Integer pageSize, Integer status) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long posterId = TypeConversionUtil.toLong(userIdObj);

        Page<ForumOrderVO> page = new Page<>(pageNum, pageSize);
        IPage<ForumOrderVO> orderPage = forumOrderMapper.selectOrderApplicationsByPoster(page, posterId, null, status);

        // 设置状态文本
        orderPage.getRecords().forEach(order -> {
            order.setStatusText(getStatusText(order.getStatus()));
        });

        return Result.success(new PageResult<>(orderPage.getTotal(), orderPage.getRecords()));
    }

    /**
     * 同意接单（需求发布者操作）
     */
    @Override
    @Transactional
    public Result acceptOrder(Long orderId) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        // 查询接单信息
        ForumOrder order = forumOrderMapper.selectById(orderId);
        if (order == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 检查是否是需求发布者
        if (!Objects.equals(order.getPosterId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }

        // 检查状态是否为待同意
        if (order.getStatus() != 0) {
            return Result.error("该接单申请状态不正确");
        }

        // 检查帖子是否已经有接单者（状态为1或2）
        int acceptedCount = forumOrderMapper.countAcceptedOrders(order.getPostId());
        if (acceptedCount > 0) {
            return Result.error("该需求已被接单");
        }

        // 更新接单状态为已接单未完成
        order.setStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        if (forumOrderMapper.updateById(order) == 0) {
            return Result.error("同意接单失败");
        }

        // 更新帖子的接单状态
        ForumPost post = forumPostMapper.selectById(order.getPostId());
        if (post != null) {
            post.setIsAccepted(1);
            post.setUpdateTime(LocalDateTime.now());
            forumPostMapper.updateById(post);
        }

        return Result.success("已同意接单");
    }

    /**
     * 拒绝接单（需求发布者操作）
     */
    @Override
    @Transactional
    public Result rejectOrder(Long orderId) {
        log.info("拒绝接单请求，orderId: {}", orderId);
        
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);
        
        log.info("当前用户ID: {}", userId);

        // 查询接单信息
        ForumOrder order = forumOrderMapper.selectById(orderId);
        if (order == null) {
            log.warn("接单记录不存在，orderId: {}", orderId);
            return Result.error(MessageConstant.NOT_FOUND);
        }
        
        log.info("接单记录信息 - orderId: {}, posterId: {}, accepterId: {}, status: {}", 
                order.getId(), order.getPosterId(), order.getAccepterId(), order.getStatus());

        // 检查是否是需求发布者
        if (!Objects.equals(order.getPosterId(), userId)) {
            log.warn("权限不足 - 当前用户ID: {}, 需求发布者ID: {}", userId, order.getPosterId());
            return Result.error(MessageConstant.NO_PERMISSION);
        }

        // 检查状态是否为待同意
        if (order.getStatus() != 0) {
            log.warn("接单状态不正确 - 当前状态: {}, 期望状态: 0", order.getStatus());
            return Result.error("只能拒绝待同意的接单申请");
        }

        // 更新接单状态为已拒绝（状态3）
        log.info("准备更新接单状态为3，orderId: {}, 当前状态: {}", orderId, order.getStatus());
        order.setStatus(3);
        order.setUpdateTime(LocalDateTime.now());
        log.info("更新前对象状态: status={}, updateTime={}", order.getStatus(), order.getUpdateTime());
        
        int updateResult = forumOrderMapper.updateById(order);
        log.info("更新接单申请结果: {}, orderId: {}", updateResult, orderId);
        
        // 验证更新是否成功
        ForumOrder updatedOrder = forumOrderMapper.selectById(orderId);
        if (updatedOrder != null) {
            log.info("更新后查询结果 - orderId: {}, status: {}", updatedOrder.getId(), updatedOrder.getStatus());
        } else {
            log.warn("更新后查询不到接单记录，orderId: {}", orderId);
        }
        
        if (updateResult == 0) {
            log.error("更新接单申请失败，orderId: {}", orderId);
            return Result.error("拒绝接单失败");
        }

        log.info("成功拒绝接单申请，orderId: {}, 新状态: {}", orderId, updatedOrder != null ? updatedOrder.getStatus() : "未知");
        return Result.success("已拒绝接单申请");
    }

    /**
     * 标记为已完成（需求发布者操作）
     */
    @Override
    @Transactional
    public Result completeOrder(Long orderId) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        // 查询接单信息
        ForumOrder order = forumOrderMapper.selectById(orderId);
        if (order == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        // 检查是否是需求发布者
        if (!Objects.equals(order.getPosterId(), userId)) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }

        // 检查状态是否为已接单未完成
        if (order.getStatus() != 1) {
            return Result.error("只有已接单未完成的状态才能标记为已完成");
        }

        // 更新接单状态为已完成
        order.setStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        if (forumOrderMapper.updateById(order) == 0) {
            return Result.error("标记为已完成失败");
        }

        return Result.success("已标记为已完成");
    }

    /**
     * 分页查询接单者的接单列表
     */
    @Override
    public Result getOrdersByAccepter(Integer pageNum, Integer pageSize, Integer status) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long accepterId = TypeConversionUtil.toLong(userIdObj);

        log.info("查询接单列表 - accepterId: {}, pageNum: {}, pageSize: {}, status: {}", 
                accepterId, pageNum, pageSize, status);

        Page<ForumOrderVO> page = new Page<>(pageNum, pageSize);
        IPage<ForumOrderVO> orderPage = forumOrderMapper.selectOrdersByAccepter(page, accepterId, status);

        log.info("查询结果 - 总数: {}, 记录数: {}", orderPage.getTotal(), orderPage.getRecords().size());
        // 记录每个接单的状态（使用INFO级别以便调试）
        orderPage.getRecords().forEach(order -> {
            log.info("接单记录 - id: {}, status: {}, postTitle: {}, accepterId: {}", 
                    order.getId(), order.getStatus(), order.getPostTitle(), order.getAccepterId());
        });

        // 设置状态文本
        orderPage.getRecords().forEach(order -> {
            order.setStatusText(getStatusText(order.getStatus()));
        });

        return Result.success(new PageResult<>(orderPage.getTotal(), orderPage.getRecords()));
    }

    /**
     * 获取接单详情
     */
    @Override
    public Result getOrderDetail(Long orderId) {
        ForumOrderVO orderVO = forumOrderMapper.selectOrderDetail(orderId);
        if (orderVO == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }

        orderVO.setStatusText(getStatusText(orderVO.getStatus()));
        return Result.success(orderVO);
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待同意";
            case 1:
                return "已接单未完成";
            case 2:
                return "已完成";
            case 3:
                return "已拒绝";
            default:
                return "未知";
        }
    }
}

