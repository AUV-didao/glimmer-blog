# 微光博客（Glimmer Blog）· 个人博客网站

《Web 服务器端开发》期中大作业。基于 **Servlet + JSP + JDBC**，不使用任何 MVC 框架，
把课程要求的七项技术（Servlet、Cookie/Session、JSP、Filter、Listener、文件上传下载、数据库编程）
全部落到一个能真实使用的博客站点上。

- 站点名：微光博客　|　工程名：blog　|　上下文路径：`/blog`
- 运行环境：JDK 8+（本项目用 JDK 21 编译，`-source/-target 8`）、Tomcat 9、SQLite 3（通过 JDBC，数据库为单文件）
- 演示账号：`admin / 123456`（管理员）、`lin / 123456`（普通用户）

## 一、功能清单

| 模块 | 功能 | 对应技术 |
|---|---|---|
| 用户 | 注册（服务端校验 + 加盐散列）、登录、退出、会话保持 | Servlet、Session |
| 用户 | **记住我 / 自动登录**（自签名持久 Cookie，7 天免登录） | **Cookie（显式读写）** + Filter |
| 用户 | 个人资料修改、头像上传 | Servlet + multipart/form-data |
| 博客 | 发表文章、编辑、删除、草稿/发布状态 | Servlet + JDBC |
| 博客 | 文章列表（分页）、详情、浏览量统计 | JSP + EL + JSTL、JDBC |
| 博客 | 分类筛选、标签（多对多）、关键词搜索（标题/摘要/正文） | JDBC 动态 SQL |
| 互动 | 发表评论、**二级回复（评论的评论）**、删除评论、点赞/取消点赞（AJAX） | Servlet + fetch + JSON |
| 互动 | **站内通知中心**：被评论/被回复提醒、未读数徽标、标记已读 | Filter + JDBC + 业务层 |
| 权限 | 登录拦截、管理员权限拦截（后台） | Filter |
| 统计 | 在线人数、累计访问量（PV）、站点统计数字 | Listener（Session / Request / Context） |
| 文件 | 文章导出为 txt 下载、上传文件下载（含路径穿越防护） | Servlet 输出流 |
| 后台 | 站点概览、全部文章管理、用户列表、最新评论 | JSP + JDBC + Filter |

## 二、目录结构

```
blog/
├── pom.xml                       Maven war 打包配置（servlet-api / jstl / sqlite-jdbc）
├── data/blog.db                  当前 SQLite 数据库（8 张表 + 现有演示数据）
├── sql/blog.sql                  迁移前 MySQL 历史脚本（不用于当前运行）
├── sql/blog-seed.db              SQLite 可复位种子数据库
├── scripts/                      自动化脚本：环境自检、HTTP 回归、SQLite 查询与数据库复位
├── src/test/java/                JUnit 5 单元测试（21 条，纯逻辑、不依赖容器）
└── src/main/
    ├── java/com/example/blog/
    │   ├── bean/                 实体：User、Post、Comment、Notification、Category、Tag
    │   ├── dao/                  数据访问：UserDao、PostDao、CommentDao、NotificationDao、CategoryDao、TagDao
    │   ├── service/              业务规则：NoticeService（谁该收到通知）、CommentService（评论 + 通知同事务）
    │   ├── util/                 DBUtil、PasswordUtil、UploadUtil、WebUtil、PageBean、Const、Logs、RememberMeUtil、CsrfUtil
    │   └── web/                  Servlet：登录注册、发文评论、通知、点赞、上传下载、后台
    │       ├── filter/           Encoding、AutoLogin、NoticeBadge、Auth、Csrf、Admin（顺序写在 web.xml）
    │       └── listener/         AppInitListener、OnlineUserListener、PageViewListener、LogInitListener
    ├── resources/db.properties   数据库连接配置（改配置不用改代码）
    └── webapp/
        ├── assets/style.css      全站样式（CSS 变量 + 深色模式）
        ├── upload/avatar|cover/  头像与封面图（运行时创建）
        └── WEB-INF/
            ├── web.xml           6 个 Filter 的顺序、会话超时、Cookie HttpOnly、统一错误页
            └── views/            JSP 视图（放在 WEB-INF 下，浏览器无法直接访问）
                ├── common/       taglib.jspf、head.jspf、foot.jspf
                ├── home.jsp、notice.jsp、error.jsp
                ├── auth/         login.jsp、register.jsp
                ├── post/         detail.jsp、edit.jsp
                ├── user/         profile.jsp
                └── admin/        index.jsp
```

