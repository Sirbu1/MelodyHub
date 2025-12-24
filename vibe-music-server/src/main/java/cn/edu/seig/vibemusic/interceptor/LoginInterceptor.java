package cn.edu.seig.vibemusic.interceptor;


import cn.edu.seig.vibemusic.config.RolePermissionManager;
import cn.edu.seig.vibemusic.constant.JwtClaimsConstant;
import cn.edu.seig.vibemusic.constant.MessageConstant;
import cn.edu.seig.vibemusic.constant.PathConstant;
import cn.edu.seig.vibemusic.util.JwtUtil;
import cn.edu.seig.vibemusic.util.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RolePermissionManager rolePermissionManager;

    public void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
        response.setContentType("application/json;charset=UTF-8"); // 设置响应的Content-Type
        response.getWriter().write(message);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 允许 CORS 预检请求（OPTIONS 方法）直接通过
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true; // 直接放行，确保 CORS 预检请求不会被拦截
        }

        String path = request.getRequestURI();

        // 直接放行验证码相关接口
        if (path.equals("/user/sendVerificationCode") || path.equals("/user/resetUserPassword")) {
            return true;
        }

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀
        }

        // 获取 Spring 的 PathMatcher 实例
        PathMatcher pathMatcher = new AntPathMatcher();

        // 定义允许访问的路径（未登录用户也可以访问）
        List<String> allowedPaths = Arrays.asList(
                PathConstant.PLAYLIST_DETAIL_PATH,
                PathConstant.ARTIST_DETAIL_PATH,
                PathConstant.SONG_LIST_PATH,
                PathConstant.SONG_DETAIL_PATH,
                PathConstant.FORUM_POSTS_PATH,
                PathConstant.FORUM_POST_DETAIL_PATH,
                PathConstant.FORUM_REPLIES_PATH,
                "/user/sendVerificationCode",
                "/user/resetUserPassword"
        );

        // 检查路径是否匹配（公开路径）
        boolean isAllowedPath = allowedPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        // 检查是否是直接放行的公开接口
        boolean isPublicPath = path.equals("/user/sendVerificationCode") ||
            path.equals("/user/resetUserPassword") ||
            path.equals("/user/test") ||
            path.equals("/user/noauth/test") ||
            path.equals("/user/login") ||
            path.equals("/user/register") ||
            path.equals("/admin/login") ||
            path.equals("/admin/register") ||
            path.equals("/banner/getBannerList") ||
            path.startsWith("/playlist/getPlaylistDetail/") ||
            path.startsWith("/artist/getArtistDetail/") ||
            path.startsWith("/song/getSongDetail/") ||
            path.equals("/playlist/getAllPlaylists") ||
            path.equals("/playlist/getRecommendedPlaylists") ||
            path.equals("/artist/getAllArtists") ||
            path.equals("/song/getAllSongs") ||
            path.equals("/song/getRecommendedSongs") ||
            path.equals("/forum/posts") ||
            path.startsWith("/forum/postDetail/") ||
            path.equals("/forum/replies");

        // 如果没有token，只允许访问公开路径
        if (token == null || token.isEmpty()) {
            if (isAllowedPath || isPublicPath) {
                return true; // 允许未登录用户访问这些路径
            }
            sendErrorResponse(response, 401, MessageConstant.NOT_LOGIN); // 缺少令牌
            return false;
        }

        // 如果有token，验证token并设置ThreadLocal（即使路径是公开的）
        try {
            // 从redis中获取相同的token
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if (redisToken == null) {
                // token失效，如果是公开路径，仍然允许访问
                if (isAllowedPath || isPublicPath) {
                    return true;
                }
                throw new RuntimeException();
            }

            Map<String, Object> claims = JwtUtil.parseToken(token);
            String role = (String) claims.get(JwtClaimsConstant.ROLE);
            String requestURI = request.getRequestURI();

            // 如果是公开路径，直接设置ThreadLocal并放行
            if (isAllowedPath || isPublicPath) {
                ThreadLocalUtil.set(claims);
                return true;
            }

            // 非公开路径需要检查权限
            if (rolePermissionManager.hasPermission(role, requestURI)) {
                // 把业务数据存储到ThreadLocal中
                ThreadLocalUtil.set(claims);
                return true;
            } else {
                sendErrorResponse(response, 403, MessageConstant.NO_PERMISSION); // 无权限访问
                return false;
            }
        } catch (Exception e) {
            // token验证失败，如果是公开路径，仍然允许访问
            if (isAllowedPath || isPublicPath) {
                return true;
            }
            sendErrorResponse(response, 401, MessageConstant.SESSION_EXPIRED); // 令牌无效
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清空ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
