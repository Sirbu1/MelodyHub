package cn.edu.seig.vibemusic.config;

import cn.edu.seig.vibemusic.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 重新启用拦截器，但只拦截需要登录的路径
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/user/**", "/forum/**", "/playlist/**", "/artist/**",
                        "/song/**", "/favorite/**", "/comment/**", "/banner/**"
                )
                .excludePathPatterns(
                        "/user/login", "/user/register", "/user/sendVerificationCode",
                        "/user/resetUserPassword", "/user/test", "/user/noauth/test",
                        "/banner/getBannerList", "/song/getAllOriginalSongs"
                )
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有路径
                .allowedOriginPatterns("*") // 允许所有来源（推荐）
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 加上 OPTIONS
                .allowedHeaders("*") // 允许所有请求头
                .exposedHeaders("Authorization") // 允许前端获取 Authorization 头
                .allowCredentials(true); // 允许携带 Cookie 或 Token
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理器，但不处理API路径
        // 这样可以确保API请求不会被当作静态资源处理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .setCachePeriod(0);
    }
}
