# 课程讲义编写系统

老师在前端维护**课程 → 章节 → 讲义正文 → 发布状态**，Java 后端把数据读写到本地
JSON 文件；学生端只能浏览和阅读**已发布**的讲义。

- **lesson-backend**：纯 JDK（`com.sun.net.httpserver`）实现的 HTTP 服务，零第三方依赖，
  数据持久化在本地 `data/db.json`，首次启动自动播种示例数据。
- **lesson-frontend**：纯静态 HTML/CSS/JS（无框架、无构建步骤），分老师端与学生端两个页面。

---

## 一、技术栈

| 端 | 技术 | 说明 |
| --- | --- | --- |
| 后端 | Java 11+（推荐 17），仅 JDK 自带 API | 内置 HTTP Server + 手写 JSON 解析/序列化，无需 Maven/Gradle |
| 前端 | 原生 HTML / CSS / JavaScript | 三页：门户、老师端、学生端；Markdown 渲染器为内置轻量实现 |
| 存储 | 单个本地 JSON 文件 | `lesson-backend/data/db.json`，原子写入（临时文件 + rename） |
| 鉴权 | 请求头 `X-Teacher-Token` | 仅老师端接口需要，默认令牌 `teacher-123456` |

---

## 二、目录结构

```
.
├── README.md
├── lesson-backend
│   ├── run.sh                 # 一键编译并启动后端
│   ├── data/                  # 数据目录（db.json 首次运行自动生成，已被 gitignore）
│   └── src/com/lesson
│       ├── Main.java          # 启动入口（端口 / 数据文件 / 令牌可用环境变量覆盖）
│       ├── model
│       │   ├── Course.java    # 课程
│       │   ├── Chapter.java   # 章节（属于课程，sortOrder 排序）
│       │   └── Lecture.java   # 讲义（属于章节，published 控制学生可见性）
│       ├── store
│       │   ├── DataStore.java     # 内存数据 + 读写锁 + 本地 JSON 落盘 + 种子数据 + 级联删除
│       │   └── NotFoundException.java
│       ├── util
│       │   └── Json.java      # 零依赖 JSON 解析器 / 序列化器
│       └── web
│           ├── ApiHandler.java    # 路由、CORS、老师鉴权、参数校验、统一错误
│           └── ApiException.java  # 带 HTTP 状态码的业务异常
└── lesson-frontend
    ├── index.html             # 门户入口（选择老师端 / 学生端）
    ├── teacher.html           # 老师端：课程/章节/讲义维护 + Markdown 编辑预览 + 发布
    ├── student.html           # 学生端：只读浏览已发布讲义
    ├── css/style.css
    └── js
        ├── api.js             # fetch 封装，老师接口自动带 X-Teacher-Token
        ├── markdown.js        # 轻量 Markdown 渲染（含 HTML 转义，防 XSS）
        ├── teacher.js         # 老师端逻辑
        └── student.js         # 学生端逻辑
```

---

## 三、快速启动

### 1. 启动后端（端口 8080）

前置要求：安装 JDK 11 或更高版本（`java -version` 可用）。

```bash
cd lesson-backend
./run.sh
```

`run.sh` 会先 `javac` 编译到 `out/`，再启动服务。看到下面的输出即成功：

```
课程讲义系统后端已启动
 监听地址 : http://localhost:8080
 数据文件 : .../lesson-backend/data/db.json
 老师令牌 : teacher-123456（请求头 X-Teacher-Token）
 健康检查 : GET /api/health
```

手工方式（不用脚本）：

```bash
cd lesson-backend
javac -encoding UTF-8 -d out $(find src -name '*.java')
cd out && java com.lesson.Main
```

可用环境变量覆盖默认配置：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PORT` | `8080` | 监听端口 |
| `DATA_FILE` | `data/db.json`（相对运行目录） | 本地数据文件路径 |
| `TEACHER_TOKEN` | `teacher-123456` | 老师令牌 |

示例：`PORT=9090 TEACHER_TOKEN=my-secret ./run.sh`

### 2. 启动前端（端口 8000）

前端是纯静态文件，任意静态服务器均可：

```bash
cd lesson-frontend
python3 -m http.server 8000
```

浏览器打开：

- 门户入口：<http://localhost:8000/index.html>
- 老师端：<http://localhost:8000/teacher.html>
- 学生端：<http://localhost:8000/student.html>

> 也可以直接双击 HTML 文件用 `file://` 打开；只要后端仍在 `localhost:8080` 即可联调
> （后端已开启 CORS，允许任意来源）。

### 3. 在页面上配置后端地址 / 令牌

- 老师端、学生端顶栏都有「后端地址」输入框，默认 `http://localhost:8080`，
  修改后保存在浏览器 `localStorage`，换端口/反代时改这里即可，无需改代码重启。
- 老师端顶栏「令牌」输入框默认填 `teacher-123456`，同样保存在本地。

---

## 四、示例数据

首次启动且数据文件不存在时，后端自动写入两份课程作为示例，便于直接联调：

| 课程 | 章节 | 讲义 | 状态 |
| --- | --- | --- | --- |
| Java 程序设计入门 | 第一章 课程导论 | 1.1 为什么学习 Java | ✅ 已发布 |
| Java 程序设计入门 | 第二章 开发环境 | 2.1 安装 JDK 与第一个程序 | ✅ 已发布 |
| Java 程序设计入门 | 第三章 面向对象基础 | 3.1 类与对象（草稿） | ⬜ 草稿（学生不可见） |
| 高等数学（上） | 第一章 极限与连续 | 1.1 数列的极限 | ✅ 已发布 |

