package cn.edu.seig.vibemusic.mapper;

import cn.edu.seig.vibemusic.model.entity.Song;
import cn.edu.seig.vibemusic.model.vo.SongAdminVO;
import cn.edu.seig.vibemusic.model.vo.SongDetailVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Mapper
public interface SongMapper extends BaseMapper<Song> {

    // 获取歌曲列表（使用XML映射文件，支持动态SQL）
    IPage<SongVO> getSongsWithArtist(Page<SongVO> page,
                                     @Param("songName") String songName,
                                     @Param("artistName") String artistName);

    // 获取歌曲列表
    @Select("""
                SELECT 
                    s.id AS songId, 
                    s.name AS songName, 
                    s.artist_id AS artistId, 
                    s.lyric, 
                    s.duration, 
                    s.style, 
                    s.cover_url AS coverUrl, 
                    s.audio_url AS audioUrl, 
                    s.release_time AS releaseTime, 
                    COALESCE(a.name, u.username) AS artistName
                FROM tb_song s
                LEFT JOIN tb_artist a ON s.artist_id = a.id
                LEFT JOIN tb_user u ON s.creator_id = u.id
                WHERE 
                    (#{artistId} IS NULL OR s.artist_id = #{artistId})
                    AND(#{songName} IS NULL OR s.name LIKE CONCAT('%', #{songName}, '%'))
                    AND (s.audit_status IS NULL OR s.audit_status = 1)
                ORDER BY s.release_time DESC
            """)
    IPage<SongAdminVO> getSongsWithArtistName(Page<SongAdminVO> page,
                                              @Param("artistId") Long artistId,
                                              @Param("songName") String songName);

    // 获取随机歌曲列表
    @Select("""
                SELECT 
                    s.id AS songId, 
                    s.name AS songName, 
                    s.duration, 
                    s.cover_url AS coverUrl, 
                    s.audio_url AS audioUrl, 
                    s.release_time AS releaseTime, 
                    COALESCE(a.name, u.username) AS artistName
                FROM tb_song s
                LEFT JOIN tb_artist a ON s.artist_id = a.id
                LEFT JOIN tb_user u ON s.creator_id = u.id
                WHERE (s.audit_status IS NULL OR s.audit_status = 1)
                ORDER BY RAND() LIMIT 20
            """)
    List<SongVO> getRandomSongsWithArtist();

    // 根据id获取歌曲详情
    SongDetailVO getSongDetailById(Long songId);

    // 根据用户收藏的歌曲id列表获取歌曲列表
    IPage<SongVO> getSongsByIds(Page<SongVO> page,
                                @Param("songIds") List<Long> songIds,
                                @Param("songName") String songName,
                                @Param("artistName") String artistName);

    // 根据用户收藏的歌曲id列表获取歌曲列表
    List<Long> getFavoriteSongStyles(@Param("favoriteSongIds") List<Long> favoriteSongIds);

    // 根据用户收藏的歌曲id列表获取歌曲列表
    List<SongVO> getRecommendedSongsByStyles(@Param("sortedStyleIds") List<Long> sortedStyleIds,
                                             @Param("favoriteSongIds") List<Long> favoriteSongIds,
                                             @Param("limit") int limit);

    // 获取用户原创歌曲列表（使用XML映射以支持动态SQL）
    IPage<SongVO> getUserOriginalSongs(Page<SongVO> page, @Param("userId") Long userId, @Param("auditStatus") Integer auditStatus);

    // 统计用户当日上传原创歌曲数量
    @Select("""
                SELECT COUNT(*)
                FROM tb_song
                WHERE creator_id = #{userId}
                AND is_original = true
                AND DATE(create_time) = CURDATE()
            """)
    Integer countUserTodayUploads(@Param("userId") Long userId);

    // 获取所有原创歌曲列表
    @Select("""
                SELECT
                    s.id AS songId,
                    s.name AS songName,
                    s.duration,
                    s.style,
                    s.cover_url AS coverUrl,
                    s.audio_url AS audioUrl,
                    s.release_time AS releaseTime,
                    s.is_original AS isOriginal,
                    s.is_reward_enabled AS isRewardEnabled,
                    s.reward_qr_url AS rewardQrUrl,
                    s.create_time AS createTime,
                    s.creator_id AS creatorId,
                    u.username AS creatorName
                FROM tb_song s
                LEFT JOIN tb_user u ON s.creator_id = u.id
                WHERE s.is_original = true AND (s.audit_status = 1 OR s.audit_status IS NULL)
                ORDER BY s.create_time DESC
            """)
    IPage<SongVO> getAllOriginalSongs(Page<SongVO> page);

}
