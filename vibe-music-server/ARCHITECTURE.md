# Vibe Music Server 体系结构逻辑模型

## 项目概述
Vibe Music Server 采用经典的四层架构模式：表现层、业务逻辑层、数据访问层、数据存储层。

---

## 体系结构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        表现层 (Controller Layer)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │   User   │ │   Song   │ │ Playlist │ │  Artist  │  ...    │
│  │Controller│ │Controller│ │Controller│ │Controller│          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                   业务逻辑层 (Service Layer)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │   User   │ │   Song   │ │ Playlist │ │  Artist  │  ...    │
│  │ Service  │ │ Service  │ │ Service  │ │ Service  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│                                                                  │
│  ┌─────────────────────────────────────────────────────┐       │
│  │           辅助服务 (Auxiliary Services)              │       │
│  │  EmailService | MinioService | RedisCache           │       │
│  └─────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                   数据访问层 (Data Access Layer)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │   User   │ │   Song   │ │ Playlist │ │  Artist  │  ...    │
│  │  Mapper  │ │  Mapper  │ │  Mapper  │ │  Mapper  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐                              │
│  │ MyBatis-Plus│ │  MyBatis-XML│                              │
│  │   BaseMapper│ │   CustomSQL │                              │
│  └─────────────┘ └─────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                   数据存储层 (Data Storage Layer)                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐          │
│  │    MySQL     │ │    Redis     │ │    MinIO     │          │
│  │  关系数据库   │ │   缓存/会话   │ │  对象存储     │          │
│  └──────────────┘ └──────────────┘ └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 一、表现层 (Presentation Layer / Controller Layer)

**包路径**: `cn.edu.seig.vibemusic.controller`

**职责**: 
- 接收HTTP请求
- 参数验证
- 调用业务逻辑层
- 返回HTTP响应

### 1.1 Controller列表

| Controller | 主要功能 | 映射路径 |
|-----------|---------|---------|
| **UserController** | 用户相关接口 | `/user` |
| **SongController** | 歌曲相关接口 | `/song` |
| **PlaylistController** | 歌单相关接口 | `/playlist` |
| **ArtistController** | 歌手相关接口 | `/artist` |
| **CommentController** | 评论相关接口 | `/comment` |
| **UserFavoriteController** | 收藏相关接口 | `/favorite` |
| **BannerController** | 轮播图相关接口 | `/banner` |
| **FeedbackController** | 反馈相关接口 | `/feedback` |
| **ForumController** | 论坛相关接口 | `/forum` |
| **AdminController** | 管理员相关接口 | `/admin` |
| **PlaylistBindingController** | 歌单绑定相关接口 | `/playlistBinding` |
| **GenreController** | 流派相关接口 | `/genre` |
| **StyleController** | 风格相关接口 | `/style` |