示例讲义正文是 Markdown，覆盖标题、列表、引用、行内代码、代码块等语法。

**重置示例数据**：停止后端后删除数据文件再启动即可。

```bash
rm lesson-backend/data/db.json
# 重新启动后端，会再次播种
```

---

## 五、接口说明（联调清单）

所有响应均为 JSON。错误响应格式：`{"error": "错误描述", "status": 400}`。
老师端接口必须带请求头 `X-Teacher-Token: teacher-123456`。

### 公共

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查 |
| OPTIONS | 任意路径 | CORS 预检，返回 204 |

### 学生端（公开，只读）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/student/courses` | 课程列表（**只返回含有已发布讲义的课程**） |
| GET | `/api/student/courses/{courseId}` | 课程详情：课程、章节、**已发布**讲义（列表不含正文）；无已发布讲义的章节自动隐藏 |
| GET | `/api/student/lectures/{lectureId}` | 读取一篇**已发布**讲义的正文；草稿返回 404 |

### 老师端（需令牌）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/teacher/courses` | 全部课程 |
| POST | `/api/teacher/courses` | 新建课程，body：`{"title","description"}` |
| GET | `/api/teacher/courses/{courseId}` | 课程详情（含全部章节与讲义，**含草稿与正文**） |
| PUT | `/api/teacher/courses/{courseId}` | 修改课程标题/简介 |
| DELETE | `/api/teacher/courses/{courseId}` | 删除课程（**级联删除章节与讲义**） |
| POST | `/api/teacher/courses/{courseId}/chapters` | 新建章节，body：`{"title"}` |
| PUT | `/api/teacher/chapters/{chapterId}` | 重命名章节 |
| DELETE | `/api/teacher/chapters/{chapterId}` | 删除章节（级联删除其讲义） |
| POST | `/api/teacher/courses/{courseId}/lectures` | 新建讲义，body：`{"chapterId","title","content"}` |
| GET | `/api/teacher/lectures/{lectureId}` | 读取讲义（含正文，草稿也可） |
| PUT | `/api/teacher/lectures/{lectureId}` | 保存标题/正文，body：`{"title","content"}` |
| PATCH | `/api/teacher/lectures/{lectureId}/publish` | 发布/撤销，body：`{"published": true/false}` |
| DELETE | `/api/teacher/lectures/{lectureId}` | 删除讲义 |

数据对象字段：

- Course：`id, title, description, createdAt, updatedAt`
- Chapter：`id, courseId, title, sortOrder, createdAt, updatedAt`
- Lecture：`id, courseId, chapterId, title, content, published, sortOrder, createdAt, updatedAt, publishedAt`

状态码：200 成功 / 201 创建成功 / 400 参数错误 / 401 令牌缺失或错误 /
404 资源不存在或学生访问未发布讲义 / 405 方法不允许 / 500 服务器错误。

---

## 六、用 curl 联调

```bash
# 健康检查
curl http://localhost:8080/api/health

# 学生端：课程列表（无需令牌）
curl http://localhost:8080/api/student/courses

# 学生端：读取已发布讲义正文
curl http://localhost:8080/api/student/lectures/le-welcome

# 不带令牌访问老师接口 -> 401
curl -i http://localhost:8080/api/teacher/courses

# 老师端：课程列表
curl -H "X-Teacher-Token: teacher-123456" \
     http://localhost:8080/api/teacher/courses

# 新建课程
curl -X POST http://localhost:8080/api/teacher/courses \
     -H "X-Teacher-Token: teacher-123456" -H "Content-Type: application/json" \
     -d '{"title":"数据结构","description":"链表、树与图"}'

# 发布讲义（true=发布，false=撤销）
curl -X PATCH http://localhost:8080/api/teacher/lectures/le-oop-draft/publish \
     -H "X-Teacher-Token: teacher-123456" -H "Content-Type: application/json" \
     -d '{"published": true}'
```

典型业务闭环（已在联调中验证）：

1. 老师新建课程 → 学生端课程列表**看不到**（没有任何已发布讲义）；
2. 老师新建章节、新建讲义（默认草稿）→ 学生端访问该讲义返回 **404**；
3. 老师编辑正文并「发布」→ 学生端课程、目录、正文**立即可见**；
4. 老师「撤销发布」→ 学生端再次 **404**；
5. 删除章节/课程会级联删除其下讲义。

---

## 七、实现说明与约定

- **并发**：后端用 `ReentrantReadWriteLock` 保护内存数据，读多写少；每次写操作后
  将全量数据原子落盘（先写 `.tmp` 再 rename），避免半写文件损坏数据。
- **排序**：章节、讲义按 `sortOrder`（创建时自增）再按创建时间升序。
- **发布语义**：讲义默认 `published=false`（草稿）；首次发布记录 `publishedAt`，
  撤销发布时清零。学生端只暴露 `published=true` 的内容。
- **安全**：Markdown 渲染前做 HTML 转义，讲义内容中的 `<script>` 不会被执行。
- **CORS**：开发环境下 `Access-Control-Allow-Origin: *`，方便前端跨端口调用。
- **ID**：课程 `c-xxx`、章节 `ch-xxx`、讲义 `le-xxx`，后缀为随机 UUID 片段。
