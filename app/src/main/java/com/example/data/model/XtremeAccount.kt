package com.example.data.model

import java.util.UUID

data class XtremeAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val streamExtension: String = "m3u8",
    val streamFormat: String = "m3u8",
    val targetSection: String = "iptv", // "iptv", "library", or "both"
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncSignature: String = ""
) {
    val credentialsSignature: String
        get() = "${host.trim()}|${username.trim()}|${password.trim()}|${streamFormat.trim()}|${targetSection.trim()}"

    fun isForIptv(): Boolean = targetSection.equals("iptv", ignoreCase = true) ||
            targetSection.equals("both", ignoreCase = true) ||
            targetSection.isBlank()

    fun isForLibrary(): Boolean = targetSection.equals("library", ignoreCase = true) ||
            targetSection.equals("both", ignoreCase = true) ||
            targetSection.isBlank()

    val targetSectionArabicLabel: String
        get() = when (targetSection.lowercase().trim()) {
            "iptv" -> "قسم IPTV (بث مباشر)"
            "library" -> "قسم المكتبة (أفلام ومسلسلات)"
            else -> "قسم IPTV + المكتبة معاً"
        }

    fun getCleanHost(): String {
        var h = host.trim()
        if (!h.startsWith("http://") && !h.startsWith("https://")) {
            h = "http://$h"
        }
        return h.trimEnd('/')
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "host" to host,
            "username" to username,
            "password" to password,
            "streamExtension" to streamExtension,
            "streamFormat" to streamFormat,
            "targetSection" to targetSection.ifBlank { "iptv" },
            "isActive" to isActive,
            "addedAt" to addedAt,
            "createdAt" to createdAt
        )
    }

    companion object {
        const val SECTION_IPTV = "iptv"
        const val SECTION_LIBRARY = "library"
        const val SECTION_BOTH = "both"

        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): XtremeAccount {
            return XtremeAccount(
                id = (map["id"] as? String)?.ifBlank { id } ?: id,
                name = map["name"] as? String ?: "حساب IPTV",
                host = map["host"] as? String ?: "",
                username = map["username"] as? String ?: "",
                password = map["password"] as? String ?: "",
                streamExtension = (map["streamExtension"] as? String)?.ifBlank { "m3u8" } ?: "m3u8",
                streamFormat = (map["streamFormat"] as? String)?.ifBlank { "m3u8" } ?: "m3u8",
                targetSection = (map["targetSection"] as? String)?.ifBlank { "both" } ?: "both",
                isActive = (map["isActive"] as? Boolean) ?: true,
                addedAt = (map["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class XtremeItem(
    val id: String,
    val title: String,
    val streamId: String,
    val streamIcon: String,
    val type: XtremeContentType, // LIVE, VOD_MOVIE, SERIES
    val categoryId: String = "",
    val categoryName: String = "",
    val streamUrl: String,
    val accountName: String = "",
    val accountId: String = "",
    val containerExtension: String = "mp4",
    val quality: String = ""
)

enum class XtremeContentType(val label: String) {
    LIVE("بث مباشر"),
    MOVIE("أفلام"),
    SERIES("مسلسلات")
}
