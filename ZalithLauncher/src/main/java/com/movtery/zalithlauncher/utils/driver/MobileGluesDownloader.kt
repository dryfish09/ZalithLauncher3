/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.utils.driver

import android.content.Context
import android.widget.Toast
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.upgrade.GithubReleaseApi
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.util.zip.ZipFile
import kotlinx.serialization.json.Json

object MobileGluesDownloader {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private const val REPO_API = "https://api.github.com/repos/MobileGL-Dev/MobileGlues-release/releases/latest"

    private fun getLibAbi(): String {
        return when (Architecture.primaryArmArch) {
            Architecture.ARCH_ARM64 -> "arm64-v8a"
            Architecture.ARCH_ARM -> "armeabi-v7a"
            else -> "arm64-v8a"
        }
    }

    fun downloadLatest(context: Context) {
        val abi = getLibAbi()
        val task = Task.runTask(
            id = "download_mobileglues",
            task = { it ->
                it.updateMessage("Fetching MobileGlues release...")
                it.updateProgress(-1f)

                val request = Request.Builder().url(REPO_API).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (!response.isSuccessful) throw Exception("Failed to fetch latest release")

                val body = response.body?.string() ?: throw Exception("Empty response body")
                val release = json.decodeFromString<GithubReleaseApi>(body)
                val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: throw Exception("No APK asset found in latest release")

                it.updateMessage("Downloading MobileGlues...")
                it.updateProgress(-1f)

                val downloadFile = File(PathManager.DIR_CACHE, asset.name)
                val downloadRequest = Request.Builder().url(asset.browserDownloadUrl).build()

                withContext(Dispatchers.IO) {
                    client.newCall(downloadRequest).execute().use { downloadResponse ->
                        if (!downloadResponse.isSuccessful) throw Exception("Failed to download MobileGlues APK")
                        val source = downloadResponse.body?.source() ?: throw Exception("Empty download body")
                        val totalSize = downloadResponse.body?.contentLength() ?: -1L

                        downloadFile.sink().buffer().use { sink ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Long = 0
                            var read: Int
                            while (source.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                                bytesRead += read
                                if (totalSize > 0) {
                                    it.updateProgress(bytesRead.toFloat() / totalSize)
                                }
                            }
                        }
                    }
                }

                it.updateMessage("Extracting MobileGlues library...")
                it.updateProgress(-1f)

                val extractDir = File(PathManager.DIR_DRIVERS, "mobileglues")
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()

                withContext(Dispatchers.IO) {
                    ZipFile(downloadFile).use { zip ->
                        val found = zip.entries().asSequence()
                            .any { entry ->
                                entry.name.startsWith("lib/$abi/") && entry.name.endsWith("libMobileGlues.so")
                            }
                        if (!found) throw Exception("libMobileGlues.so not found for $abi in APK")
                        zip.extractFromZip("lib/$abi/", extractDir)
                    }
                }

                downloadFile.delete()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "MobileGlues installed successfully", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { th ->
                lError("Failed to download MobileGlues", th)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${th.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
        TaskSystem.submitTask(task)
    }
}
