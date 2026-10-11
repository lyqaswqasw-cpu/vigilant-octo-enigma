package com.example.data.util

import com.example.data.model.Channel
import com.example.data.model.ChannelServer
import com.example.data.model.XtremeItem
import java.util.UUID

/**
 * Intelligent Channel Clustering & Quality Server Merging Engine.
 *
 * Automatically detects multiple stream qualities (4K, FHD, 1080p, HD, 720p, SD, 480p, Low, 360p, 240p, HEVC, RAW)
 * and server variations for any TV channel (beIN Sports, SSC, AD Sports, Alkass, MBC, OSN, Canal+, Sky, etc.)
 * from M3U playlists or Xtream Codes accounts (even 50,000+ channels) and merges them into a SINGLE clean Channel
 * with distinct quality servers organized from highest to lowest quality at the top.
 */
object ChannelClusteringEngine {

    data class RawStream(
        val rawName: String,
        val streamUrl: String,
        val logoUrl: String = "",
        val category: String = "عام",
        val streamType: String = "m3u8"
    )

    data class QualityInfo(
        val qualityTag: String,
        val shortBadge: String,
        val displayName: String,
        val priority: Int
    )

    // Repeated prefix tags like |AR|, [AR], (AR), AR:, AR |, AR -, ★, VIP:, etc.
    private val prefixTagsRegex = Regex("""^(?:(?:\|[^|]{1,12}\||\[[^\]]{1,12}\]|\([^\)]{1,12}\)|[A-Za-z0-9_]{2,8}\s*[:|\-]|★+|•+)\s*)+""")

    // Regex to detect quality tokens anywhere in name
    private val quality4kRegex = Regex("""(?i)(?:\b|_|-|\[|\()(4k|uhd|2160p?|3840p?)(?:\b|_|-|\]|\))""")
    private val qualityFhdRegex = Regex("""(?i)(?:\b|_|-|\[|\()(fhd\+?|1080p?|full\s*hd|1080i|raw|50fps|60fps)(?:\b|_|-|\]|\))""")
    private val qualityHdRegex = Regex("""(?i)(?:\b|_|-|\[|\()(hd\+?|720p?|high\s*quality|hq)(?:\b|_|-|\]|\))""")
    private val qualitySdRegex = Regex("""(?i)(?:\b|_|-|\[|\()(sd|576p?|480p?|576i|480i|standard|med|medium)(?:\b|_|-|\]|\))""")
    private val qualityLowRegex = Regex("""(?i)(?:\b|_|-|\[|\()(low|lq|240p?|360p?|144p?|mobile|weak|ضعيفة|منخفضة)(?:\b|_|-|\]|\))""")
    private val qualityHevcRegex = Regex("""(?i)(?:\b|_|-|\[|\()(hevc|h\.?265)(?:\b|_|-|\]|\))""")

    // Regex to strip qualities and cleanup junk from channel name
    private val stripQualityKeywordsRegex = Regex(
        """(?i)(?:\b|_|-)(4k|uhd|2160p?|fhd\+?|fhd|full\s*hd|1080p?|1080i|hd\+?|hd|720p?|hq|sd|576p?|480p?|360p?|240p?|144p?|low|lq|hevc|h\.?265|vip|raw|50fps|60fps|backup|bk|auto|multi|server\s*\d+|serveur\s*\d+|src\s*\d+|srv\s*\d+|سيرفر\s*\d+|مصدر\s*\d+)(?:\b|_|-)*"""
    )
    private val bracketJunkRegex = Regex("""[\[\(\{][^\]\)\}]*(?:p|4k|hd|fhd|sd|hevc|fps|mbps|low|hq|uhd|raw|vip|ar|en|fr|tr|backup|bk)[^\]\)\}]*[\]\)\}]""", RegexOption.IGNORE_CASE)
    private val trailingSymbolsRegex = Regex("""[\s|_\-:,•★/\\]+$""")
    private val leadingSymbolsRegex = Regex("""^[\s|_\-:,•★/\\]+""")
    private val extraSpacesRegex = Regex("""\s+""")

