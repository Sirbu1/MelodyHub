package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.enumeration.LikeStatusEnum;
import cn.edu.seig.vibemusic.enumeration.RoleEnum;
import cn.edu.seig.vibemusic.mapper.ArtistMapper;
import cn.edu.seig.vibemusic.mapper.UserFavoriteMapper;
import cn.edu.seig.vibemusic.model.dto.ArtistAddDTO;
import cn.edu.seig.vibemusic.model.dto.ArtistDTO;
import cn.edu.seig.vibemusic.model.dto.ArtistUpdateDTO;
import cn.edu.seig.vibemusic.model.entity.Artist;
import cn.edu.seig.vibemusic.model.entity.UserFavorite;
import cn.edu.seig.vibemusic.model.vo.ArtistDetailVO;
import cn.edu.seig.vibemusic.model.vo.ArtistNameVO;
import cn.edu.seig.vibemusic.model.vo.ArtistVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IArtistService;
import cn.edu.seig.vibemusic.service.MinioService;
import cn.edu.seig.vibemusic.util.JwtUtil;
import cn.edu.seig.vibemusic.util.TypeConversionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "artistCache")
public class ArtistServiceImpl extends ServiceImpl<ArtistMapper, Artist> implements IArtistService {

    @Autowired
    private ArtistMapper artistMapper;
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    @Autowired
    private MinioService minioService;

    /**
     * 获取所有歌手列表
     *
     * @param artistDTO artistDTO
     * @return 歌手列表
     */
    @Override
    // @Cacheable(key = "#artistDTO.pageNum + '-' + #artistDTO.pageSize + '-' + #artistDTO.artistName + '-' + #artistDTO.gender + '-' + #artistDTO.area")  // 临时禁用缓存以便调试
    public Result<PageResult<ArtistVO>> getAllArtists(ArtistDTO artistDTO) {
        log.info("======================================");
        log.info("客户端查询歌手列表，pageNum: {}, pageSize: {}, artistName: {}, gender: {}, area: {}", 
                artistDTO.getPageNum(), artistDTO.getPageSize(), artistDTO.getArtistName(), 
                artistDTO.getGender(), artistDTO.getArea());
        log.info("======================================");
        
        // 分页查询
        Page<Artist> page = new Page<>(artistDTO.getPageNum(), artistDTO.getPageSize());
        QueryWrapper<Artist> queryWrapper = new QueryWrapper<>();
        // 根据 artistDTO 的条件构建查询条件
        if (artistDTO.getArtistName() != null && !artistDTO.getArtistName().trim().isEmpty()) {
            queryWrapper.like("name", artistDTO.getArtistName());
        }
        if (artistDTO.getGender() != null) {
            queryWrapper.eq("gender", artistDTO.getGender());
            // 如果是原创歌手（gender=3），只显示上传过已通过审核的原创歌曲的用户
            if (artistDTO.getGender() == 3) {
                log.info("查询原创歌手，添加IN子查询条件");
                // 使用IN子查询：只返回那些用户名在"上传过已通过审核的原创歌曲的用户"列表中的歌手
                queryWrapper.inSql("name", 
                    "SELECT u.username FROM tb_user u " +
                    "INNER JOIN tb_song s ON u.id = s.creator_id " +
                    "WHERE s.is_original = true " +
                    "AND (s.audit_status = 1 OR s.audit_status IS NULL) " +
                    "GROUP BY u.username");
            }
        } else {
            // 如果没有指定gender，排除原创歌手（gender=3），因为原创歌手需要特殊处理
            queryWrapper.ne("gender", 3);
            log.info("未指定gender，排除原创歌手");
        }
        if (artistDTO.getArea() != null && !artistDTO.getArea().trim().isEmpty()) {
            queryWrapper.like("area", artistDTO.getArea());
        }

        log.info("执行查询，QueryWrapper: {}", queryWrapper.getSqlSegment());
        IPage<Artist> artistPage = artistMapper.selectPage(page, queryWrapper);
        log.info("查询结果：总数 {}, 当前页记录数 {}", artistPage.getTotal(), artistPage.getRecords().size());
        
        if (artistPage.getRecords().size() == 0) {
            log.warn("没有找到符合条件的歌手");
            return Result.success(MessageConstant.DATA_NOT_FOUND, new PageResult<>(0L, List.of()));
        }

        // 转换成 ArtistVO
        List<ArtistVO> artistVOList = artistPage.getRecords().stream()
                .map(artist -> {
                    ArtistVO artistVO = new ArtistVO();
                    BeanUtils.copyProperties(artist, artistVO);
                    log.debug("转换歌手：artistId={}, artistName={}, gender={}", 
                            artist.getArtistId(), artist.getArtistName(), artist.getGender());
                    return artistVO;
                }).toList();

        log.info("成功返回 {} 个歌手", artistVOList.size());
        return Result.success(new PageResult<>(artistPage.getTotal(), artistVOList));
    }

