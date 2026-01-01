package cn.edu.seig.vibemusic.controller;


import cn.edu.seig.vibemusic.model.dto.SongDTO;
import cn.edu.seig.vibemusic.model.dto.SongUploadDTO;
import cn.edu.seig.vibemusic.model.vo.SongDetailVO;
import cn.edu.seig.vibemusic.model.vo.SongVO;
import cn.edu.seig.vibemusic.result.PageResult;
import cn.edu.seig.vibemusic.result.Result;
import cn.edu.seig.vibemusic.service.ISongService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author sunpingli
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/song")
public class SongController {

    @Autowired
    private ISongService songService;

    /**
     * 获取所有歌曲
     *
     * @param songDTO songDTO
     * @return 歌曲列表
     */
    @PostMapping("/getAllSongs")
    public Result<PageResult<SongVO>> getAllSongs(@RequestBody @Valid SongDTO songDTO, HttpServletRequest request) {
        return songService.getAllSongs(songDTO, request);
    }

    /**
     * 获取推荐歌曲
     * 推荐歌曲的数量为 20
     *
     * @param request 请求
     * @return 推荐歌曲列表
     */
    @GetMapping("/getRecommendedSongs")
    public Result<List<SongVO>> getRecommendedSongs(HttpServletRequest request) {
        return songService.getRecommendedSongs(request);
    }

    /**
     * 获取歌曲详情
     *
     * @param songId 歌曲id
     * @return 歌曲详情
     */
    @GetMapping("/getSongDetail/{id}")
    public Result<SongDetailVO> getSongDetail(@PathVariable("id") Long songId, HttpServletRequest request) {
        return songService.getSongDetail(songId, request);
    }

    /**
     * 上传原创歌曲
     *
     * @param songName 歌曲标题
     * @param style 歌曲风格
     * @param coverFile 封面文件
     * @param audioFile 音频文件
     * @param isRewardEnabled 是否开启打赏
     * @param rewardQrFile 收款码文件
     * @return 上传结果
     */
    @PostMapping("/uploadOriginalSong")
    public Result uploadOriginalSong(
            @RequestParam("songName") String songName,
            @RequestParam("style") String style,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "isRewardEnabled", defaultValue = "false") Boolean isRewardEnabled,
            @RequestParam(value = "rewardQrFile", required = false) MultipartFile rewardQrFile,
            @RequestParam(value = "duration", required = false) String duration,
            jakarta.servlet.http.HttpServletRequest request) {

        // 调试信息
        System.out.println("收到上传请求:");
        System.out.println("songName: " + songName);
        System.out.println("style: " + style);
        System.out.println("isRewardEnabled: " + isRewardEnabled);
        System.out.println("duration: " + duration);
        System.out.println("audioFile: " + (audioFile != null ? audioFile.getOriginalFilename() : "null"));
        System.out.println("coverFile: " + (coverFile != null ? coverFile.getOriginalFilename() : "null"));
        System.out.println("rewardQrFile: " + (rewardQrFile != null ? rewardQrFile.getOriginalFilename() : "null"));

        // 检查认证头
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        SongUploadDTO songUploadDTO = new SongUploadDTO();
        songUploadDTO.setSongName(songName);
        songUploadDTO.setStyle(style);
        songUploadDTO.setCoverFile(coverFile);
        songUploadDTO.setAudioFile(audioFile);
        songUploadDTO.setIsRewardEnabled(isRewardEnabled);
        songUploadDTO.setRewardQrFile(rewardQrFile);
        songUploadDTO.setDuration(duration);

        return songService.uploadOriginalSong(songUploadDTO);
    }

    /**
     * 获取用户原创歌曲
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param auditStatus 审核状态：0-待审核，1-已通过，2-未通过，null-全部
     * @return 用户原创歌曲列表
     */
    @GetMapping("/getUserOriginalSongs/{userId}")
    public Result<PageResult<SongVO>> getUserOriginalSongs(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return songService.getUserOriginalSongs(userId, pageNum, pageSize, auditStatus);
    }

    /**
     * 获取所有原创歌曲
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 所有原创歌曲列表
     */
    @GetMapping("/getAllOriginalSongs")
    public Result<PageResult<SongVO>> getAllOriginalSongs(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return songService.getAllOriginalSongs(pageNum, pageSize);
    }

    /**
     * 删除原创歌曲（仅创建者可删除）
     *
     * @param songId 歌曲ID
     * @return 删除结果
     */
    @DeleteMapping("/deleteOriginalSong/{id}")
    public Result deleteOriginalSong(@PathVariable("id") Long songId) {
        System.out.println("收到删除请求，songId: " + songId);
        try {
            Result result = songService.deleteOriginalSong(songId);
            System.out.println("删除结果: " + result.getCode() + ", " + result.getMessage());
            return result;
        } catch (Exception e) {
            System.err.println("删除歌曲异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新原创歌曲并重新提交审核
     *
     * @param songId 歌曲ID
     * @param songName 歌曲标题
     * @param style 歌曲风格
     * @param coverFile 封面文件（可选）
     * @param audioFile 音频文件（可选）
     * @param isRewardEnabled 是否开启打赏
     * @param rewardQrFile 收款码文件（可选）
     * @param duration 歌曲时长（可选）
     * @return 更新结果
     */
    @PostMapping("/updateOriginalSong/{id}")
    public Result updateOriginalSong(
            @PathVariable("id") Long songId,
            @RequestParam(value = "songName", required = false) String songName,
            @RequestParam(value = "style", required = false) String style,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "isRewardEnabled", required = false) Boolean isRewardEnabled,
            @RequestParam(value = "rewardQrFile", required = false) MultipartFile rewardQrFile,
            @RequestParam(value = "duration", required = false) String duration,
            jakarta.servlet.http.HttpServletRequest request) {
        
        SongUploadDTO songUploadDTO = new SongUploadDTO();
        songUploadDTO.setSongId(songId);
        songUploadDTO.setSongName(songName);
        songUploadDTO.setStyle(style);
        songUploadDTO.setCoverFile(coverFile);
        songUploadDTO.setAudioFile(audioFile);
        songUploadDTO.setIsRewardEnabled(isRewardEnabled);
        songUploadDTO.setRewardQrFile(rewardQrFile);
        songUploadDTO.setDuration(duration);
        
        return songService.updateOriginalSong(songUploadDTO);
    }

}