    /**
     * Extracts detected quality info from the raw channel title or stream URL.
     */
    fun detectQuality(rawTitle: String, streamUrl: String = ""): QualityInfo {
        val combined = "$rawTitle $streamUrl"
        val isHevc = qualityHevcRegex.containsMatchIn(combined)
        val hevcSuffix = if (isHevc) " HEVC" else ""

        return when {
            quality4kRegex.containsMatchIn(combined) -> QualityInfo(
                qualityTag = "4K",
                shortBadge = "4K$hevcSuffix",
                displayName = "4K UHD$hevcSuffix",
                priority = 100
            )
            qualityFhdRegex.containsMatchIn(combined) -> QualityInfo(
                qualityTag = "FHD",
                shortBadge = "FHD$hevcSuffix",
                displayName = "FHD 1080p$hevcSuffix",
                priority = 85
            )
            qualityHdRegex.containsMatchIn(combined) -> QualityInfo(
                qualityTag = "HD",
                shortBadge = "HD$hevcSuffix",
                displayName = "HD 720p$hevcSuffix",
                priority = 70
            )
            isHevc -> QualityInfo(
                qualityTag = "HEVC",
                shortBadge = "HEVC",
                displayName = "HEVC H.265",
                priority = 60
            )
            qualitySdRegex.containsMatchIn(combined) -> QualityInfo(
                qualityTag = "SD",
                shortBadge = "SD",
                displayName = "SD 480p",
                priority = 45
            )
            qualityLowRegex.containsMatchIn(combined) -> QualityInfo(
                qualityTag = "LOW",
                shortBadge = "LOW",
                displayName = "LOW 240p",
                priority = 25
            )
            else -> QualityInfo(
                qualityTag = "AUTO",
                shortBadge = "أساسي",
                displayName = "سيرفر أساسي",
                priority = 50
            )
        }
    }

    /**
     * Cleans and normalizes the channel title to its base name so all qualities merge together.
     * E.g. "|AR| BEIN SPORTS 1 HD [1080P]" -> "beIN SPORTS 1"
     * "AR: SSC 1 SD (576p)" -> "SSC 1"
     * "AD SPORTS PREMIUM 1 FHD" -> "AD Sports PREMIUM 1"
     */
    fun cleanChannelName(rawName: String): String {
        var clean = rawName.trim()

        // 1. Remove bracketed quality/country junk like [1080p], (HD), [HEVC], (240p)
        clean = bracketJunkRegex.replace(clean, " ")

        // 2. Remove leading prefix tags like |AR|, [AR], AR:, AR |, AR -
        clean = prefixTagsRegex.replace(clean, "")

        // 3. Remove standalone quality keywords
        clean = stripQualityKeywordsRegex.replace(clean, " ")

        // 4. Clean extra symbols
        clean = leadingSymbolsRegex.replace(clean, "")
        clean = trailingSymbolsRegex.replace(clean, "")
        clean = extraSpacesRegex.replace(clean, " ").trim()

        if (clean.isBlank()) {
            clean = rawName.trim()
        }

        // Standardize common Arabic/English sports & entertainment channel naming conventions
        clean = clean
            .replace(Regex("""(?i)\bbein\s*sport\b"""), "beIN SPORTS")
            .replace(Regex("""(?i)\bbein\s*sports\b"""), "beIN SPORTS")
            .replace(Regex("""(?i)\bssc\s*sport\b"""), "SSC")
            .replace(Regex("""(?i)\bssc\s*sports\b"""), "SSC")
            .replace(Regex("""(?i)\balkass\b"""), "Alkass")
            .replace(Regex("""(?i)\bad\s*sport\b"""), "AD Sports")
            .replace(Regex("""(?i)\bad\s*sports\b"""), "AD Sports")

        return clean
    }

