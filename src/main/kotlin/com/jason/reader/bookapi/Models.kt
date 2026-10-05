package com.jason.reader.bookapi

/**
 * 数据传输对象（DTO）：全部为纯 Kotlin data class，不允许出现 Android / Room / okhttp 类型，
 * 保证插件与宿主跨 ClassLoader 传递时类型唯一（类由宿主加载，插件不打包契约层）。
 */

/** 搜索请求 */
data class SearchQuery(
    /** 关键词（宿主已做空白裁剪，插件直接 URL encode 使用） */
    val keyword: String,
    /** 页码，从 1 开始 */
    val page: Int = 1,
)

/** 搜索/发现列表中的书籍项 */
data class SearchBook(
    /** 书名 */
    val name: String,
    /** 作者（未知为空串） */
    val author: String = "",
    /** 封面图地址（未知为空串） */
    val coverUrl: String = "",
    /** 简介（列表页没有可留空，详情页补全） */
    val intro: String = "",
    /** 最新章节名（仅网文站点有，可空） */
    val latestChapter: String = "",
    /** 书籍详情页地址（后续 getBookInfo/getChapterList 的入参） */
    val detailUrl: String,
)

/** 发现页分类槽位 */
data class ExploreSlot(
    /** 分类标题（玄幻/都市…） */
    val title: String,
    /**
     * 分类地址模板，含 {{page}} 页码占位符（pageStart 起始页）；
     * 宿主在请求前替换。固定单页可不含占位符。
     */
    val url: String,
    /** 起始页码（多数站点为 1，部分为 0） */
    val pageStart: Int = 1,
)

/** 书籍详情元数据 */
data class BookInfo(
    val name: String,
    val author: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val latestChapter: String = "",
    /** 详情页地址（回显入参，便于宿主关联） */
    val detailUrl: String,
    /** 目录地址；为空表示目录就在详情页 */
    val tocUrl: String = "",
)

/** 章节项。[url] 为空且 [isVolume] 为 true 表示分卷标题 */
data class BookChapter(
    /** 章节序号（从 0 开始，宿主按此排序与记录进度） */
    val index: Int,
    val title: String,
    /** 正文页地址（分卷节点为空串） */
    val url: String,
    /** 是否为分卷标题（不参与正文加载） */
    val isVolume: Boolean = false,
)

/**
 * 章节正文结果。
 * @param text 净化后的纯文本正文（段落用 \n 分隔）；插图以 <img src="..."> 标签内嵌，
 *             宿主负责图片加载与缓存
 * @param nextUrl 正文分页的下一页地址：部分站点一章拆多页，宿主继续拉取并追加，
 *                无下一页为空串
 */
data class ChapterContent(
    val text: String,
    val nextUrl: String = "",
)
