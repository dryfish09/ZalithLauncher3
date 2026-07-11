/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.plugin.renderer_v2

import android.content.Context
import android.content.pm.ApplicationInfo
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.ApkPlugin
import com.movtery.zalithlauncher.game.plugin.ApkPluginManager
import com.movtery.zalithlauncher.game.plugin.cacheAppIcon
import com.movtery.zalithlauncher.game.plugin.renderer_v2.data.RendererConfigList
import com.movtery.zalithlauncher.path.GLOBAL_JSON
import com.movtery.zalithlauncher.utils.logging.Logger

object RendererV2PluginManager : ApkPluginManager() {
    private const val TAG = "RendererV2Plugin"
    private const val META_V2_PLUGIN = "fclPlugin_V2"

    private val rendererPluginList: MutableList<RendererV2Data> = mutableListOf()
    private val packageNameList: MutableList<String> = mutableListOf()

    /** 扫描阶段暂存的插件信息 */
    private val pendingPlugins: MutableList<Pair<String, ApplicationInfo>> = mutableListOf()

    fun getRendererList(): List<RendererV2Data> = rendererPluginList

    fun getPackageNameList(): List<String> = packageNameList

    fun clearPlugin() {
        rendererPluginList.clear()
        packageNameList.clear()
        pendingPlugins.clear()
    }

    /**
     * 识别插件并暂存[ApplicationInfo]，不做加载
     */
    override fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return

        val metaData = info.metaData ?: return
        if (!metaData.getBoolean(META_V2_PLUGIN, false)) return

        pendingPlugins.add(info.packageName to info)
    }

    /**
     * 批量并行启动所有插件的 Activity 获取配置
     */
    fun loadAllConfigs(
        context: Context,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (pendingPlugins.isEmpty()) return

        Logger.debug(TAG, "Batch loading ${pendingPlugins.size} plugin(s)...")

        val configMap = ActivityConfigLoader.loadConfigs(context, pendingPlugins)
        val pm = context.packageManager

        pendingPlugins.forEach { (packageName, info) ->
            val configJson = configMap[packageName]
            if (configJson == null) {
                Logger.warning(TAG, "No config received from $packageName")
                return@forEach
            }

            // 反序列化渲染器配置信息
            val configList = runCatching {
                GLOBAL_JSON.decodeFromString<RendererConfigList>(configJson)
            }.onFailure {
                Logger.error(TAG, "Failed to parse config JSON from $packageName", it)
            }.getOrNull() ?: return@forEach

            // 获取插件应用信息
            val appLabel = info.loadLabel(pm).toString()
            val appVersion = runCatching {
                pm.getPackageInfo(packageName, 0).versionName ?: ""
            }.getOrDefault("")

            packageNameList.add(packageName)

            configList.data.forEach { data ->
                val renderer = RendererV2Data(
                    packageName = packageName,
                    summary = context.getString(R.string.settings_renderer_from_plugins, appLabel),
                    renderer = data
                )
                rendererPluginList.add(renderer)
            }

            // 已成功加载目标插件
            runCatching {
                cacheAppIcon(context, info)
                ApkPlugin(
                    packageName = packageName,
                    appName = appLabel,
                    appVersion = appVersion
                )
            }.getOrNull()?.let { loaded(it) }

            Logger.debug(TAG, "Loaded ${configList.data.size} renderer(s) from $packageName")
        }

        pendingPlugins.clear()
    }

    /**
     * 移除加载失败的渲染器
     */
    fun removeRenderer(failedToLoadList: List<RendererV2Data>) {
        rendererPluginList.removeAll { it in failedToLoadList }

        // 检查是否有包名的所有渲染器都被移除
        val failedByPackage = failedToLoadList.groupBy { it.packageName }
        failedByPackage.keys.forEach { packageName ->
            if (rendererPluginList.none { it.packageName == packageName }) {
                packageNameList.remove(packageName)
                Logger.info(TAG, "All renderers removed for $packageName, package unloaded.")
            }
        }
    }
}
