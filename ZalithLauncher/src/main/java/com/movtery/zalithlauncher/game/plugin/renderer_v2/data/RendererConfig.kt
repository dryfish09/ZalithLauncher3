package com.movtery.zalithlauncher.game.plugin.renderer_v2.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param displayName               向用户展示的名称
 * @param rendererId                渲染器ID，启动器将会配置到环境变量`POJAV_RENDERER`，**不再需要塞进[env]里**
 * @param rendererGLPath            渲染器图形库具体路径
 * @param rendererEGLPath           渲染器EGL具体路径
 * @param dlopenLibPaths            需要 dlopen 的库的具体路径
 * @param env                       渲染器环境变量列表
 * @param minMCVer                  最低支持的 Minecraft 版本号，如`1.17`，为`null`则不限制
 * @param maxMCVer                  最高支持的 Minecraft 版本号，如`1.17`，为`null`则不限制
 */
@Serializable
data class RendererConfig(
    @SerialName("displayName")
    val displayName: String,
    @SerialName("rendererId")
    val rendererId: String,
    @SerialName("rendererGLPath")
    val rendererGLPath: String,
    @SerialName("rendererEGLPath")
    val rendererEGLPath: String,
    @SerialName("dlopenLibPaths")
    val dlopenLibPaths: List<String>,
    @SerialName("env")
    val env: List<Env>,
    @SerialName("minMCVer")
    val minMCVer: String?,
    @SerialName("maxMCVer")
    val maxMCVer: String?,
) {
    @Serializable
    sealed interface Env {
        /**
         * 普通的环境变量，这是不可配置的，固定存在的环境变量
         */
        @Serializable
        @SerialName("NormalEnv")
        data class NormalEnv(
            @SerialName("key")
            val key: String,
            @SerialName("value")
            val value: String,
        ): Env

        /**
         * 可由启动器进行配置的环境变量
         * @see EnvItems
         * @param values 该环境变量的配置项
         */
        @Serializable
        @SerialName("EditableEnv")
        data class EditableEnv(
            @SerialName("key")
            val key: String,
            @SerialName("values")
            val values: EnvItems
        ): Env
    }

    /**
     * 环境变量配置项，启动器将根据这些项
     * @param defaultValue 默认环境变量
     * @param values 可选环境变量
     * @param title 该配置项的标题（meta-data 索引）
     */
    @Serializable
    data class EnvItems(
        @SerialName("defaultValue")
        val defaultValue: String,
        @SerialName("values")
        val values: List<String>,
        @SerialName("title")
        val title: MetaString? = null,
    )

    /**
     * 在 meta-data 中添加字符串资源，启动器将通过索引访问到本地化文本
     */
    @Serializable
    data class MetaString(
        @SerialName("key")
        val key: String
    )
}