    /**
     * 获取所有歌手列表（含详情）
     *
     * @param artistDTO artistDTO
     * @return 歌手列表
     */
    @Override
    // @Cacheable(key = "#artistDTO.pageNum + '-' + #artistDTO.pageSize + '-' + #artistDTO.artistName + '-' + #artistDTO.gender + '-' + #artistDTO.area + '-admin'")  // 临时禁用缓存以便调试
    public Result<PageResult<Artist>> getAllArtistsAndDetail(ArtistDTO artistDTO) {
        log.info("======================================");
        log.info("管理端查询歌手列表，pageNum: {}, pageSize: {}, artistName: {}, gender: {}, area: {}", 
                artistDTO.getPageNum(), artistDTO.getPageSize(), artistDTO.getArtistName(), 
                artistDTO.getGender(), artistDTO.getArea());
        log.info("artistName类型: {}, gender类型: {}", 
                artistDTO.getArtistName() != null ? artistDTO.getArtistName().getClass().getName() : "null",
                artistDTO.getGender() != null ? artistDTO.getGender().getClass().getName() : "null");
        log.info("======================================");
        // 分页查询
        Page<Artist> page = new Page<>(artistDTO.getPageNum(), artistDTO.getPageSize());
        QueryWrapper<Artist> queryWrapper = new QueryWrapper<>();
        // 根据 artistDTO 的条件构建查询条件
        boolean hasSearchKeyword = artistDTO.getArtistName() != null && !artistDTO.getArtistName().trim().isEmpty();
        
        if (hasSearchKeyword) {
            queryWrapper.like("name", artistDTO.getArtistName());
        }
        
        if (artistDTO.getGender() != null) {
            queryWrapper.eq("gender", artistDTO.getGender());
            // 如果是原创歌手（gender=3），只显示上传过已通过审核的原创歌曲的用户
            if (artistDTO.getGender() == 3) {
                // 使用IN子查询：只返回那些用户名在"上传过已通过审核的原创歌曲的用户"列表中的歌手
                queryWrapper.inSql("name", 
                    "SELECT u.username FROM tb_user u " +
                    "INNER JOIN tb_song s ON u.id = s.creator_id " +
                    "WHERE s.is_original = true " +
                    "AND (s.audit_status = 1 OR s.audit_status IS NULL) " +
                    "GROUP BY u.username");
            }
        } else {
            // 如果没有指定gender
            if (!hasSearchKeyword) {
                // 没有搜索关键词时，排除原创歌手（gender=3），因为原创歌手需要特殊处理
                queryWrapper.ne("gender", 3);
            } else {
                // 有搜索关键词时，允许搜索所有类型，但原创歌手需要满足条件
                // 使用嵌套条件：(非原创歌手) OR (原创歌手 AND 有已通过审核的歌曲)
                queryWrapper.and(wrapper -> {
                    wrapper.ne("gender", 3)
                           .or(w -> w.eq("gender", 3)
                                    .inSql("name", 
                                        "SELECT u.username FROM tb_user u " +
                                        "INNER JOIN tb_song s ON u.id = s.creator_id " +
                                        "WHERE s.is_original = true " +
                                        "AND (s.audit_status = 1 OR s.audit_status IS NULL) " +
                                        "GROUP BY u.username"));
                });
            }
        }
        if (artistDTO.getArea() != null) {
            queryWrapper.like("area", artistDTO.getArea());
        }

        // 倒序排序
        queryWrapper.orderByDesc("id");

        log.info("执行查询，SQL条件: {}", queryWrapper.getSqlSegment());
        log.info("执行查询，参数: {}", queryWrapper.getParamNameValuePairs());
        
        IPage<Artist> artistPage = artistMapper.selectPage(page, queryWrapper);
        log.info("查询结果：总数 {}, 当前页记录数 {}", artistPage.getTotal(), artistPage.getRecords().size());
        
        if (artistPage.getRecords().size() == 0) {
            log.warn("没有找到符合条件的歌手");
            return Result.success(MessageConstant.DATA_NOT_FOUND, new PageResult<>(0L, List.of()));
        }

        return Result.success(new PageResult<>(artistPage.getTotal(), artistPage.getRecords()));
    }

