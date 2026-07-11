package com.movtery.zalithlauncher.game.plugin.renderer_v2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.HandlerThread
import com.movtery.zalithlauncher.utils.logging.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TAG = "ActivityConfigLoader"

object ActivityConfigLoader {
    private const val ACTION_REQUEST_CONFIG = "com.launchers_plugin.renderer.REQUEST_CONFIG"
    private const val ACTION_RESPONSE_CONFIG = "com.launchers_plugin.renderer.RESPONSE_CONFIG"
    private const val EXTRA_CONFIG_JSON = "config_json"
    private const val EXTRA_PACKAGE_NAME = "package_name"
    private const val CONFIG_TIMEOUT_SECONDS = 5L

    fun loadConfigs(
        context: Context,
        pluginInfos: List<Pair<String, ApplicationInfo>>
    ): Map<String, String> {
        if (pluginInfos.isEmpty()) return emptyMap()

        val results = mutableMapOf<String, String>()
        val latch = CountDownLatch(pluginInfos.size)
        val hostPackage = context.packageName

        val handlerThread = HandlerThread("ConfigLoader").also { it.start() }
        val handler = Handler(handlerThread.looper)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != ACTION_RESPONSE_CONFIG) return
                val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
                val json = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: return
                if (pluginInfos.any { it.first == pkg }) {
                    results[pkg] = json
                    latch.countDown()
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(ACTION_RESPONSE_CONFIG),
            null,
            handler,
            Context.RECEIVER_EXPORTED
        )

        // 一次性启动所有插件的 Activity
        pluginInfos.forEach { (packageName, _) ->
            val intent = Intent(ACTION_REQUEST_CONFIG).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("host_package", hostPackage)
            }
            runCatching {
                context.startActivity(intent)
            }.onFailure {
                Logger.warning(TAG, "Failed to start activity for $packageName")
                latch.countDown() // 失败也 countDown，避免永久等待
            }
        }

        // 等待所有回复
        runCatching {
            latch.await(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        runCatching {
            context.unregisterReceiver(receiver)
        }
        handlerThread.quitSafely()

        Logger.debug(TAG, "Batch loaded ${results.size}/${pluginInfos.size} configs")
        return results
    }
}