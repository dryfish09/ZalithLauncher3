package com.movtery.zalithlauncher.game.account.labynet

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class LabyProfileCape(
    val id: String = "",
    @kotlinx.serialization.SerialName("image_hash")
    val imageHash: String = "",
    val name: String = ""
)

@Serializable
data class LabyProfileTextures(
    val cape: LabyProfileCape? = null
)

@Serializable
data class LabyProfile(
    val textures: LabyProfileTextures? = null
)

object LabyCapeApi {

    suspend fun fetchProfileCapes(client: HttpClient, uuid: String): List<LabyProfileCape> {
        val response = client.get("https://laby.net/$uuid/get-profile")
        val profile = response.body<LabyProfile>()
        val cape = profile.textures?.cape
        return if (cape != null && cape.id.isNotBlank()) listOf(cape) else emptyList()
    }

    suspend fun downloadCapeImage(
        client: HttpClient,
        textureId: String,
        targetFile: File
    ) {
        val url = "https://labymod.net/$textureId.png"
        val response = client.get(url)
        val channel = response.bodyAsChannel()
        FileUtils.copyInputStreamToFile(channel.toInputStream(), targetFile)
    }

    fun getCapeImageUrl(textureId: String): String =
        "https://labymod.net/$textureId.png"
}