    /**
     * 获取所有歌手id和歌手名称
     *
     * @return 歌手名称列表
     */
    @Override
    @Cacheable(key = "'allArtistNames'")
    public Result<List<ArtistNameVO>> getAllArtistNames() {
        List<Artist> artists = artistMapper.selectList(new QueryWrapper<Artist>().orderByDesc("id"));
        if (artists.isEmpty()) {
            return Result.success(MessageConstant.DATA_NOT_FOUND, null);
        }

        List<ArtistNameVO> artistNameVOList = artists.stream()
                .map(artist -> {
                    ArtistNameVO artistNameVO = new ArtistNameVO();
                    artistNameVO.setArtistId(artist.getArtistId());
                    artistNameVO.setArtistName(artist.getArtistName());
                    return artistNameVO;
                }).toList();

        return Result.success(artistNameVOList);
    }

    /**
     * 获取随机歌手
     * 随机歌手的数量为 10
     *
     * @return 随机歌手列表
     */
    @Override
    public Result<List<ArtistVO>> getRandomArtists() {
        QueryWrapper<Artist> queryWrapper = new QueryWrapper<>();
        // 对于原创歌手（gender=3），只显示上传过已通过审核的原创歌曲的用户
        queryWrapper.and(wrapper -> {
            wrapper.ne("gender", 3)
                    .or(w -> w.eq("gender", 3)
                            .inSql("name", 
                                "SELECT u.username FROM tb_user u " +
                                "INNER JOIN tb_song s ON u.id = s.creator_id " +
                                "WHERE s.is_original = true " +
                                "AND (s.audit_status = 1 OR s.audit_status IS NULL) " +
                                "GROUP BY u.username"));
        });
        queryWrapper.last("ORDER BY RAND() LIMIT 10");

        List<Artist> artists = artistMapper.selectList(queryWrapper);
        if (artists.isEmpty()) {
            return Result.success(MessageConstant.DATA_NOT_FOUND, null);
        }

        List<ArtistVO> artistVOList = artists.stream()
                .map(artist -> {
                    ArtistVO artistVO = new ArtistVO();
                    BeanUtils.copyProperties(artist, artistVO);
                    return artistVO;
                }).toList();

        return Result.success(artistVOList);
    }