命名规则：类名大驼峰、方法小驼峰、常量全大写；Servlet 以 `XxxServlet` 结尾，
DAO 以 `XxxDao` 结尾，视图目录按业务模块分（auth / post / user / admin）。

## 三、启动步骤

### 1. 准备数据库

项目已自带迁移后的 `data/blog.db`，不需要安装或启动 MySQL/PostgreSQL 等数据库服务。首次克隆或需要恢复演示数据时执行：

```powershell
pwsh -File scripts/reset-db.ps1 -Force
```

默认 JDBC 地址写在 `src/main/resources/db.properties`。换电脑或部署目录时，推荐在 Tomcat JVM 参数中设置
`-Dblog.db.path=D:/your/path/blog/data/blog.db`，也可以设置环境变量 `BLOG_DB_PATH`；两者都无需改 Java 代码，
且 JVM 参数优先于环境变量。复位脚本会覆盖数据库文件，执行前应先停止正在使用该文件的 Tomcat。

### 2. 部署到 Tomcat 9（IDEA）

1. `File → Open` 选 `blog/pom.xml`，等 Maven 依赖下载完；
2. `Run → Edit Configurations → + → Tomcat Server → Local`：
   - HTTP port：**8082**（8080/8081 留给其它工程）
   - Deployment：`+ → Artifact → blog:war exploded`
   - Application context：**`/blog`**
3. 启动后访问 <http://localhost:8082/blog/>

> 用 `war` 包部署时，`ServletContext.getRealPath()` 会返回 null，上传功能会提示
> 「应用未解压部署」，所以必须用 **war exploded** 或让 Tomcat 自动解压。

### 3. 命令行打包（可选）

```bash
mvn -f blog/pom.xml clean test        # 只跑单元测试（21 条）
mvn -f blog/pom.xml clean package     # 编译 + 21 条测试 + 产物 target/blog.war
```

### 4. 自动化脚本（验证 / 演示都能用）

```powershell
pwsh -File scripts/check-env.ps1        # 自检：JDK / 工程文件 / SQLite 完整性 / 应用访问
pwsh -File scripts/test-http.ps1        # HTTP 回归：63 条用例（含权限、安全与数据库一致性负例）
pwsh -File scripts/reset-db.ps1 -Force  # 用 sql/blog-seed.db 复位 data/blog.db
```

脚本使用 Python 标准库 `sqlite3` 做只读断言与数据库复位，不依赖 `sqlite3.exe` 或数据库服务。
`test-http.ps1` 会先 GET 首页取得 CSRF 令牌再提交表单，覆盖登录 / 发文 / 评论 / 点赞 / 上传 /
下载 / 管理员跨作者编辑删除 / 草稿导出权限 / 危险封面地址 / 非法数字 ID / 点赞计数一致性 /
记住我伪造 / 通知越权 / CSRF 负例等 63 项；任一断言失败即返回非零，跑完先停 Tomcat，再用复位脚本恢复种子数据。

## 四、实现要点（评审时可以直接对着讲）

1. **三层结构**：JSP 只管显示、Servlet 只管流程、DAO 只管 SQL，实体在层间传递；
   JSP 全部放在 `/WEB-INF/views` 下，只能由 Servlet 转发，避免绕过流程直接访问页面。
2. **会话管理**：登录成功后把 User 放进 `HttpSession`，并调用 `changeSessionId()`
   防会话固定攻击；`web.xml` 里 Cookie 加了 `HttpOnly`，会话 120 分钟过期。
3. **过滤器**：`EncodingFilter` 统一 UTF-8；`AuthFilter` 拦未登录（AJAX 请求返回 401 而不是重定向）；
   `AdminFilter` 拦非管理员（返回 403）。
4. **监听器**：`AppInitListener` 启动自检 + 预热统计 + 建上传目录；
   `OnlineUserListener` 用 `AtomicInteger` 原子统计在线访客，并用 `CopyOnWriteArrayList` 维护在线会员；
   `PageViewListener` 统计 PV 并排除静态资源。
