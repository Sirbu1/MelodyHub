package cn.edu.seig.vibemusic.service.impl;

import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.enumeration.LikeStatusEnum;
import cn.edu.seig.vibemusic.enumeration.RoleEnum;
import cn.edu.seig.vibemusic.mapper.ArtistMapper;
import cn.edu.seig.vibemusic.mapper.GenreMapper;
import cn.edu.seig.vibemusic.mapper.SongMapper;
import cn.edu.seig.vibemusic.mapper.StyleMapper;
import cn.edu.seig.vibemusic.mapper.UserFavoriteMapper;
import cn.edu.seig.vibemusic.mapper.UserMapper;
import cn.edu.seig.vibemusic.model.dto.SongAddDTO;
import cn.edu.seig.vibemusic.model.dto.SongAndArtistDTO;
import cn.edu.seig.vibemusic.model.dto.SongDTO;
import cn.edu.seig.vibemusic.model.dto.SongUpdateDTO;
import cn.edu.seig.vibemusic.model.dto.SongUploadDTO;
import cn.edu.seig.vibemusic.model.entity.Artist;
import cn.edu.seig.vibemusic.model.entity.Genre;
import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.model.entity.Style;
import cn.edu.seig.vibemusic.model.entity.User;
import cn.edu.seig.vibemusic.model.entity.UserFavorite;
import cn.edu.seig.vibemusic.model.vo.SongAdminVO;
import cn.edu.seig.vibemusic.model.vo.SongDetailVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.IArtistService;
import cn.edu.seig.vibemusic.service.ISongService;
import cn.edu.seig.vibemusic.service.MinioService;
import cn.edu.seig.vibemusic.util.JwtUtil;
import cn.edu.seig.vibemusic.util.ThreadLocalUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
@CacheConfig(cacheNames = "songCache")
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements ISongService {

    @Autowired
    private SongMapper songMapper;
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    @Autowired
    private StyleMapper styleMapper;
    @Autowired
    private GenreMapper genreMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ArtistMapper artistMapper;
    @Autowired
    private IArtistService artistService;
    @Autowired
    private MinioService minioService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取所有歌曲
     *
     * @param songDTO songDTO
     * @return 歌曲列表
     */
    @Override
    @Cacheable(key = "#songDTO.pageNum + '-' + #songDTO.pageSize + '-' + #songDTO.songName + '-' + #songDTO.artistName")
    public Result<PageResult<SongVO>> getAllSongs(SongDTO songDTO, HttpServletRequest request) {
        // 获取请求头中的 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀
        }

        Map<String, Object> map = null;
        if (token != null && !token.isEmpty()) {
            map = JwtUtil.parseToken(token);
        }

        // 查询歌曲列表
        Page<SongVO> page = new Page<>(songDTO.getPageNum(), songDTO.getPageSize());
        IPage<SongVO> songPage = songMapper.getSongsWithArtist(page, songDTO.getSongName(), songDTO.getArtistName());
        if (songPage.getRecords().isEmpty()) {
            return Result.success(MessageConstant.DATA_NOT_FOUND, new PageResult<>(0L, null));
        }

        // 设置默认状态
        List<SongVO> songVOList = songPage.getRecords().stream()
                .peek(songVO -> songVO.setLikeStatus(LikeStatusEnum.DEFAULT.getId()))
                .toList();

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

        return Result.success(new PageResult<>(songPage.getTotal(), songVOList));
    }

    /**
     * 获取歌手的所有歌曲
     *
     * @param songDTO songAndArtistDTO
     * @return 歌曲列表
     */
    @Override
    @Cacheable(key = "#songDTO.pageNum + '-' + #songDTO.pageSize + '-' + #songDTO.songName + '-' + #songDTO.artistId")
    public Result<PageResult<SongAdminVO>> getAllSongsByArtist(SongAndArtistDTO songDTO) {
        // 分页查询
        Page<SongAdminVO> page = new Page<>(songDTO.getPageNum(), songDTO.getPageSize());
        IPage<SongAdminVO> songPage = songMapper.getSongsWithArtistName(page, songDTO.getArtistId(), songDTO.getSongName());

        if (songPage.getRecords().isEmpty()) {
            return Result.success(MessageConstant.DATA_NOT_FOUND, new PageResult<>(0L, null));
        }

        return Result.success(new PageResult<>(songPage.getTotal(), songPage.getRecords()));
    }

    /**
     * 获取推荐歌曲
     * 推荐歌曲的数量为 20
     *
     * @param request HttpServletRequest，用于获取请求头中的 token
     * @return 推荐歌曲列表
     */
    @Override
    public Result<List<SongVO>> getRecommendedSongs(HttpServletRequest request) {
        // 获取请求头中的 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);  // 去掉 "Bearer " 前缀
        }

        Map<String, Object> map = null;
        if (token != null && !token.isEmpty()) {
            map = JwtUtil.parseToken(token);
        }

        // 用户未登录，返回随机歌曲列表
        if (map == null) {
            return Result.success(songMapper.getRandomSongsWithArtist());
        }

        // 获取用户 ID
        Long userId = TypeConversionUtil.toLong(map.get(JwtClaimsConstant.USER_ID));

        // 查询用户收藏的歌曲 ID
        List<Long> favoriteSongIds = userFavoriteMapper.getFavoriteSongIdsByUserId(userId);
        if (favoriteSongIds.isEmpty()) {
            return Result.success(songMapper.getRandomSongsWithArtist());
        }

        // 查询用户收藏的歌曲风格并统计频率
        List<Long> favoriteStyleIds = songMapper.getFavoriteSongStyles(favoriteSongIds);
        Map<Long, Long> styleFrequency = favoriteStyleIds.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 按风格出现次数降序排序
        List<Long> sortedStyleIds = styleFrequency.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 从 Redis 获取缓存的推荐列表
        String redisKey = "recommended_songs:" + userId;
        List<SongVO> cachedSongs = redisTemplate.opsForList().range(redisKey, 0, -1);

        // 如果 Redis 没有缓存，则查询数据库并缓存
        if (cachedSongs == null || cachedSongs.isEmpty()) {
            // 根据排序后的风格推荐歌曲（排除已收藏歌曲）
            cachedSongs = songMapper.getRecommendedSongsByStyles(sortedStyleIds, favoriteSongIds, 80);
            redisTemplate.opsForList().rightPushAll(redisKey, cachedSongs);
            redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES); // 设置过期时间 30 分钟
        }

        // 随机选取 20 首
        Collections.shuffle(cachedSongs);
        List<SongVO> recommendedSongs = cachedSongs.subList(0, Math.min(20, cachedSongs.size()));

        // 如果推荐的歌曲不足 20 首，则用随机歌曲填充
        if (recommendedSongs.size() < 20) {
            List<SongVO> randomSongs = songMapper.getRandomSongsWithArtist();
            Set<Long> addedSongIds = recommendedSongs.stream().map(SongVO::getSongId).collect(Collectors.toSet());
            for (SongVO song : randomSongs) {
                if (recommendedSongs.size() >= 20) break;
                if (!addedSongIds.contains(song.getSongId())) {
                    recommendedSongs.add(song);
                }
            }
        }

        return Result.success(recommendedSongs);
    }

    /**
     * 获取歌曲详情
     *
     * @param songId  歌曲id
     * @param request HttpServletRequest，用于获取请求头中的 token
     * @return 歌曲详情
     */
    @Override
    // @Cacheable(key = "#songId")  // 临时禁用缓存进行调试
    public Result<SongDetailVO> getSongDetail(Long songId, HttpServletRequest request) {
        // 先查询原始数据，检查数据库
        Song song = songMapper.selectById(songId);
        if (song != null) {
            log.info("========== 数据库原始数据 ==========");
            log.info("songId: {}", songId);
            log.info("songName: {}", song.getSongName());
            log.info("isOriginal: {}", song.getIsOriginal());
            log.info("isRewardEnabled: {}", song.getIsRewardEnabled());
            log.info("rewardQrUrl: {}", song.getRewardQrUrl());
            log.info("creatorId: {}", song.getCreatorId());
            log.info("artistId: {}", song.getArtistId());
            log.info("===================================");
        } else {
            log.warn("数据库中未找到歌曲 - songId: {}", songId);
        }
        
        SongDetailVO songDetailVO = songMapper.getSongDetailById(songId);
        
        // 如果查询结果为null，说明歌曲不存在或未审核通过
        if (songDetailVO == null) {
            return Result.error(MessageConstant.NOT_FOUND);
        }
        
        // 调试日志：输出完整的歌曲详情信息
        log.info("========== MyBatis查询结果 ==========");
        log.info("songId: {}", songId);
        log.info("songName: {}", songDetailVO.getSongName());
        log.info("isOriginal: {}", songDetailVO.getIsOriginal());
        log.info("isRewardEnabled: {}", songDetailVO.getIsRewardEnabled());
        log.info("rewardQrUrl: {}", songDetailVO.getRewardQrUrl());
        log.info("creatorName: {}", songDetailVO.getCreatorName());
        log.info("artistName: {}", songDetailVO.getArtistName());
        log.info("===================================");

        // 获取请求头中的 token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);  // 去掉 "Bearer " 前缀
        }

        Map<String, Object> map = null;
        if (token != null && !token.isEmpty()) {
            map = JwtUtil.parseToken(token);
        }

        // 如果 token 解析成功且用户为登录状态，进一步操作
        if (map != null) {
            String role = (String) map.get(JwtClaimsConstant.ROLE);
            if (role.equals(RoleEnum.USER.getRole())) {
                Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
                Long userId = TypeConversionUtil.toLong(userIdObj);

                // 获取用户收藏的歌曲
                UserFavorite favoriteSong = userFavoriteMapper.selectOne(new QueryWrapper<UserFavorite>()
                        .eq("user_id", userId)
                        .eq("type", 0)
                        .eq("song_id", songId));
                if (favoriteSong != null) {
                    songDetailVO.setLikeStatus(LikeStatusEnum.LIKE.getId());
                }
            }
        }

        return Result.success(songDetailVO);
    }

    /**
     * 获取所有歌曲的数量
     *
     * @param style 歌曲风格
     * @return 歌曲数量
     */
    @Override
    public Result<Long> getAllSongsCount(String style) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        // 只统计审核通过的歌曲
        queryWrapper.and(wrapper -> wrapper.isNull("audit_status").or().eq("audit_status", 1));
        if (style != null) {
            queryWrapper.like("style", style);
        }

        return Result.success(songMapper.selectCount(queryWrapper));
    }

    /**
     * 添加歌曲信息
     *
     * @param songAddDTO 歌曲信息
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result addSong(SongAddDTO songAddDTO) {
        Song song = new Song();
        BeanUtils.copyProperties(songAddDTO, song);

        // 插入歌曲记录
        if (songMapper.insert(song) == 0) {
            return Result.error(MessageConstant.ADD + MessageConstant.FAILED);
        }

        // 获取刚插入的歌曲记录
        Song songInDB = songMapper.selectOne(new QueryWrapper<Song>()
                .eq("artist_id", songAddDTO.getArtistId())
                .eq("name", songAddDTO.getSongName())
                .orderByDesc("id")
                .last("LIMIT 1"));

        if (songInDB == null) {
            return Result.error(MessageConstant.SONG + MessageConstant.NOT_FOUND);
        }

        Long songId = songInDB.getSongId();

        // 解析风格字段（多个风格以逗号分隔）
        String styleStr = songAddDTO.getStyle();
        if (styleStr != null && !styleStr.isEmpty()) {
            List<String> styles = Arrays.asList(styleStr.split(","));

            // 查询风格 ID
            List<Style> styleList = styleMapper.selectList(new QueryWrapper<Style>().in("name", styles));

            // 插入到 tb_genre
            for (Style style : styleList) {
                Genre genre = new Genre();
                genre.setSongId(songId);
                genre.setStyleId(style.getStyleId());
                genreMapper.insert(genre);
            }
        }

        return Result.success(MessageConstant.ADD + MessageConstant.SUCCESS);
    }

    /**
     * 更新歌曲信息
     *
     * @param songUpdateDTO 歌曲信息
     * @return 结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result updateSong(SongUpdateDTO songUpdateDTO) {
        // 查询数据库中是否存在该歌曲
        Song songInDB = songMapper.selectById(songUpdateDTO.getSongId());
        if (songInDB == null) {
            return Result.error(MessageConstant.SONG + MessageConstant.NOT_FOUND);
        }

        // 更新歌曲基本信息
        Song song = new Song();
        BeanUtils.copyProperties(songUpdateDTO, song);
        if (songMapper.updateById(song) == 0) {
            return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
        }

        Long songId = songUpdateDTO.getSongId();

        // 删除 tb_genre 中该歌曲的原有风格映射
        genreMapper.delete(new QueryWrapper<Genre>().eq("song_id", songId));

        // 解析新的风格字段（多个风格以逗号分隔）
        String styleStr = songUpdateDTO.getStyle();
        if (styleStr != null && !styleStr.isEmpty()) {
            List<String> styles = Arrays.asList(styleStr.split(","));

            // 查询风格 ID
            List<Style> styleList = styleMapper.selectList(new QueryWrapper<Style>().in("name", styles));

            // 插入新的风格映射到 tb_genre
            for (Style style : styleList) {
                Genre genre = new Genre();
                genre.setSongId(songId);
                genre.setStyleId(style.getStyleId());
                genreMapper.insert(genre);
            }
        }

        return Result.success(MessageConstant.UPDATE + MessageConstant.SUCCESS);
    }

    /**
     * 更新歌曲封面
     *
     * @param songId   歌曲id
     * @param coverUrl 封面url
     * @return 更新结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result updateSongCover(Long songId, String coverUrl) {
        Song song = songMapper.selectById(songId);
        String cover = song.getCoverUrl();
        if (cover != null && !cover.isEmpty()) {
            minioService.deleteFile(cover);
        }

        song.setCoverUrl(coverUrl);
        if (songMapper.updateById(song) == 0) {
            return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.UPDATE + MessageConstant.SUCCESS);
    }

    /**
     * 更新歌曲音频
     *
     * @param songId   歌曲id
     * @param audioUrl 音频url
     * @return 更新结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result updateSongAudio(Long songId, String audioUrl, String duration) {
        Song song = songMapper.selectById(songId);
        String audio = song.getAudioUrl();
        if (audio != null && !audio.isEmpty()) {
            minioService.deleteFile(audio);
        }

        song.setAudioUrl(audioUrl).setDuration(duration);
        if (songMapper.updateById(song) == 0) {
            return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.UPDATE + MessageConstant.SUCCESS);
    }

    /**
     * 删除歌曲
     *
     * @param songId 歌曲id
     * @return 删除结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result deleteSong(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            return Result.error(MessageConstant.SONG + MessageConstant.NOT_FOUND);
        }
        String cover = song.getCoverUrl();
        String audio = song.getAudioUrl();

        if (cover != null && !cover.isEmpty()) {
            minioService.deleteFile(cover);
        }
        if (audio != null && !audio.isEmpty()) {
            minioService.deleteFile(audio);
        }

        if (songMapper.deleteById(songId) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

    /**
     * 批量删除歌曲
     *
     * @param songIds 歌曲id列表
     * @return 删除结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result deleteSongs(List<Long> songIds) {
        // 1. 查询歌曲信息，获取歌曲封面 URL 列表
        List<Song> songs = songMapper.selectByIds(songIds);
        List<String> coverUrlList = songs.stream()
                .map(Song::getCoverUrl)
                .filter(coverUrl -> coverUrl != null && !coverUrl.isEmpty())
                .toList();
        List<String> audioUrlList = songs.stream()
                .map(Song::getAudioUrl)
                .filter(audioUrl -> audioUrl != null && !audioUrl.isEmpty())
                .toList();

        // 2. 先删除 MinIO 里的歌曲封面和音频文件
        for (String coverUrl : coverUrlList) {
            minioService.deleteFile(coverUrl);
        }
        for (String audioUrl : audioUrlList) {
            minioService.deleteFile(audioUrl);
        }

        // 3. 删除数据库中的歌曲信息
        if (songMapper.deleteByIds(songIds) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

    /**
     * 上传原创歌曲
     *
     * @param songUploadDTO 上传信息
     * @return 上传结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result uploadOriginalSong(SongUploadDTO songUploadDTO) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);

        // 检查用户是否填写了完整的歌手信息（生日、国籍、简介）
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getBirth() == null 
            || user.getArea() == null || user.getArea().trim().isEmpty()
            || user.getIntroduction() == null || user.getIntroduction().trim().isEmpty()) {
            return Result.error("上传歌曲前，请先在个人信息页面填写完整的歌手信息（生日、国籍、简介）");
        }

        // 检查当日上传限制（最多10首）
        Integer todayUploads = songMapper.countUserTodayUploads(userId);
        if (todayUploads >= 10) {
            return Result.error("今日上传次数已达上限（10首），请明天再试");
        }

        // 验证文件格式和大小
        String validationError = validateUploadFiles(songUploadDTO);
        if (validationError != null) {
            return Result.error(validationError);
        }

        try {
            // 上传封面文件
            String coverUrl = null;
            if (songUploadDTO.getCoverFile() != null && !songUploadDTO.getCoverFile().isEmpty()) {
                coverUrl = minioService.uploadFile(songUploadDTO.getCoverFile(), "covers");
            }

            // 上传音频文件
            String audioUrl = null;
            if (songUploadDTO.getAudioFile() != null && !songUploadDTO.getAudioFile().isEmpty()) {
                audioUrl = minioService.uploadFile(songUploadDTO.getAudioFile(), "songs");
            }

            // 上传收款码文件（如果开启打赏）
            String rewardQrUrl = null;
            if (songUploadDTO.getIsRewardEnabled() != null && songUploadDTO.getIsRewardEnabled()
                && songUploadDTO.getRewardQrFile() != null && !songUploadDTO.getRewardQrFile().isEmpty()) {
                rewardQrUrl = minioService.uploadFile(songUploadDTO.getRewardQrFile(), "reward-qr");
            }

            // 创建歌曲实体
            Song song = new Song();
            song.setSongName(songUploadDTO.getSongName());
            song.setStyle(songUploadDTO.getStyle());
            song.setCoverUrl(coverUrl);
            song.setAudioUrl(audioUrl);
            song.setArtistId(null); // 原创歌曲没有关联艺术家，设置为null
            song.setReleaseTime(java.time.LocalDate.now()); // 设置发行时间为当前日期
            song.setCreatorId(userId);
            song.setIsOriginal(true);
            song.setIsRewardEnabled(songUploadDTO.getIsRewardEnabled() != null ? songUploadDTO.getIsRewardEnabled() : false);
            song.setRewardQrUrl(rewardQrUrl);
            song.setDuration(songUploadDTO.getDuration()); // 设置歌曲时长
            song.setAuditStatus(0); // 设置审核状态为待审核
            song.setCreateTime(java.time.LocalDateTime.now());
            song.setUpdateTime(java.time.LocalDateTime.now());

            if (songMapper.insert(song) == 0) {
                return Result.error(MessageConstant.ADD + MessageConstant.FAILED);
            }

            // 上传成功后，创建或更新歌手记录（原创歌手）
            try {
                // 查询是否已存在同名歌手（原创歌手）
                QueryWrapper<Artist> artistQueryWrapper = new QueryWrapper<>();
                artistQueryWrapper.eq("name", user.getUsername())
                                  .eq("gender", 3); // 原创歌手类型为3
                Artist existingArtist = artistMapper.selectOne(artistQueryWrapper);

                if (existingArtist != null) {
                    // 更新现有歌手信息
                    log.info("更新现有原创歌手信息，userId: {}, artistId: {}", userId, existingArtist.getArtistId());
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
                        log.info("成功更新原创歌手信息，userId: {}, artistId: {}", userId, existingArtist.getArtistId());
                    } else {
                        log.warn("更新原创歌手信息失败，userId: {}, artistId: {}", userId, existingArtist.getArtistId());
                    }
                } else {
                    // 创建新歌手记录（默认类型为原创歌手）
                    log.info("创建新原创歌手记录，userId: {}, username: {}", userId, user.getUsername());
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
                    int insertResult = artistMapper.insert(artist);
                    if (insertResult > 0) {
                        log.info("成功创建原创歌手记录，userId: {}, username: {}, artistId: {}", 
                                userId, user.getUsername(), artist.getArtistId());
                    } else {
                        log.warn("创建原创歌手记录失败，userId: {}, username: {}", userId, user.getUsername());
                    }
                }
                // 清除歌手缓存，确保新创建的歌手能立即显示
                try {
                    // 清除所有以 "artistCache" 开头的缓存
                    Set<String> keys = redisTemplate.keys("artistCache::*");
                    if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                        log.info("已清除歌手缓存，清除数量: {}", keys.size());
                    }
                } catch (Exception cacheException) {
                    log.warn("清除歌手缓存失败", cacheException);
                }
            } catch (Exception e) {
                // 记录错误但不影响歌曲上传
                log.error("创建或更新歌手记录失败，userId: {}, username: {}", userId, user.getUsername(), e);
            }

            return Result.success("歌曲上传成功，等待审核", song.getSongId());
        } catch (Exception e) {
            log.error("原创歌曲上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户原创歌曲
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 歌曲列表
     */
    @Override
    public Result<PageResult<SongVO>> getUserOriginalSongs(Long userId, Integer pageNum, Integer pageSize, Integer auditStatus) {
        try {
            Page<SongVO> page = new Page<>(pageNum, pageSize);
            IPage<SongVO> songPage = songMapper.getUserOriginalSongs(page, userId, auditStatus);

            PageResult<SongVO> pageResult = new PageResult<>();
            pageResult.setTotal(songPage.getTotal());
            pageResult.setItems(songPage.getRecords());

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("获取用户原创歌曲失败，userId: {}, pageNum: {}, pageSize: {}, auditStatus: {}", 
                    userId, pageNum, pageSize, auditStatus, e);
            return Result.error("获取歌曲列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有原创歌曲
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 歌曲列表
     */
    @Override
    public Result<PageResult<SongVO>> getAllOriginalSongs(Integer pageNum, Integer pageSize) {
        Page<SongVO> page = new Page<>(pageNum, pageSize);
        IPage<SongVO> songPage = songMapper.getAllOriginalSongs(page);

        if (songPage.getRecords().isEmpty()) {
            return Result.success(MessageConstant.DATA_NOT_FOUND, new PageResult<>(0L, null));
        }

        return Result.success(new PageResult<>(songPage.getTotal(), songPage.getRecords()));
    }

    /**
     * 删除原创歌曲（仅创建者可删除）
     *
     * @param songId 歌曲ID
     * @return 删除结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result deleteOriginalSong(Long songId) {
        log.info("开始删除原创歌曲，songId: {}", songId);
        
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map == null) {
            log.error("ThreadLocal中没有用户信息");
            return Result.error("用户未登录");
        }
        
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);
        log.info("当前用户ID: {}", userId);

        // 查询歌曲信息
        Song song = songMapper.selectById(songId);
        if (song == null) {
            log.error("歌曲不存在，songId: {}", songId);
            return Result.error(MessageConstant.SONG + MessageConstant.NOT_FOUND);
        }
        
        log.info("查询到歌曲信息: songId={}, songName={}, creatorId={}, isOriginal={}", 
                song.getSongId(), song.getSongName(), song.getCreatorId(), song.getIsOriginal());

        // 检查是否是原创歌曲
        if (song.getIsOriginal() == null || !song.getIsOriginal()) {
            return Result.error("只能删除原创歌曲");
        }

        // 检查是否是创建者
        if (song.getCreatorId() == null || !song.getCreatorId().equals(userId)) {
            return Result.error("只能删除自己上传的歌曲");
        }

        // 删除MinIO中的文件
        String cover = song.getCoverUrl();
        String audio = song.getAudioUrl();
        String rewardQr = song.getRewardQrUrl();

        if (cover != null && !cover.isEmpty()) {
            try {
                minioService.deleteFile(cover);
            } catch (Exception e) {
                log.warn("删除封面文件失败: {}", cover, e);
            }
        }
        if (audio != null && !audio.isEmpty()) {
            try {
                minioService.deleteFile(audio);
            } catch (Exception e) {
                log.warn("删除音频文件失败: {}", audio, e);
            }
        }
        if (rewardQr != null && !rewardQr.isEmpty()) {
            try {
                minioService.deleteFile(rewardQr);
            } catch (Exception e) {
                log.warn("删除收款码文件失败: {}", rewardQr, e);
            }
        }

        // 删除数据库记录
        if (songMapper.deleteById(songId) == 0) {
            return Result.error(MessageConstant.DELETE + MessageConstant.FAILED);
        }

        return Result.success(MessageConstant.DELETE + MessageConstant.SUCCESS);
    }

    /**
     * 更新原创歌曲并重新提交审核
     *
     * @param songUploadDTO 更新信息（包含songId）
     * @return 更新结果
     */
    @Override
    @CacheEvict(cacheNames = "songCache", allEntries = true)
    public Result updateOriginalSong(SongUploadDTO songUploadDTO) {
        // 获取当前用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map == null) {
            log.error("ThreadLocal中没有用户信息");
            return Result.error("用户未登录");
        }
        
        Object userIdObj = map.get(JwtClaimsConstant.USER_ID);
        Long userId = TypeConversionUtil.toLong(userIdObj);
        
        // 检查songId是否存在
        if (songUploadDTO.getSongId() == null) {
            return Result.error("歌曲ID不能为空");
        }
        
        // 查询歌曲信息
        Song song = songMapper.selectById(songUploadDTO.getSongId());
        if (song == null) {
            return Result.error(MessageConstant.SONG + MessageConstant.NOT_FOUND);
        }
        
        // 检查是否是原创歌曲
        if (song.getIsOriginal() == null || !song.getIsOriginal()) {
            return Result.error("只能更新原创歌曲");
        }
        
        // 检查是否是创建者
        if (song.getCreatorId() == null || !song.getCreatorId().equals(userId)) {
            return Result.error("只能更新自己上传的歌曲");
        }
        
        // 验证文件格式和大小（如果提供了新文件）
        if (songUploadDTO.getAudioFile() != null && !songUploadDTO.getAudioFile().isEmpty()) {
            String validationError = validateUploadFiles(songUploadDTO);
            if (validationError != null) {
                return Result.error(validationError);
            }
        }
        
        try {
            // 更新封面文件（如果提供了新封面）
            if (songUploadDTO.getCoverFile() != null && !songUploadDTO.getCoverFile().isEmpty()) {
                // 删除旧封面
                String oldCover = song.getCoverUrl();
                if (oldCover != null && !oldCover.isEmpty()) {
                    try {
                        minioService.deleteFile(oldCover);
                    } catch (Exception e) {
                        log.warn("删除旧封面文件失败: {}", oldCover, e);
                    }
                }
                // 上传新封面
                String coverUrl = minioService.uploadFile(songUploadDTO.getCoverFile(), "covers");
                song.setCoverUrl(coverUrl);
            }
            
            // 更新音频文件（如果提供了新音频）
            if (songUploadDTO.getAudioFile() != null && !songUploadDTO.getAudioFile().isEmpty()) {
                // 删除旧音频
                String oldAudio = song.getAudioUrl();
                if (oldAudio != null && !oldAudio.isEmpty()) {
                    try {
                        minioService.deleteFile(oldAudio);
                    } catch (Exception e) {
                        log.warn("删除旧音频文件失败: {}", oldAudio, e);
                    }
                }
                // 上传新音频
                String audioUrl = minioService.uploadFile(songUploadDTO.getAudioFile(), "songs");
                song.setAudioUrl(audioUrl);
                // 更新时长
                if (songUploadDTO.getDuration() != null) {
                    song.setDuration(songUploadDTO.getDuration());
                }
            }
            
            // 更新收款码文件（如果开启打赏且提供了新收款码）
            if (songUploadDTO.getIsRewardEnabled() != null && songUploadDTO.getIsRewardEnabled()) {
                if (songUploadDTO.getRewardQrFile() != null && !songUploadDTO.getRewardQrFile().isEmpty()) {
                    // 删除旧收款码
                    String oldRewardQr = song.getRewardQrUrl();
                    if (oldRewardQr != null && !oldRewardQr.isEmpty()) {
                        try {
                            minioService.deleteFile(oldRewardQr);
                        } catch (Exception e) {
                            log.warn("删除旧收款码文件失败: {}", oldRewardQr, e);
                        }
                    }
                    // 上传新收款码
                    String rewardQrUrl = minioService.uploadFile(songUploadDTO.getRewardQrFile(), "reward-qr");
                    song.setRewardQrUrl(rewardQrUrl);
                }
                song.setIsRewardEnabled(true);
            } else {
                // 如果关闭打赏，删除收款码
                if (song.getRewardQrUrl() != null && !song.getRewardQrUrl().isEmpty()) {
                    try {
                        minioService.deleteFile(song.getRewardQrUrl());
                    } catch (Exception e) {
                        log.warn("删除收款码文件失败: {}", song.getRewardQrUrl(), e);
                    }
                    song.setRewardQrUrl(null);
                }
                song.setIsRewardEnabled(false);
            }
            
            // 更新歌曲基本信息
            if (songUploadDTO.getSongName() != null && !songUploadDTO.getSongName().trim().isEmpty()) {
                song.setSongName(songUploadDTO.getSongName().trim());
            }
            if (songUploadDTO.getStyle() != null && !songUploadDTO.getStyle().trim().isEmpty()) {
                song.setStyle(songUploadDTO.getStyle().trim());
            }
            
            // 重置审核状态为待审核
            song.setAuditStatus(0);
            song.setUpdateTime(java.time.LocalDateTime.now());
            
            // 更新数据库
            if (songMapper.updateById(song) == 0) {
                return Result.error(MessageConstant.UPDATE + MessageConstant.FAILED);
            }
            
            return Result.success("歌曲更新成功，已重新提交审核");
        } catch (Exception e) {
            log.error("更新原创歌曲失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 验证上传文件
     *
     * @param songUploadDTO 上传信息
     * @return 验证错误信息，如果验证通过返回null
     */
    private String validateUploadFiles(SongUploadDTO songUploadDTO) {
        // 验证音频文件
        if (songUploadDTO.getAudioFile() == null || songUploadDTO.getAudioFile().isEmpty()) {
            return "请上传音频文件";
        }

        // 检查音频文件格式
        String audioFileName = songUploadDTO.getAudioFile().getOriginalFilename();
        if (audioFileName != null) {
            String audioExtension = audioFileName.substring(audioFileName.lastIndexOf(".") + 1).toLowerCase();
            if (!audioExtension.equals("mp3") && !audioExtension.equals("wav")) {
                return "音频文件格式仅支持MP3和WAV";
            }
        }

        // 检查音频文件大小（100MB）
        long audioSize = songUploadDTO.getAudioFile().getSize();
        if (audioSize > 100 * 1024 * 1024) {
            return "文件大于100M，请压缩后上传";
        }

        // 验证封面文件（如果提供）
        if (songUploadDTO.getCoverFile() != null && !songUploadDTO.getCoverFile().isEmpty()) {
            long coverSize = songUploadDTO.getCoverFile().getSize();
            if (coverSize > 10 * 1024 * 1024) { // 封面文件最大10MB
                return "封面文件大小不能超过10MB";
            }
        }

        // 验证收款码文件（如果开启打赏且提供）
        if (songUploadDTO.getIsRewardEnabled() != null && songUploadDTO.getIsRewardEnabled()) {
            if (songUploadDTO.getRewardQrFile() == null || songUploadDTO.getRewardQrFile().isEmpty()) {
                return "开启打赏功能必须上传收款码图片";
            }

            long qrSize = songUploadDTO.getRewardQrFile().getSize();
            if (qrSize > 5 * 1024 * 1024) { // 收款码文件最大5MB
                return "收款码图片大小不能超过5MB";
            }
        }

        return null; // 验证通过
    }

}