    /**
     * Cleans and sanitizes raw logo URLs extracted from M3U or Xtream API.
     */
    fun cleanLogoUrl(rawLogo: String): String {
        var logo = rawLogo.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (logo.isBlank() || logo.equals("null", ignoreCase = true) || logo.equals("undefined", ignoreCase = true)) return ""
        if (logo.startsWith("//")) {
            logo = "https:$logo"
        }
        return logo
    }

    /**
     * Normalizes a clean name to a grouping key for clustering across qualities.
     */
    fun getNormalizedChannelKey(rawOrCleanName: String): String {
        val clean = cleanChannelName(rawOrCleanName)
        return clean.lowercase()
            .replace(Regex("""[^a-z0-9\u0600-\u06FF]"""), "")
    }

    private fun getGroupKey(cleanName: String, category: String): String {
        val normalized = cleanName.lowercase()
            .replace(Regex("""[^a-z0-9\u0600-\u06FF]"""), "")
        return "${category.trim().lowercase()}##$normalized"
    }

    /**
     * Converts any number of Xtream Codes live stream items into clustered Channels
     * with all qualities merged into clean servers at the top.
     */
    fun clusterXtremeLiveItems(
        items: List<XtremeItem>,
        forcedCategoryName: String = ""
    ): List<Channel> {
        if (items.isEmpty()) return emptyList()
        val rawStreams = ArrayList<RawStream>(items.size)
        for (item in items) {
            val cat = forcedCategoryName.ifBlank { item.categoryName.ifBlank { "عام" } }
            rawStreams.add(
                RawStream(
                    rawName = item.title,
                    streamUrl = item.streamUrl,
                    logoUrl = item.streamIcon,
                    category = cat,
                    streamType = item.containerExtension.ifBlank { "m3u8" }
                )
            )
        }
        return clusterRawStreams(rawStreams, forcedCategoryName.ifBlank { "عام" }, groupAcrossAllSubCategories = forcedCategoryName.isNotBlank())
    }

    /**
     * Finds all quality server variations of the given XtremeItem from the full list of IPTV channels
     * so the player and search view can display merged quality servers at the top.
     */
    fun findMergedQualityServersForXtremeItem(
        targetItem: XtremeItem,
        allItems: List<XtremeItem>
    ): List<Pair<String, XtremeItem>> {
        val targetKey = getNormalizedChannelKey(targetItem.title)
        if (targetKey.isBlank()) {
            val q = detectQuality(targetItem.title, targetItem.streamUrl)
            return listOf(q.displayName to targetItem)
        }

        val matching = allItems.filter { candidate ->
            getNormalizedChannelKey(candidate.title) == targetKey
        }

        if (matching.isEmpty()) {
            val q = detectQuality(targetItem.title, targetItem.streamUrl)
            return listOf(q.displayName to targetItem)
        }

        val withQuality = matching.map { item ->
            detectQuality(item.title, item.streamUrl) to item
        }.sortedByDescending { it.first.priority }

        val seenUrls = HashSet<String>()
        val qualityCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<Pair<String, XtremeItem>>()

        for ((qInfo, item) in withQuality) {
            val url = item.streamUrl.trim()
            if (seenUrls.add(url)) {
                val count = qualityCounts.getOrDefault(qInfo.qualityTag, 0) + 1
                qualityCounts[qInfo.qualityTag] = count
                val label = if (withQuality.size > 1) {
                    if (count > 1) "${qInfo.displayName} ($count)" else qInfo.displayName
                } else {
                    qInfo.displayName
                }
                result.add(label to item)
            }
        }
        return result
    }

