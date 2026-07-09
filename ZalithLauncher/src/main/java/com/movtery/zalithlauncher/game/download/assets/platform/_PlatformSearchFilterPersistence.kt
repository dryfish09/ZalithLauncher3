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

package com.movtery.zalithlauncher.game.download.assets.platform

import com.google.gson.Gson
import com.movtery.zalithlauncher.setting.launcherMMKV

private val gson = Gson()

private data class PersistedPlatformSearchFilter(
    val gameVersion: String? = null,
    val sortField: String = PlatformSortField.RELEVANCE.name,
    val categories: List<String> = emptyList(),
    val modloader: String? = null
)

fun saveSearchFilter(key: String, filter: PlatformSearchFilter) {
    val persisted = PersistedPlatformSearchFilter(
        gameVersion = filter.gameVersion,
        sortField = filter.sortField.name,
        categories = filter.categories.mapNotNull { (it as? Enum<*>)?.name },
        modloader = (filter.modloader as? Enum<*>)?.name
    )
    launcherMMKV().putString(key, gson.toJson(persisted)).apply()
}

fun loadSearchFilter(key: String): PlatformSearchFilter? {
    val json = launcherMMKV().getString(key, null) ?: return null
    if (json.isEmpty()) return null
    val persisted = try {
        gson.fromJson(json, PersistedPlatformSearchFilter::class.java)
    } catch (_: Exception) {
        return null
    }

    val sortField = try {
        PlatformSortField.valueOf(persisted.sortField)
    } catch (_: Exception) {
        PlatformSortField.RELEVANCE
    }

    return PlatformSearchFilter(
        gameVersion = persisted.gameVersion,
        sortField = sortField
    )
}
