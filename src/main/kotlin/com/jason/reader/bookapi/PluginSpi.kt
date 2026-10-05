package com.jason.reader.bookapi

/**
 * 书源插件 SPI（Service Provider Interface）。
 *
 * 这是宿主（墨伴）与外部书源插件之间的**唯一契约层**：
 * - 纯 Kotlin/JVM，不依赖 Android、okhttp、Room，保证插件可用普通 Kotlin 工程开发；
 * - 插件以 dex-jar/apk 形式发布，打包时对本模块一律 `compileOnly`，
 *   运行时由宿主的 ClassLoader 提供实现（禁止把本模块打进插件，避免单例与类型分裂）；
 * - 契约一旦发布需保持二进制兼容，不兼容变更必须提升 [API_VERSION]。
 *
 * 一个插件包可声明一个入口实现，一个入口可提供多个书源。
 */
interface BookSourcePlugin {

    /** 插件元数据（版本协商与展示用） */
    val metadata: PluginMetadata

    /**
     * 创建本插件提供的全部书源。宿主加载插件时调用一次；
     * 插件应在此返回无状态或自持状态的 [RemoteBookSource]，网络/文件能力一律经 [host]。
     */
    fun createSources(host: PluginHost): List<RemoteBookSource>
}

/** 插件描述信息 */
data class PluginMetadata(
    /** 插件唯一 id（包名风格，如 com.example.reader.source.demo），全局不可重复 */
    val id: String,
    /** 展示名称 */
    val name: String,
    /** 插件版本号（语义化版本，仅展示用） */
    val version: String,
    /** 兼容的最低宿主 API 版本；高于宿主 [API_VERSION] 时宿主拒绝加载 */
    val minApiVersion: Int = API_VERSION,
    val author: String = "",
    val description: String = "",
)

/**
 * 宿主注入给插件的运行环境。插件**不应**自行创建 OkHttpClient、读写插件目录之外的文件，
 * 全部能力经此接口获取，以共享宿主的 Cookie 罐、连接池、磁盘缓存与流量策略。
 */
interface PluginHost {

    /** 当前宿主支持的契约版本 */
    val apiVersion: Int

    /** 统一 HTTP 门面（共享 Cookie、缓存、UA） */
    val http: HttpFacade

    /**
     * 取该插件专属的数据/缓存目录（已创建）。插件只允许在此目录内读写。
     * @param name 子目录名（如 "covers"、"cache"）
     */
    fun cacheDir(name: String = ""): java.io.File

    /** 写日志到宿主日志系统（级别由宿主决定如何落盘/展示） */
    fun log(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * 远程书源：插件提供的在线书库。所有方法均为 suspend，失败以 [Result.failure] 返回
 * （抛异常也会被宿主捕获，但推荐显式 failure 并使用 [SourceException] 语义化异常）。
 *
 * 方法调用链：
 * 搜索/发现 → [SearchBook.detailUrl] → [getBookInfo]/[getChapterList]
 * → [BookChapter.url] → [getChapterContent]。
 */
interface RemoteBookSource {

    /** 书源唯一 id（建议 = 站点主机名，同插件内不可重复） */
    val id: String

    /** 书源展示名 */
    val name: String

    /** 分组标签（可为空，宿主用于书源分组展示） */
    val group: String get() = ""

    /** 关键词搜索（[page] 从 1 开始；无结果返回空 List，不是 failure） */
    suspend fun search(query: SearchQuery): Result<List<SearchBook>>

    /** 发现页分类槽位（如「玄幻/都市/出版」）；不支持发现可返回空 List */
    suspend fun exploreSlots(): Result<List<ExploreSlot>> = Result.success(emptyList())

    /** 发现页某分类的第 [page] 页 */
    suspend fun explore(slot: ExploreSlot, page: Int): Result<List<SearchBook>> =
        Result.failure(SourceException.Unsupported("本书源不支持发现"))

    /** 书籍详情：补全封面/简介等元数据（搜索结果已带全的字段可直接回显） */
    suspend fun getBookInfo(detailUrl: String): Result<BookInfo>

    /** 章节目录（顺序即书籍章节顺序；[BookChapter.url] 为正文页地址） */
    suspend fun getChapterList(detailUrl: String): Result<List<BookChapter>>

    /** 拉取单章正文（[BookChapter] 由本 source 的目录方法产出） */
    suspend fun getChapterContent(
        detailUrl: String, chapter: BookChapter
    ): Result<ChapterContent>
}

/** 当前契约 API 版本，随不兼容变更递增 */
const val API_VERSION: Int = 1
