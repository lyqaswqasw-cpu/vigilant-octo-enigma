package com.example.data.model

import java.util.UUID

data class ChannelCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val imageUrl: String = "",
    val m3uUrl: String = "",
    val xtremeHost: String = "",
    val xtremeUsername: String = "",
    val xtremePassword: String = "",
    val xtremeExtension: String = "m3u8",
    val xtremeStreamFormat: String = "m3u8",
    val isVisible: Boolean = true,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val hasM3uSource: Boolean
        get() = m3uUrl.isNotBlank()

    val hasXtremeSource: Boolean
        get() = xtremeHost.isNotBlank() && xtremeUsername.isNotBlank() && xtremePassword.isNotBlank()

    fun hasXtremeAccount(): Boolean = hasXtremeSource

    fun toXtremeAccount(): XtremeAccount? {
        if (!hasXtremeSource) return null
        return XtremeAccount(
            id = "cat_xt_$id",
            name = name.ifBlank { "قسم Xtreme" },
            host = xtremeHost.trim(),
            username = xtremeUsername.trim(),
            password = xtremePassword.trim(),
            streamExtension = xtremeExtension.ifBlank { "m3u8" },
            targetSection = "iptv"
        )
    }

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "name" to name,
            "order" to order,
            "createdAt" to createdAt,
            "m3uUrl" to m3uUrl.trim(),
            "xtremeHost" to xtremeHost.trim(),
            "xtremeUsername" to xtremeUsername.trim(),
            "xtremePassword" to xtremePassword.trim(),
            "xtremeExtension" to xtremeExtension.trim().ifBlank { "m3u8" },
            "xtremeStreamFormat" to xtremeStreamFormat.trim().ifBlank { "m3u8" },
            "isVisible" to isVisible
        )
        if (imageUrl.isNotBlank()) {
            map["imageUrl"] = imageUrl
        }
        return map
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ChannelCategory {
            return ChannelCategory(
                id = id,
                name = map["name"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                m3uUrl = map["m3uUrl"] as? String ?: "",
                xtremeHost = map["xtremeHost"] as? String ?: "",
                xtremeUsername = map["xtremeUsername"] as? String ?: "",
                xtremePassword = map["xtremePassword"] as? String ?: "",
                xtremeExtension = (map["xtremeExtension"] as? String)?.ifBlank { "m3u8" } ?: "m3u8",
                xtremeStreamFormat = (map["xtremeStreamFormat"] as? String)?.ifBlank { "m3u8" } ?: "m3u8",
                isVisible = (map["isVisible"] as? Boolean) ?: true,
                order = (map["order"] as? Number)?.toInt() ?: 999,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
