package cn.edu.seig.vibemusic.service;

import cn.edu.seig.vibemusic.model.entity.ForumOrder;
import cn.edu.seig.vibemusic.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 论坛接单 服务类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-03
 */
public interface IForumOrderService extends IService<ForumOrder> {

    /**
     * 申请接单（其他用户点击接单按钮）
     *
     * @param postId 帖子ID
     * @return 结果
     */
    Result applyOrder(Long postId);

    /**
     * 分页查询需求发布者的接单申请列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @param status   状态筛选（可选）：0-待同意，1-已接单未完成，2-已完成
     * @return 接单申请列表
     */
    Result getOrderApplicationsByPoster(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 同意接单（需求发布者操作）
     *
     * @param orderId 接单ID
     * @return 结果
     */
    Result acceptOrder(Long orderId);

    /**
     * 拒绝接单（需求发布者操作）
     *
     * @param orderId 接单ID
     * @return 结果
     */
    Result rejectOrder(Long orderId);

    /**
     * 标记为已完成（需求发布者操作）
     *
     * @param orderId 接单ID
     * @return 结果
     */
    Result completeOrder(Long orderId);

    /**
     * 分页查询接单者的接单列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @param status   状态筛选（可选）：0-待同意，1-已接单未完成，2-已完成
     * @return 接单列表
     */
    Result getOrdersByAccepter(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 获取接单详情
     *
     * @param orderId 接单ID
     * @return 接单详情
     */
    Result getOrderDetail(Long orderId);
}

