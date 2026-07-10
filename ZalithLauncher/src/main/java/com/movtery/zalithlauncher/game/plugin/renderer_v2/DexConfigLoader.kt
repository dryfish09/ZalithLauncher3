package com.movtery.zalithlauncher.game.plugin.renderer_v2

import android.content.Context
import android.content.pm.ApplicationInfo
import dalvik.system.DexClassLoader
import com.movtery.zalithlauncher.utils.logging.Logger

private const val TAG = "DexConfigLoader"

/**
 * 通过 DexClassLoader 加载插件 APK 中的配置类，调用指定方法获取配置 JSON 字符串
 */
object DexConfigLoader {
    /**
     * @param info          插件的 ApplicationInfo
     * @param className     插件清单中 meta-data 指定的完整类名
     * @param methodName    要调用的方法名
     * @return 配置 JSON 字符串，失败返回 null
     */
    fun loadConfig(
        context: Context,
        info: ApplicationInfo,
        className: String,
        methodName: String = "getConfig"
    ): String? {
        val apkPath = info.sourceDir ?: run {
            Logger.warning(TAG, "sourceDir is null for ${info.packageName}")
            return null
        }

        val dexOutputDir = context.cacheDir
        val loader = runCatching {
            DexClassLoader(apkPath, dexOutputDir.absolutePath, null, context.classLoader)
        }.onFailure {
            Logger.error(TAG, "Failed to create DexClassLoader for ${info.packageName}", it)
        }.getOrNull() ?: return null

        val clazz = runCatching {
            loader.loadClass(className)
        }.onFailure {
            Logger.error(TAG, "Failed to load class $className from ${info.packageName}", it)
        }.getOrNull() ?: return null

        val instance = runCatching {
            clazz.getDeclaredConstructor().newInstance()
        }.onFailure {
            Logger.error(TAG, "Failed to instantiate $className from ${info.packageName}", it)
        }.getOrNull() ?: return null

        return runCatching {
            val method = clazz.getMethod(methodName, String::class.java)
            method.invoke(instance, info.nativeLibraryDir!!) as? String
        }.onFailure {
            Logger.error(TAG, "Failed to invoke $className.$methodName(nativeLibDir) from ${info.packageName}", it)
        }.getOrNull()
    }
}
