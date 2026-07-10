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
    private const val META_CONFIG_CLASS = "config_class"
    private const val CONFIG_METHOD_NAME = "getConfig"

    private val rendererPluginList: MutableList<RendererV2Data> = mutableListOf()
    private val packageNameList: MutableList<String> = mutableListOf()

    /**
     * 获取所有已加载的插件提供的渲染器数据
     */
    fun getRendererList(): List<RendererV2Data> = rendererPluginList

    /**
     * 获取所有已加载的插件的包名
     */
    fun getPackageNameList(): List<String> = packageNameList

    fun clearPlugin() {
        rendererPluginList.clear()
        packageNameList.clear()
    }

    /**
     * 解析 V2 渲染器插件：通过 DexClassLoader 加载插件 dex，调用配置类获取 JSON
     */
    override fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return

        val metaData = info.metaData ?: return
        if (!metaData.getBoolean(META_V2_PLUGIN, false)) return

        val packageName = info.packageName
        val pm = context.packageManager

        // 读取配置类全限定名
        val configClassName = metaData.getString(META_CONFIG_CLASS)
        if (configClassName.isNullOrBlank()) {
            Logger.warning(TAG, "$packageName declares $META_V2_PLUGIN but missing $META_CONFIG_CLASS")
            return
        }

        // 通过 dex 加载配置
        Logger.debug(TAG, "Starting to load dex from $packageName.")
        val configJson = DexConfigLoader.loadConfig(
            context = context,
            info = info,
            className = configClassName,
            methodName = CONFIG_METHOD_NAME
        )
        if (configJson == null) {
            Logger.warning(TAG, "Failed to load config from $packageName via dex")
            return
        }

        // 反序列化渲染器配置信息
        val configList = runCatching {
            GLOBAL_JSON.decodeFromString<RendererConfigList>(configJson)
        }.onFailure {
            Logger.error(TAG, "Failed to parse config JSON from $packageName", it)
        }.getOrNull() ?: return

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