    /**
     * 获取歌手详情
     *
     * @param artistId 歌手id
     * @param request  HttpServletRequest，用于获取请求头中的 token
     * @return 歌手详情
     */
    @Override
    // @Cacheable(key = "#artistId")  // 临时禁用缓存以便调试
    public Result<ArtistDetailVO> getArtistDetail(Long artistId, HttpServletRequest request) {
        log.info("======================================");
        log.info("查询歌手详情，artistId: {}", artistId);
        ArtistDetailVO artistDetailVO = artistMapper.getArtistDetailById(artistId);
        
        if (artistDetailVO == null) {
            log.error("未找到歌手详情，artistId: {}", artistId);
            return Result.error(MessageConstant.NOT_FOUND);
        }
        
        log.info("查询到歌手详情：artistId={}, artistName={}, gender={}, avatar={}, songs.size={}", 
                artistDetailVO.getArtistId(), artistDetailVO.getArtistName(), 
                artistDetailVO.getGender(), artistDetailVO.getAvatar(), 
                artistDetailVO.getSongs() != null ? artistDetailVO.getSongs().size() : 0);
        log.info("======================================");

        // 设置默认状态
        List<SongVO> songVOList = artistDetailVO.getSongs();
        if (songVOList == null) {
            songVOList = List.of();
            artistDetailVO.setSongs(songVOList);
        }
        songVOList.forEach(songVO -> songVO.setLikeStatus(LikeStatusEnum.DEFAULT.getId()));

        // 获取请求头中的 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀
        }