5. **文件上传**：`@MultipartConfig` 限制 5MB，用 UUID 重命名 + 后缀白名单，
   数据库只存路径；下载时用 `filename*=UTF-8''` 处理中文文件名。
6. **安全**：密码 `SHA-256(salt + 明文)`、所有 SQL 使用占位符、JSP 属性用 `c:out` 转义，
   `safeImageUrl` 仅允许本站封面目录与 HTTP(S)，另有开放重定向与下载路径越界防护。
7. **分页**：`PageBean` 统一管理页码/总数/上下页，`PostDao.search` 用同一份 WHERE 条件
   分别 COUNT 与 LIMIT，保证总数与列表一致。
8. **Cookie 显式编程（记住我）**：`RememberMeUtil` 自己 `new Cookie` 写响应、自己从
   `request.getCookies()` 读回来；令牌是 `userId.expireAt.HMAC-SHA256`，签名密钥掺了该用户的
   密码盐，所以改一个字符就验不过、用户改密码旧令牌立即失效；`AutoLoginFilter` 验签通过后
   `changeSessionId()` 重建登录态并滑动续期 7 天。
9. **六个 Filter 的顺序写在 `web.xml` 里**（不用注解）：编码 → 记住我自动登录 → 未读徽标 →
   登录拦截 → **CSRF 校验** → 管理员权限。注解注册的顺序由容器扫描决定，多个过滤器有依赖时不可控；
   CSRF 排在登录拦截之后，是为了让未登录的 AJAX 仍然拿到 401（前端据此跳登录）而不是 403。
10. **完整业务流程（二级回复 + 站内通知）**：评论落库 → `NoticeService` 按规则决定通知谁
    （回复通知被回复者、评论通知作者；自己人不通知；同一动作只发一条）→ 对方页头铃铛未读数 +1
    → 通知中心未读优先展示 → 标记已读（只收 POST，避免 GET 被图片地址刷掉未读）。
11. **统一日志**：`Logs` 把 JUL 接到 `catalina.base/logs/blog-0.log`（2MB×5 滚动、UTF-8），
    `BaseServlet.fail()` 先记 SEVERE 日志再包装成 `ServletException`，出错能直接 grep 日志。
12. **单元测试**：`src/test/java` 下 21 条 JUnit 5 用例覆盖密码散列、Cookie 令牌、参数工具、
    封面 URL 白名单与 CSRF 令牌；HTTP 层另有 63 项真实 Tomcat + SQLite 回归。
13. **CSRF 防护（所有写操作都要令牌）**：`CsrfUtil` 首次访问生成 32 字节随机令牌存进 Session，
    `head.jspf` 用 `<meta name="csrf-token">` 输出，14 处 POST 表单统一注入 `_csrf` 隐藏域，
    AJAX 走 `X-CSRF-Token` 请求头；`CsrfFilter` 对 GET 只发令牌，对 POST/PUT/DELETE/PATCH 用
    `MessageDigest.isEqual` 定长比较，不匹配直接 403（AJAX 回 JSON、普通请求回错误页）。
    这里刻意**不用 Cookie 双提交**：那样没带字段的 POST 也会被放行，只认表单字段/请求头才叫
    「忘了加令牌就一定失败」。退出登录也从 GET 改成 POST，避免链接被预取就掉线。
14. **事务与数据一致性**：发文（文章 + 标签）、评论（评论 + 通知）及点赞（关联行 + 计数）均在
    单连接显式事务中完成；SQLite 点赞切换方法串行化执行，并依靠事务与唯一主键保持计数一致，异常整体回滚。
15. **数据级权限**：普通更新/删除 SQL 带作者 ID；管理员走显式 DAO 分支；草稿只允许作者或管理员预览、导出。

## 五、已知限制

- 正文按纯文本存储并用 `white-space: pre-wrap` 展示，不支持富文本/Markdown（避免 XSS，也够用）；
- 没有引入连接池，每次 DAO 调用开关一个连接（课程规模足够，换 DataSource 时 DAO 不用改）；
- 上传文件存在部署目录下，重新部署 war 会丢失（生产应放独立目录或对象存储）。
- 密码目前是加盐 SHA-256（未上 PBKDF2/bcrypt），记住我签名密钥是代码常量，生产环境应外置；
- 通知中心一次取全量、未做分页（数据量还小），上万条时应改成按页查询。