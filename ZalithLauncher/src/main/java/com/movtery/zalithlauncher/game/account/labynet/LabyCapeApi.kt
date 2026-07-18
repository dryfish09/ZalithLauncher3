package com.movtery.zalithlauncher.game.account.labynet

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class LabyCape(
    val name: String = "",
    val description: Map<String, String> = emptyMap(),
    @kotlinx.serialization.SerialName("image_hash")
    val imageHash: String = "",
    @kotlinx.serialization.SerialName("use_count")
    val useCount: Int = 0
)

object LabyCapeApi {
    private const val BASE_URL = "https://api.laby.net/api/v3"

    suspend fun fetchAllCapes(client: HttpClient): List<LabyCape> {
        val response = client.get("$BASE_URL/capes")
        return response.body<List<LabyCape>>()
    }

    suspend fun downloadCapeImage(
        client: HttpClient,
        imageHash: String,
        targetFile: File
    ) {
        val url = "https://laby.net/texture/cape/$imageHash.png"
        val response = client.get(url)
        val channel = response.bodyAsChannel()
        FileUtils.copyInputStreamToFile(channel.toInputStream(), targetFile)
    }

    fun getCapeImageUrl(imageHash: String): String =
        "https://laby.net/texture/cape/$imageHash.png"
}