        Map<String, Object> map = null;
        if (token != null && !token.isEmpty()) {
            try {
                map = JwtUtil.parseToken(token);
            } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
                // Token 过期，按未登录处理
                log.warn("Token expired, treating as unauthenticated user");
                map = null;
            } catch (Exception e) {
                // Token 解析失败，按未登录处理
                log.warn("Token parse failed: {}, treating as unauthenticated user", e.getMessage());
                map = null;
            }
        }

        // 如果 token 解析成功且用户为登录状态，进一步操作
        if (map != null) {
            String role = (String) map.get(JwtClaimsConstant.ROLE);
            if (role.equals(RoleEnum.USER.getRole())) {
                Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
                Long userId = TypeConversionUtil.toLong(userIdObj);

                // 获取用户收藏的歌曲
                List<UserFavorite> favoriteSongs = userFavoriteMapper.selectList(new QueryWrapper<UserFavorite>()
                        .eq("user_id", userId)
                        .eq("type", 0));

                // 获取用户收藏的歌曲 id
                Set<Long> favoriteSongIds = favoriteSongs.stream()
                        .map(UserFavorite::getSongId)
                        .collect(Collectors.toSet());

                // 检查并更新状态
                for (SongVO songVO : songVOList) {
                    if (favoriteSongIds.contains(songVO.getSongId())) {
                        songVO.setLikeStatus(LikeStatusEnum.LIKE.getId());
                    }
                }
            }
        }

        // 设置歌曲列表
        artistDetailVO.setSongs(songVOList);

        return Result.success(artistDetailVO);
    }

    /**
     * 获取所有歌手数量
     *
     * @param gender 性别
     * @param area   地区
     * @return 歌手数量
     */
    @Override
    public Result<Long> getAllArtistsCount(Integer gender, String area) {
        QueryWrapper<Artist> queryWrapper = new QueryWrapper<>();
        if (gender != null) {
            queryWrapper.eq("gender", gender);
            // 如果是原创歌手（gender=3），只统计上传过已通过审核的原创歌曲的用户
            if (gender == 3) {
                // 使用IN子查询：只返回那些用户名在"上传过已通过审核的原创歌曲的用户"列表中的歌手
                queryWrapper.inSql("name", 
                    "SELECT u.username FROM tb_user u " +
                    "INNER JOIN tb_song s ON u.id = s.creator_id " +
                    "WHERE s.is_original = true " +
                    "AND (s.audit_status = 1 OR s.audit_status IS NULL) " +
                    "GROUP BY u.username");
            }
        }
        if (area != null) {
            queryWrapper.eq("area", area);
        }

        return Result.success(artistMapper.selectCount(queryWrapper));
    }

    /**
     * 添加歌手
     *
     * @param artistAddDTO 歌手添加DTO
     * @return 添加结果
     */
    @Override
    @CacheEvict(cacheNames = "artistCache", allEntries = true)
    public Result addArtist(ArtistAddDTO artistAddDTO) {
        QueryWrapper<Artist> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", artistAddDTO.getArtistName());
        if (artistMapper.selectCount(queryWrapper) > 0) {
            return Result.error(MessageConstant.ARTIST + MessageConstant.ALREADY_EXISTS);
        }

        Artist artist = new Artist();
        BeanUtils.copyProperties(artistAddDTO, artist);
        artistMapper.insert(artist);

        return Result.success(MessageConstant.ADD + MessageConstant.SUCCESS);
    }

    /**
     * 更新歌手
     *
     * @param artistUpdateDTO 歌手更新DTO
     * @return 更新结果
     */
    @Override
    @CacheEvict(cacheNames = "artistCache", allEntries = true)
    public Result updateArtist(ArtistUpdateDTO artistUpdateDTO) {
        Long artistId = artistUpdateDTO.getArtistId();

        Artist artistByArtistName = artistMapper.selectOne(new QueryWrapper<Artist>().eq("name", artistUpdateDTO.getArtistName()));
        if (artistByArtistName != null && !artistByArtistName.getArtistId().equals(artistId)) {
            return Result.error(MessageConstant.ARTIST + MessageConstant.ALREADY_EXISTS);
        }

        Artist artist = new Artist();
        BeanUtils.copyProperties(artistUpdateDTO, artist);
        if (artistMapper.updateById(artist) == 0) {
            return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.UPDATE + MessageConstant.SUCCESS);
    }

    /**
     * 更新歌手头像
     *
     * @param artistId 歌手id
     * @param avatar   头像
     * @return 更新结果
     */
    @Override
    @CacheEvict(cacheNames = "artistCache", allEntries = true)
    public Result updateArtistAvatar(Long artistId, String avatar) {
        Artist artist = artistMapper.selectById(artistId);
        String avatarUrl = artist.getAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            minioService.deleteFile(avatarUrl);
        }

        artist.setAvatar(avatar);
        if (artistMapper.updateById(artist) == 0) {
            return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.UPDATE + MessageConstant.SUCCESS);
    }

    /**
     * 删除歌手
     *
     * @param artistId 歌手id
     * @return 删除结果
     */
    @Override
    @CacheEvict(cacheNames = "artistCache", allEntries = true)
    public Result deleteArtist(Long artistId) {
        // 1. 查询歌手信息，获取头像 URL
        Artist artist = artistMapper.selectById(artistId);
        if (artist == null) {
            return Result.error(MessageConstant.ARTIST + MessageConstant.NOT_FOUND);
        }
        String avatarUrl = artist.getAvatar();

        // 2. 先删除 MinIO 里的头像文件
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            minioService.deleteFile(avatarUrl);
        }

        // 3. 删除数据库中的歌手信息
        if (artistMapper.deleteById(artistId) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

    /**
     * 批量删除歌手
     *
     * @param artistIds 歌手id列表
     * @return 删除结果
     */
    @Override
    @CacheEvict(cacheNames = {"artistCache", "songCache"}, allEntries = true)
    public Result deleteArtists(List<Long> artistIds) {
        // 1. 查询歌手信息，获取头像 URL 列表
        List<Artist> artists = artistMapper.selectByIds(artistIds);
        List<String> avatarUrlList = artists.stream()
                .map(Artist::getAvatar)
                .filter(avatarUrl -> avatarUrl != null && !avatarUrl.isEmpty())
                .toList();

        // 2. 先删除 MinIO 里的头像文件
        for (String avatarUrl : avatarUrlList) {
            minioService.deleteFile(avatarUrl);
        }

        // 3. 删除数据库中的歌手信息
        if (artistMapper.deleteByIds(artistIds) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

}