    /**
     * Clusters a list of raw stream items into consolidated Channels with quality servers.
     */
    fun clusterRawStreams(
        rawStreams: List<RawStream>,
        defaultCategory: String,
        groupAcrossAllSubCategories: Boolean = false
    ): List<Channel> {
        if (rawStreams.isEmpty()) return emptyList()

        val groupMap = LinkedHashMap<String, MutableList<Pair<RawStream, QualityInfo>>>()
        val bestNameMap = LinkedHashMap<String, String>()
        val bestLogoMap = LinkedHashMap<String, String>()

        for (item in rawStreams) {
            if (item.streamUrl.isBlank() || item.rawName.isBlank()) continue

            val cat = if (groupAcrossAllSubCategories) {
                defaultCategory.ifBlank { "عام" }
            } else {
                item.category.ifBlank { defaultCategory }.ifBlank { "عام" }
            }
            val cleanName = cleanChannelName(item.rawName)
            val groupKey = getGroupKey(cleanName, cat)
            val quality = detectQuality(item.rawName, item.streamUrl)

            if (!groupMap.containsKey(groupKey)) {
                groupMap[groupKey] = mutableListOf()
                bestNameMap[groupKey] = cleanName
            }
            groupMap[groupKey]?.add(Pair(item, quality))

            val cleanLogo = cleanLogoUrl(item.logoUrl)
            if (cleanLogo.isNotBlank() && bestLogoMap[groupKey].isNullOrBlank()) {
                bestLogoMap[groupKey] = cleanLogo
            }
        }

        val clusteredChannels = ArrayList<Channel>(groupMap.size)

        for ((groupKey, streamPairs) in groupMap) {
            val displayName = bestNameMap[groupKey] ?: "قناة"
            val logo = bestLogoMap[groupKey]
                ?: cleanLogoUrl(streamPairs.firstOrNull { it.first.logoUrl.isNotBlank() }?.first?.logoUrl ?: "")
            val cat = if (groupAcrossAllSubCategories) {
                defaultCategory.ifBlank { "عام" }
            } else {
                streamPairs.firstOrNull()?.first?.category?.ifBlank { defaultCategory } ?: defaultCategory
            }

            // Sort servers by quality priority descending (4K -> FHD -> HD -> HEVC -> SD -> Low)
            val sortedPairs = streamPairs.sortedByDescending { it.second.priority }

            // Deduplicate same stream URLs within the channel
            val seenUrls = HashSet<String>()
            val servers = mutableListOf<ChannelServer>()
            val qualityCounts = mutableMapOf<String, Int>()

            for ((streamItem, qInfo) in sortedPairs) {
                val cleanUrl = streamItem.streamUrl.trim()
                if (seenUrls.add(cleanUrl)) {
                    val count = qualityCounts.getOrDefault(qInfo.qualityTag, 0) + 1
                    qualityCounts[qInfo.qualityTag] = count

                    val serverLabel = if (streamPairs.size > 1) {
                        if (count > 1) {
                            "${qInfo.displayName} ($count)"
                        } else {
                            qInfo.displayName
                        }
                    } else {
                        qInfo.displayName
                    }

                    val streamType = when {
                        cleanUrl.endsWith(".m3u8", ignoreCase = true) || streamItem.streamType == "m3u8" -> "m3u8"
                        cleanUrl.endsWith(".ts", ignoreCase = true) || streamItem.streamType == "ts" -> "ts"
                        else -> "normal"
                    }

                    val serverId = "srv_" + UUID.nameUUIDFromBytes(cleanUrl.toByteArray()).toString().take(10)
                    servers.add(
                        ChannelServer(
                            id = serverId,
                            name = serverLabel,
                            streamUrl = cleanUrl,
                            streamType = streamType
                        )
                    )
                }
            }

            if (servers.isNotEmpty()) {
                val channelId = "ch_" + UUID.nameUUIDFromBytes("${cat}_${displayName}".toByteArray()).toString().take(12)
                clusteredChannels.add(
                    Channel(
                        id = channelId,
                        name = displayName,
                        imageUrl = logo,
                        category = cat,
                        servers = servers
                    )
                )
            }
        }

        return clusteredChannels
    }

    fun normalizeComparisonKey(raw: String, streamUrl: String = ""): String = getNormalizedChannelKey(raw)
    fun extractCleanBaseName(raw: String, streamUrl: String = ""): String = cleanChannelName(raw)
    fun extractQualityBadge(raw: String, streamUrl: String = ""): String = detectQuality(raw, streamUrl).shortBadge
}
