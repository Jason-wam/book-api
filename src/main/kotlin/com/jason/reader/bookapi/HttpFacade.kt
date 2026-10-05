package com.jason.reader.bookapi

/**
 * 宿主统一 HTTP 门面：插件通过此接口发请求，自动共享宿主的连接池、Cookie 持久化、
 * UA、DNS 与证书策略。接口刻意不暴露 okhttp 类型，插件无需引入任何网络库。
 */
interface HttpFacade {

    /** 拉取文本响应（HTML/JSON/XML）。失败以 [Result.failure] 返回 */
    suspend fun text(
        url: String,
        options: RequestOptions = RequestOptions(),
    ): Result<String>

    /**
     * 下载文件到宿主分配的绝对路径 [destPath]（父目录自动创建）。
     * @param onProgress 已下载字节数回调（主线程外触发，插件不要直接操作 UI）
     * @return 成功返回写入的文件绝对路径
     */
    suspend fun download(
        url: String,
        destPath: String,
        options: RequestOptions = RequestOptions(),
        onProgress: ((bytes: Long) -> Unit)? = null,
    ): Result<String>
}

/** 请求选项（全部带默认值，按需覆盖） */
data class RequestOptions(
    val method: String = "GET",
    /** 自定义请求头（Cookie 由宿主自动管理，一般无需手动传） */
    val headers: Map<String, String> = emptyMap(),
    /** 请求体文本（POST 表单/JSON）；GET/HEAD 必须为 null */
    val body: String? = null,
    /** 请求体 Content-Type（如 application/x-www-form-urlencoded、application/json） */
    val contentType: String? = null,
    /**
     * 强制响应编码（如 "GBK"）；为空由宿主按响应头/字节序列自动探测
     * （宿主内置 chardet4j 检测能力）
     */
    val charset: String? = null,
    /** 连接/读取超时（毫秒）；null 用宿主默认值 */
    val timeoutMs: Long? = null,
)

/** 书源语义化异常：宿主据此向用户展示可理解的提示而非原始堆栈 */
sealed class SourceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** 触发限流（429 或站点频控），宿主可退避重试 */
    class RateLimited(message: String = "请求过于频繁，请稍后重试") :
        SourceException(message)

    /** 需要登录/登录态失效 */
    class LoginRequired(message: String = "需要登录后才能访问") :
        SourceException(message)

    /** 站点结构变更或规则解析失败（选择器无结果等） */
    class ParseFailed(message: String, cause: Throwable? = null) :
        SourceException(message, cause)

    /** 网络不可达/超时等 */
    class Network(message: String, cause: Throwable? = null) :
        SourceException(message, cause)

    /** 源不支持该能力（如未实现发现页） */
    class Unsupported(message: String) : SourceException(message)
}
