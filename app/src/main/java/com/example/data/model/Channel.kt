package com.example.data.model

import java.util.UUID

enum class StreamType(val title: String, val extensionNote: String) {
    M3U8("m3u8", "HLS Live Stream"),
    TS("ts", "MPEG-TS Stream"),
    NORMAL("عادي", "Direct / MP4 Link");

    companion object {
        fun fromString(value: String?): StreamType {
            return when (value?.lowercase()?.trim()) {
                "m3u8" -> M3U8
                "ts" -> TS
                else -> NORMAL
            }
        }
    }
}

data class ChannelServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "SD",
    val streamUrl: String = "",
    val streamType: String = "m3u8", // "m3u8", "ts", "normal"
    val drmLicenseUrl: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val streamFormat: String = "m3u8",
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "streamUrl" to streamUrl,
            "streamType" to streamType,
            "drmLicenseUrl" to drmLicenseUrl,
            "userAgent" to userAgent,
            "referer" to referer,
            "streamFormat" to streamFormat,
            "isActive" to isActive,
            "addedAt" to addedAt
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ChannelServer {
            return ChannelServer(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                name = map["name"] as? String ?: "SD",
                streamUrl = map["streamUrl"] as? String ?: "",
                streamType = map["streamType"] as? String ?: "m3u8",
                drmLicenseUrl = map["drmLicenseUrl"] as? String ?: "",
                userAgent = map["userAgent"] as? String ?: "",
                referer = map["referer"] as? String ?: "",
                streamFormat = map["streamFormat"] as? String ?: "m3u8",
                isActive = (map["isActive"] as? Boolean) ?: true,
                addedAt = (map["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class Channel(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val imageUrl: String = "",
    val category: String = "كرة قدم",
    val branchId: String = "",
    val branchName: String = "",
    val servers: List<ChannelServer> = emptyList(),
    val streamUrl: String = "",
    val streamType: String = "m3u8",
    val createdAt: Long = System.currentTimeMillis(),
    val views: Long = 0L,
    val isLive: Boolean = true
) {
    val effectiveServers: List<ChannelServer> get() = servers

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "name" to name,
            "imageUrl" to imageUrl,
            "category" to category,
            "servers" to servers.map { it.toMap() },
            "createdAt" to createdAt,
            "views" to views,
            "isLive" to isLive,
            "appVersion" to "1.0.0"
        )
        if (branchId.isNotBlank()) {
            map["branchId"] = branchId
        }
        if (branchName.isNotBlank()) {
            map["branchName"] = branchName
        }
        return map
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): Channel {
            val serversRaw = map["servers"]
            val serverList = mutableListOf<ChannelServer>()

            when (serversRaw) {
                is List<*> -> {
                    for (item in serversRaw) {
                        if (item is Map<*, *>) {
                            serverList.add(ChannelServer.fromMap(item as Map<String, Any?>))
                        }
                    }
                }
                is Map<*, *> -> {
                    for ((_, item) in serversRaw) {
                        if (item is Map<*, *>) {
                            serverList.add(ChannelServer.fromMap(item as Map<String, Any?>))
                        }
                    }
                }
            }

            return Channel(
                id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: id,
                name = map["name"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                category = map["category"] as? String ?: "كرة قدم",
                branchId = map["branchId"] as? String ?: "",
                branchName = map["branchName"] as? String ?: "",
                servers = serverList,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                views = (map["views"] as? Number)?.toLong() ?: 0L,
                isLive = (map["isLive"] as? Boolean) ?: true
            )
        }
    }
}
