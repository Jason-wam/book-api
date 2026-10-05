# book-api · AnyReader 书源插件契约层

AnyReader 宿主与外部书源插件之间的**唯一契约**：纯 Kotlin/JVM、零第三方依赖（仅 stdlib），
不依赖 Android / okhttp / Room，插件可用任何普通 Kotlin 工程开发。

## 集成方式

插件工程对本模块**一律 `compileOnly`**——运行时由宿主 ClassLoader 提供实现，
把契约层打进插件包会导致跨 ClassLoader 类型分裂（ClassCastException / 单例分裂），严禁。

```kotlin
// 方式一：JitPack（book-api 仓库打 tag 即发布）
compileOnly("com.github.Jason-wam:book-api:v0.1.0")

// 方式二：源码复合构建（与宿主同仓库联调）
// settings.gradle.kts:
// includeBuild("<path>/modules/book-api")
// build.gradle.kts:
compileOnly("com.jason.reader:book-api:0.1.0")
```

## 核心接口

```
BookSourcePlugin（插件入口，无参构造 + 由 plugin.json 的 entryClass 指定）
 ├─ metadata : PluginMetadata            // id / name / version / minApiVersion
 └─ createSources(host: PluginHost)      // 宿主加载时调用一次，返回书源列表

PluginHost（宿主注入的运行环境，插件的全部能力来源）
 ├─ apiVersion : Int                     // 宿主契约版本
 ├─ http : HttpFacade                    // 统一 HTTP：text()/download()，共享 Cookie/缓存/UA
 ├─ cacheDir(name) : File                // 插件专属目录（插件只允许在此读写）
 └─ log(tag, message, throwable?)        // 日志进宿主日志系统

RemoteBookSource（书源，方法全为 suspend，失败返回 Result.failure）
 search(query) → List<SearchBook>                       // 关键词搜索
 exploreSlots() → List<ExploreSlot>                     // 发现页分类（可缺省）
 explore(slot, page) → List<SearchBook>                 // 分类列表（可缺省）
 getBookInfo(detailUrl) → BookInfo                      // 详情补全
 getChapterList(detailUrl) → List<BookChapter>          // 目录
 getChapterContent(detailUrl, chapter) → ChapterContent // 正文
```

调用链：`search/explore → SearchBook.detailUrl → getBookInfo / getChapterList → BookChapter.url → getChapterContent`。

### 关键 DTO 约定

| 类型 | 要点 |
|---|---|
| `SearchBook` | `detailUrl` 是后续所有方法的入参锚点；列表页字段可留空，由详情页补全 |
| `ExploreSlot.url` | 含 `{{page}}` 占位符（宿主请求前替换），`pageStart` 指定起始页（1 或 0） |
| `BookChapter` | `index` 从 0 开始；`isVolume=true` 的分卷节点 `url` 为空串 |
| `ChapterContent` | `text` 为段落以 `\n` 分隔的纯文本；插图用 `<img src="...">` 内嵌；一章多页时 `nextUrl` 返回下一页，宿主自动续拉 |

### 错误语义

失败一律 `Result.failure(SourceException.Xxx)`，宿主据此展示可读提示：
`RateLimited`（频控退避）/ `LoginRequired` / `ParseFailed`（结构变更、选择器无结果）/
`Network` / `Unsupported`。无结果不是 failure——返回空 List。

## 发布与版本兼容

- JitPack：book-api 仓库打 tag（如 `v0.1.0`）即得 `com.github.Jason-wam:book-api:<tag>`；
  本地调试可 `gradlew publishToMavenLocal`。
- `API_VERSION`（当前 `1`）随**不兼容**契约变更递增；插件在 `plugin.json` / `PluginMetadata`
  声明 `minApiVersion`，宿主低于该值时拒绝加载。兼容性增强（新增带默认值的接口方法、
  新增 DTO 字段）不升版本。

## 硬性规则（宿主加载器依赖这些约定）

1. 契约层 `compileOnly`，禁止打包进插件；
2. 插件入口类必须有无参构造；
3. 网络/文件/日志能力一律经 `host`，禁止自建 OkHttpClient 或全局静态状态；
4. 一个插件包可含一个入口、一个入口可返回多个书源（`id` 在插件内唯一）。
