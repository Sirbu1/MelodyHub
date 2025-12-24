package cn.edu.seig.vibemusic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SongVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 歌曲 id
     */
    private Long songId;

    /**
     * 歌名
     */
    private String songName;

    /**
     * 歌手
     */
    private String artistName;

    /**
     * 专辑
     */
    private String album;

    /**
     * 歌曲风格
     */
    private String style;

    /**
     * 歌曲时长
     */
    private String duration;

    /**
     * 歌曲封面 url
     */
    private String coverUrl;

    /**
     * 歌曲 url
     */
    private String audioUrl;

    /**
     * 喜欢状态
     * 0：默认
     * 1：喜欢
     */
    private Integer likeStatus;

    /**
     * 歌曲发行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseTime;

    /**
     * 是否原创
     */
    private Boolean isOriginal;

    /**
     * 是否开启打赏
     */
    private Boolean isRewardEnabled;

    /**
     * 收款码图片URL
     */
    private String rewardQrUrl;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者用户名
     */
    private String creatorName;

    /**
     * 审核状态：0-待审核，1-已通过，2-未通过
     */
    private Integer auditStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
