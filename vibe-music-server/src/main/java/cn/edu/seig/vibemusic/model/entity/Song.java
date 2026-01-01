package cn.edu.seig.vibemusic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_song")
public class Song implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 歌曲 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long songId;

    /**
     * 歌手 id
     */
    @TableField("artist_id")
    private Long artistId;

    /**
     * 歌名
     */
    @TableField("name")
    private String songName;

    /**
     * 歌词
     */
    @TableField("lyric")
    private String lyric;

    /**
     * 歌曲时长
     */
    @TableField("duration")
    private String duration;

    /**
     * 歌曲风格
     */
    @TableField("style")
    private String style;

    /**
     * 歌曲封面 url
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 歌曲 url
     */
    @TableField("audio_url")
    private String audioUrl;

    /**
     * 歌曲发行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("release_time")
    private LocalDate releaseTime;

    /**
     * 创建者用户ID（原创歌曲）
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 是否原创
     */
    @TableField("is_original")
    private Boolean isOriginal;

    /**
     * 是否开启打赏
     */
    @TableField("is_reward_enabled")
    private Boolean isRewardEnabled;

    /**
     * 收款码图片URL
     */
    @TableField("reward_qr_url")
    private String rewardQrUrl;

    /**
     * 审核状态：0-待审核，1-已通过，2-未通过
     */
    @TableField("audit_status")
    private Integer auditStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

}
