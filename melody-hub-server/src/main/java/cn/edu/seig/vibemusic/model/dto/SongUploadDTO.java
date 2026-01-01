package cn.edu.seig.vibemusic.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

/**
 * 歌曲上传DTO
 */
@Data
public class SongUploadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 歌曲标题
     */
    private String songName;

    /**
     * 歌曲风格
     */
    private String style;

    /**
     * 歌曲封面文件
     */
    private MultipartFile coverFile;

    /**
     * 音频文件
     */
    private MultipartFile audioFile;

    /**
     * 是否开启打赏
     */
    private Boolean isRewardEnabled;

    /**
     * 收款码图片文件（开启打赏时必填）
     */
    private MultipartFile rewardQrFile;

    /**
     * 歌曲时长（秒）
     */
    private String duration;

}