### 1.2 示例代码结构

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private IUserService userService;  // 依赖业务逻辑层
    
    @PostMapping("/register")
    public Result register(@Valid @RequestBody UserRegisterDTO dto) {
        // 1. 参数验证（通过@Valid注解）
        // 2. 调用业务逻辑层
        return userService.register(dto);
    }
}
```

### 1.3 拦截器 (Interceptor)

**位置**: `cn.edu.seig.vibemusic.interceptor.LoginInterceptor`

**功能**:
- JWT Token验证
- 权限控制
- 公开路径放行

**配置**: `cn.edu.seig.vibemusic.config.WebConfig`

---

## 二、业务逻辑层 (Business Logic Layer / Service Layer)

**包路径**: 
- 接口: `cn.edu.seig.vibemusic.service`
- 实现: `cn.edu.seig.vibemusic.service.impl`

**职责**:
- 业务逻辑处理
- 事务管理
- 数据转换 (DTO ↔ Entity ↔ VO)
- 调用数据访问层
- 缓存管理

### 2.1 Service接口列表

| Service接口 | 实现类 | 主要业务功能 |
|------------|--------|------------|
| **IUserService** | UserServiceImpl | 用户注册、登录、信息管理 |
| **ISongService** | SongServiceImpl | 歌曲CRUD、查询、推荐 |
| **IPlaylistService** | PlaylistServiceImpl | 歌单CRUD、推荐 |
| **IArtistService** | ArtistServiceImpl | 歌手CRUD、查询 |
| **ICommentService** | CommentServiceImpl | 评论CRUD |
| **IUserFavoriteService** | UserFavoriteServiceImpl | 收藏管理 |
| **IBannerService** | BannerServiceImpl | 轮播图管理 |
| **IFeedbackService** | FeedbackServiceImpl | 反馈管理 |
| **IForumPostService** | ForumPostServiceImpl | 论坛帖子管理 |
| **IForumReplyService** | ForumReplyServiceImpl | 论坛回复管理 |
| **IAdminService** | AdminServiceImpl | 管理员登录、管理 |
| **IAuditService** | AuditServiceImpl | 审核服务 |
| **IPlaylistBindingService** | PlaylistBindingServiceImpl | 歌单歌曲绑定 |
| **IGenreService** | GenreServiceImpl | 流派管理 |
| **IStyleService** | StyleServiceImpl | 风格管理 |

### 2.2 辅助服务 (Auxiliary Services)

| Service | 功能 | 使用的存储 |
|---------|------|----------|
| **EmailService** | 发送邮件验证码 | - |
| **MinioService** | 文件上传/下载 | MinIO |
| **Redis Cache** | 数据缓存 | Redis |

### 2.3 示例代码结构

```java
@Service
@CacheConfig(cacheNames = "userCache")  // 配置缓存
public class UserServiceImpl extends ServiceImpl<UserMapper, User> 
    implements IUserService {
    
    @Autowired
    private UserMapper userMapper;  // 依赖数据访问层
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;  // Redis操作
    
    @Override
    @CacheEvict(cacheNames = "userCache", allEntries = true)
    public Result register(UserRegisterDTO dto) {
        // 1. 业务逻辑处理
        // 2. 调用Mapper进行数据持久化
        // 3. Redis缓存操作
        // 4. 返回结果
    }
}
```

### 2.4 缓存策略

- **@Cacheable**: 查询结果缓存
- **@CacheEvict**: 更新/删除时清除缓存
- **缓存命名空间**: songCache, playlistCache, artistCache, userCache等

---

## 三、数据访问层 (Data Access Layer / Mapper Layer)

**包路径**: 
- 接口: `cn.edu.seig.vibemusic.mapper`
- XML映射: `src/main/resources/mapper`

**职责**:
- 数据库CRUD操作
- SQL语句执行
- ORM映射

### 3.1 Mapper接口列表

| Mapper接口 | XML映射文件 | 对应实体 |
|-----------|------------|---------|
| **UserMapper** | UserMapper.xml | User |
| **SongMapper** | SongMapper.xml | Song |
| **PlaylistMapper** | PlaylistMapper.xml | Playlist |
| **ArtistMapper** | ArtistMapper.xml | Artist |
| **CommentMapper** | CommentMapper.xml | Comment |
| **UserFavoriteMapper** | UserFavoriteMapper.xml | UserFavorite |
| **BannerMapper** | BannerMapper.xml | Banner |
| **FeedbackMapper** | FeedbackMapper.xml | Feedback |
| **ForumPostMapper** | ForumPostMapper.xml | ForumPost |
| **ForumReplyMapper** | ForumReplyMapper.xml | ForumReply |
| **AdminMapper** | AdminMapper.xml | Admin |
| **PlaylistBindingMapper** | PlaylistBindingMapper.xml | PlaylistBinding |
| **GenreMapper** | GenreMapper.xml | Genre |
| **StyleMapper** | StyleMapper.xml | Style |

### 3.2 技术栈

- **MyBatis-Plus**: 
  - 提供BaseMapper接口，实现基础CRUD
  - 自动分页
  - 条件构造器 (QueryWrapper)

- **MyBatis XML**: 
  - 复杂查询SQL
  - 自定义结果映射
  - 关联查询

### 3.3 示例代码结构

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus提供的基础方法
    // selectById, insert, update, delete等
    
    // 自定义方法（在XML中实现）
    List<UserVO> selectUserWithDetails(@Param("userId") Long userId);
}
```

```xml
<!-- UserMapper.xml -->
<mapper namespace="cn.edu.seig.vibemusic.mapper.UserMapper">
    <select id="selectUserWithDetails" resultType="UserVO">
        SELECT * FROM tb_user WHERE user_id = #{userId}
    </select>
</mapper>
```

