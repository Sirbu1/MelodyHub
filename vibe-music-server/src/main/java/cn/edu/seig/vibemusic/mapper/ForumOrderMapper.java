package cn.edu.seig.vibemusic.mapper;

import cn.edu.seig.vibemusic.model.entity.ForumOrder;
import cn.edu.seig.vibemusic.model.vo.ForumOrderVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 论坛接单 Mapper 接口
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-03
 */
@Mapper
public interface ForumOrderMapper extends BaseMapper<ForumOrder> {

    /**
     * 分页查询需求发布者的接单申请列表
     *
     * @param page     分页对象
     * @param posterId 需求发布者ID
     * @param postId   帖子ID（可选，如果提供则只查询该帖子的申请）
     * @param status   状态筛选（可选）
     * @return 接单申请列表
     */
    IPage<ForumOrderVO> selectOrderApplicationsByPoster(Page<ForumOrderVO> page,
                                                          @Param("posterId") Long posterId,
                                                          @Param("postId") Long postId,
                                                          @Param("status") Integer status);

    /**
     * 分页查询接单者的接单列表
     *
     * @param page       分页对象
     * @param accepterId 接单者ID
     * @param status     状态筛选（可选）
     * @return 接单列表
     */
    IPage<ForumOrderVO> selectOrdersByAccepter(Page<ForumOrderVO> page,
                                                @Param("accepterId") Long accepterId,
                                                @Param("status") Integer status);

    /**
     * 查询接单详情
     *
     * @param orderId 接单ID
     * @return 接单详情
     */
    ForumOrderVO selectOrderDetail(@Param("orderId") Long orderId);

    /**
     * 检查用户是否已申请接单
     *
     * @param postId     帖子ID
     * @param accepterId 接单者ID
     * @return 接单记录数量
     */
    int countByPostIdAndAccepterId(@Param("postId") Long postId, @Param("accepterId") Long accepterId);

    /**
     * 检查帖子是否已有接单者（状态为1或2）
     *
     * @param postId 帖子ID
     * @return 接单记录数量
     */
    int countAcceptedOrders(@Param("postId") Long postId);

    /**
     * 查询接单者在指定帖子中的接单信息
     *
     * @param postId     帖子ID
     * @param accepterId 接单者ID
     * @return 接单信息
     */
    ForumOrderVO selectOrderByPostIdAndAccepterId(@Param("postId") Long postId, @Param("accepterId") Long accepterId);

    /**
     * 统计用户已接单但未完成的接单数量（状态为1）
     *
     * @param accepterId 接单者ID
     * @return 未完成的接单数量
     */
    int countIncompleteOrdersByAccepter(@Param("accepterId") Long accepterId);
}