### 3.4 配置

**位置**: `cn.edu.seig.vibemusic.config.MyBatisPlusConfig`

---

## 四、数据存储层 (Data Storage Layer)

**职责**: 持久化数据存储

### 4.1 MySQL (关系数据库)

**用途**: 核心业务数据持久化

**配置**: `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vibe_music
    username: root
    password: 3127
```

**存储的数据**:
- 用户信息 (tb_user)
- 歌曲信息 (tb_song)
- 歌单信息 (tb_playlist)
- 歌手信息 (tb_artist)
- 评论信息 (tb_comment)
- 收藏信息 (tb_user_favorite)
- 等等...

**连接池**: Druid

### 4.2 Redis (缓存/会话存储)

**用途**: 
1. JWT Token存储和验证
2. 邮箱验证码临时存储
3. 数据查询结果缓存

**配置**: 
- `application.yml`: 连接配置
- `RedisConfig.java`: 序列化配置、缓存管理器

**数据结构**:
- **String**: Token、验证码
- **Hash**: 缓存对象 (通过Spring Cache自动管理)

**主要操作**:
```java
// Token存储
stringRedisTemplate.opsForValue().set(token, token, 6, TimeUnit.HOURS);

// 验证码存储
stringRedisTemplate.opsForValue().set("verificationCode:" + email, code, 5, TimeUnit.MINUTES);

// 缓存操作（通过@Cacheable注解自动完成）
```

### 4.3 MinIO (对象存储)

**用途**: 文件存储（音乐文件、图片）

**配置**: `application.yml`
```yaml
minio:
  endpoint: http://127.0.0.1:9000
  accessKey: minioadmin
  secretKey: minioadmin
  bucket: vibe-music-data
```

**存储内容**:
- 音乐文件 (.mp3)
- 用户头像
- 歌曲封面
- 歌单封面
- 轮播图
- 等等...

**服务**: `MinioService`

---

## 五、数据传输对象 (DTO/VO/Entity)

### 5.1 实体类 (Entity)

**包路径**: `cn.edu.seig.vibemusic.model.entity`

**作用**: 对应数据库表结构

| Entity | 对应表 |
|--------|--------|
| User | tb_user |
| Song | tb_song |
| Playlist | tb_playlist |
| Artist | tb_artist |
| Comment | tb_comment |
| UserFavorite | tb_user_favorite |
| Banner | tb_banner |
| Feedback | tb_feedback |
| ForumPost | tb_forum_post |
| ForumReply | tb_forum_reply |
| Admin | tb_admin |

### 5.2 数据传输对象 (DTO)

**包路径**: `cn.edu.seig.vibemusic.model.dto`

**作用**: 接收前端请求数据

**命名规则**: 
- 查询: `XxxDTO` (如 `UserDTO`, `SongDTO`)
- 新增: `XxxAddDTO` (如 `SongAddDTO`)
- 更新: `XxxUpdateDTO` (如 `SongUpdateDTO`)
- 特定操作: `XxxLoginDTO`, `XxxRegisterDTO` 等

### 5.3 视图对象 (VO)

**包路径**: `cn.edu.seig.vibemusic.model.vo`

**作用**: 返回给前端的数据

| VO | 说明 |
|----|------|
| UserVO | 用户视图对象 |
| SongVO | 歌曲视图对象 |
| SongDetailVO | 歌曲详情视图对象 |
| PlaylistVO | 歌单视图对象 |
| ArtistVO | 歌手视图对象 |
| CommentVO | 评论视图对象 |

---

## 六、配置层 (Configuration Layer)

**包路径**: `cn.edu.seig.vibemusic.config`

| 配置类 | 功能 |
|-------|------|
| **RedisConfig** | Redis连接、序列化、缓存管理 |
| **MyBatisPlusConfig** | MyBatis-Plus分页插件 |
| **MinioConfig** | MinIO客户端配置 |
| **WebConfig** | Web配置、拦截器注册 |
| **CorsConfig** | 跨域配置 |
| **MailConfig** | 邮件服务配置 |
| **RolePermissionManager** | 角色权限管理 |

---

## 七、工具层 (Utility Layer)

**包路径**: `cn.edu.seig.vibemusic.util`

| 工具类 | 功能 |
|-------|------|
| **JwtUtil** | JWT Token生成和解析 |
| **ThreadLocalUtil** | 线程局部变量管理 |
| **BindingResultUtil** | 参数验证结果处理 |
| **TypeConversionUtil** | 类型转换 |
| **RandomCodeUtil** | 随机码生成 |

---

## 八、异常处理层 (Exception Handler)

**包路径**: `cn.edu.seig.vibemusic.handler`

**GlobalExceptionHandler**: 全局异常处理
- 统一异常响应格式
- SQL异常处理
- 参数校验异常处理

---

## 九、数据流转示例

### 示例1: 用户注册流程

```
1. 前端请求 → UserController.register()
   ↓
2. Controller验证参数 → 调用 IUserService.register()
   ↓
3. Service处理业务逻辑:
   - 验证验证码 (从Redis读取)
   - 检查用户名/邮箱唯一性 (调用UserMapper查询)
   - 密码加密
   - 保存用户 (调用UserMapper.insert())
   - 删除验证码 (Redis删除)
   - 清除用户缓存 (@CacheEvict)
   ↓
4. Mapper执行SQL → MySQL数据库
   ↓
5. Service返回Result → Controller返回HTTP响应
```

### 示例2: 查询歌曲列表（带缓存）

```
1. 前端请求 → SongController.getAllSongs()
   ↓
2. Controller → 调用 ISongService.getAllSongs()
   ↓
3. Service方法（带@Cacheable注解）:
   - 检查Redis缓存
   - 缓存命中 → 直接返回缓存数据
   - 缓存未命中:
     a. 调用SongMapper查询数据库
     b. 处理数据转换 (Entity → VO)
     c. 存入Redis缓存
     d. 返回结果
   ↓
4. Mapper查询MySQL → 返回Song实体列表
   ↓
5. Service转换为VO → Controller返回HTTP响应
```

---

## 十、技术栈总结

| 层级 | 技术/框架 |
|------|----------|
| **表现层** | Spring MVC (@RestController, @RequestMapping) |
| **业务层** | Spring Service, Spring Cache, Redis |
| **数据访问层** | MyBatis-Plus, MyBatis XML |
| **数据存储层** | MySQL, Redis, MinIO |
| **其他** | JWT, Lombok, Druid连接池 |

---

## 十一、依赖关系图

```
Controller
    ↓ 依赖
Service (接口)
    ↓ 实现
ServiceImpl
    ↓ 依赖
Mapper (接口)
    ↓ 实现
MyBatis-Plus / MyBatis XML
    ↓ 操作
MySQL Database

ServiceImpl
    ↓ 使用
RedisTemplate / StringRedisTemplate
    ↓ 操作
Redis

ServiceImpl
    ↓ 使用
MinioService
    ↓ 操作
MinIO Object Storage
```

---

## 十二、包结构完整树

```
cn.edu.seig.vibemusic
│
├── controller/          # 表现层 - 控制器
│   ├── UserController.java
│   ├── SongController.java
│   └── ...
│
├── service/            # 业务逻辑层 - 接口
│   ├── IUserService.java
│   ├── ISongService.java
│   └── impl/          # 业务逻辑层 - 实现
│       ├── UserServiceImpl.java
│       ├── SongServiceImpl.java
│       └── ...
│
├── mapper/            # 数据访问层 - Mapper接口
│   ├── UserMapper.java
│   ├── SongMapper.java
│   └── ...
│
├── model/            # 数据传输对象
│   ├── entity/      # 实体类 (对应数据库表)
│   ├── dto/         # 数据传输对象 (接收请求)
│   └── vo/          # 视图对象 (返回响应)
│
├── config/          # 配置层
│   ├── RedisConfig.java
│   ├── MyBatisPlusConfig.java
│   └── ...
│
├── interceptor/     # 拦截器
│   └── LoginInterceptor.java
│
├── handler/         # 异常处理
│   └── GlobalExceptionHandler.java
│
├── util/           # 工具类
│   ├── JwtUtil.java
│   └── ...
│
├── constant/       # 常量
│   └── ...
│
└── enumeration/    # 枚举
    └── ...
```

---

**文档版本**: 1.0  
**最后更新**: 2025-12-23

