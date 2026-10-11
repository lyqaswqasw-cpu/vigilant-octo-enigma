package com.example.data.repository

import android.util.Log
import com.example.data.model.Channel
import com.example.data.model.ChannelBranch
import com.example.data.model.ChannelCategory
import com.example.data.model.ChannelServer
import com.example.data.model.EventBanner
import com.example.data.model.XtremeAccount
import com.example.data.security.AdminAuthManager
import com.example.data.security.AppSecurityGuard
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Real-Time Database Repository (Developer / Admin Edition).
 * Full Read & Write access gated behind authenticated Admin session via AdminAuthManager & FirebaseAuth.
 */
class ChannelRepository {

    companion object {
        const val FIREBASE_RTDB_URL = AppSecurityGuard.FIREBASE_RTDB_URL
        val DEFAULT_XTREME_ACCOUNTS: List<XtremeAccount> = emptyList()
    }

    private val database: FirebaseDatabase? = try {
        val db = try {
            FirebaseDatabase.getInstance(FIREBASE_RTDB_URL)
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
        try {
            db.setPersistenceEnabled(true)
        } catch (_: Exception) {
        }
        db
    } catch (e: Exception) {
        Log.e("ChannelRepository", "Firebase Realtime Database not initialized: ${e.message}")
        null
    }

    private val channelsRef = database?.getReference("channels")
    private val categoriesRef = database?.getReference("categories")
    private val branchesRef = database?.getReference("channel_branches")
    private val liveMatchesRef = database?.getReference("live_matches")
    private val eventsRef = database?.getReference("events")
    private val xtremeAccountsRef = database?.getReference("xtreme_accounts")
    private val eventBannersRef = database?.getReference("event_banners")
    private val appConfigRef = database?.getReference("app_config")

    fun isAdminAuthenticated(): Boolean {
        return AdminAuthManager.isAuthorizedForWrite()
    }

    private fun checkAdminAuth(operationName: String): Result<Unit> {
        return if (AdminAuthManager.isAuthorizedForWrite()) {
            Result.success(Unit)
        } else {
            Result.failure(SecurityException("غير مصرح: يجب تسجيل الدخول بحساب المطور أولاً لتنفيذ ($operationName)"))
        }
    }

    // =========================================================================
    // 1. CHANNELS (READ & WRITE)
    // =========================================================================

    fun getChannelsFlow(): Flow<List<Channel>> = observeChannels()

    fun observeChannels(): Flow<List<Channel>> = callbackFlow {
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Channel>()
                for (child in snapshot.children) {
                    parseChannelSnapshot(child)?.let {
                        if (AppSecurityGuard.isSafeChannel(it)) {
                            list.add(it)
                        }
                    }
                }
                trySend(list.distinctBy { it.id })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        channelsRef?.addValueEventListener(rtdbListener)

        awaitClose {
            channelsRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun saveChannel(channel: Channel): Result<Unit> {
        checkAdminAuth("حفظ قناة").onFailure { return Result.failure(it) }
        return try {
            val cleanId = channel.id.ifBlank { UUID.randomUUID().toString() }
            val finalChannel = channel.copy(id = cleanId)
            val map = mapOf(
                "id" to finalChannel.id,
                "name" to finalChannel.name,
                "imageUrl" to finalChannel.imageUrl,
                "streamUrl" to finalChannel.effectiveServers.firstOrNull()?.streamUrl.orEmpty(),
                "streamType" to finalChannel.effectiveServers.firstOrNull()?.streamType.orEmpty().ifBlank { "m3u8" },
                "category" to finalChannel.category,
                "branchId" to finalChannel.branchId,
                "isLive" to finalChannel.isLive,
                "servers" to finalChannel.effectiveServers.map { s ->
                    mapOf(
                        "id" to s.id.ifBlank { UUID.randomUUID().toString() },
                        "name" to s.name,
                        "streamUrl" to s.streamUrl,
                        "streamType" to s.streamType,
                        "drmLicenseUrl" to s.drmLicenseUrl,
                        "userAgent" to s.userAgent,
                        "referer" to s.referer
                    )
                }
            )
            channelsRef?.child(cleanId)?.setValue(map)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChannel(channelId: String): Result<Unit> {
        checkAdminAuth("حذف قناة").onFailure { return Result.failure(it) }
        if (channelId.isBlank()) return Result.success(Unit)
        return try {
            channelsRef?.child(channelId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllChannels(): Result<Unit> {
        checkAdminAuth("حذف جميع القنوات").onFailure { return Result.failure(it) }
        return try {
            channelsRef?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseChannelSnapshot(child: DataSnapshot): Channel? {
        return try {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: return null
            val name = child.child("name").getValue(String::class.java) ?: ""
            if (name.isBlank()) return null

            val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
            val streamUrl = child.child("streamUrl").getValue(String::class.java) ?: ""
            val streamType = child.child("streamType").getValue(String::class.java) ?: "m3u8"
            val category = child.child("category").getValue(String::class.java) ?: "عام"
            val branchId = child.child("branchId").getValue(String::class.java) ?: ""
            val isLive = child.child("isLive").getValue(Boolean::class.java) ?: true

            val serversList = mutableListOf<ChannelServer>()
            val serversNode = child.child("servers")
            if (serversNode.exists()) {
                for (srvSnap in serversNode.children) {
                    val sId = srvSnap.child("id").getValue(String::class.java) ?: srvSnap.key ?: ""
                    val sName = srvSnap.child("name").getValue(String::class.java) ?: "سيرفر"
                    val sUrl = srvSnap.child("streamUrl").getValue(String::class.java) ?: ""
                    val sType = srvSnap.child("streamType").getValue(String::class.java) ?: "m3u8"
                    val sDrm = srvSnap.child("drmLicenseUrl").getValue(String::class.java) ?: ""
                    val sUa = srvSnap.child("userAgent").getValue(String::class.java) ?: ""
                    val sRef = srvSnap.child("referer").getValue(String::class.java) ?: ""
                    if (sUrl.isNotBlank()) {
                        serversList.add(
                            ChannelServer(
                                id = sId,
                                name = sName,
                                streamUrl = sUrl,
                                streamType = sType,
                                drmLicenseUrl = sDrm,
                                userAgent = sUa,
                                referer = sRef
                            )
                        )
                    }
                }
            }
            if (serversList.isEmpty() && streamUrl.isNotBlank()) {
                serversList.add(
                    ChannelServer(
                        id = "default_1",
                        name = "سيرفر أساسي",
                        streamUrl = streamUrl,
                        streamType = streamType
                    )
                )
            }

            Channel(
                id = id,
                name = name,
                imageUrl = imageUrl,
                streamUrl = streamUrl,
                streamType = streamType,
                category = category,
                branchId = branchId,
                isLive = isLive,
                servers = serversList
            )
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // 2. CATEGORIES (READ & WRITE)
    // =========================================================================

    fun getCategoriesFlow(): Flow<List<ChannelCategory>> = observeCategories()

    fun observeCategories(): Flow<List<ChannelCategory>> = callbackFlow {
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChannelCategory>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                        val m3uUrl = child.child("m3uUrl").getValue(String::class.java) ?: ""
                        val xtremeHost = child.child("xtremeHost").getValue(String::class.java) ?: ""
                        val xtremeUsername = child.child("xtremeUsername").getValue(String::class.java) ?: ""
                        val xtremePassword = child.child("xtremePassword").getValue(String::class.java) ?: ""
                        val xtremeStreamFormat = child.child("xtremeStreamFormat").getValue(String::class.java) ?: "m3u8"
                        val isVisible = child.child("isVisible").getValue(Boolean::class.java) ?: true
                        val order = child.child("order").getValue(Int::class.java) ?: 0
                        if (name.isNotBlank()) {
                            val cat = ChannelCategory(
                                id = id,
                                name = name,
                                imageUrl = imageUrl,
                                m3uUrl = m3uUrl,
                                xtremeHost = xtremeHost,
                                xtremeUsername = xtremeUsername,
                                xtremePassword = xtremePassword,
                                xtremeStreamFormat = xtremeStreamFormat,
                                isVisible = isVisible,
                                order = order
                            )
                            if (AppSecurityGuard.isSafeCategory(cat)) {
                                list.add(cat)
                            }
                        }
                    } catch (_: Exception) {}
                }
                trySend(list.distinctBy { it.name.trim() }.sortedBy { it.order })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        categoriesRef?.addValueEventListener(rtdbListener)

        awaitClose {
            categoriesRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun saveCategory(category: ChannelCategory): Result<Unit> {
        checkAdminAuth("حفظ قسم").onFailure { return Result.failure(it) }
        return try {
            val cleanId = category.id.ifBlank { UUID.randomUUID().toString() }
            val map = mapOf(
                "id" to cleanId,
                "name" to category.name.trim(),
                "imageUrl" to category.imageUrl.trim(),
                "m3uUrl" to category.m3uUrl.trim(),
                "xtremeHost" to category.xtremeHost.trim(),
                "xtremeUsername" to category.xtremeUsername.trim(),
                "xtremePassword" to category.xtremePassword.trim(),
                "xtremeStreamFormat" to category.xtremeStreamFormat.trim().ifBlank { "m3u8" },
                "isVisible" to category.isVisible,
                "order" to category.order
            )
            categoriesRef?.child(cleanId)?.setValue(map)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        checkAdminAuth("حذف قسم").onFailure { return Result.failure(it) }
        if (categoryId.isBlank()) return Result.success(Unit)
        return try {
            categoriesRef?.child(categoryId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. BRANCHES (READ & WRITE)
    // =========================================================================

    fun getBranchesFlow(): Flow<List<ChannelBranch>> = observeBranches()

    fun observeBranches(): Flow<List<ChannelBranch>> = callbackFlow {
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChannelBranch>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val categoryId = child.child("categoryId").getValue(String::class.java) ?: ""
                        val categoryName = child.child("categoryName").getValue(String::class.java) ?: "عام"
                        val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                        val order = child.child("order").getValue(Int::class.java) ?: 0
                        if (name.isNotBlank()) {
                            val br = ChannelBranch(
                                id = id,
                                name = name,
                                categoryId = categoryId,
                                categoryName = categoryName,
                                imageUrl = imageUrl,
                                order = order
                            )
                            if (AppSecurityGuard.isSafeBranch(br)) {
                                list.add(br)
                            }
                        }
                    } catch (_: Exception) {}
                }
                trySend(list.distinctBy { it.id }.sortedBy { it.order })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        branchesRef?.addValueEventListener(rtdbListener)

        awaitClose {
            branchesRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun saveBranch(branch: ChannelBranch): Result<Unit> {
        checkAdminAuth("حفظ فرع").onFailure { return Result.failure(it) }
        return try {
            val cleanId = branch.id.ifBlank { UUID.randomUUID().toString() }
            val map = mapOf(
                "id" to cleanId,
                "name" to branch.name.trim(),
                "categoryId" to branch.categoryId.trim(),
                "categoryName" to branch.categoryName.trim(),
                "imageUrl" to branch.imageUrl.trim(),
                "order" to branch.order
            )
            branchesRef?.child(cleanId)?.setValue(map)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBranch(branchId: String): Result<Unit> {
        checkAdminAuth("حذف فرع").onFailure { return Result.failure(it) }
        if (branchId.isBlank()) return Result.success(Unit)
        return try {
            branchesRef?.child(branchId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. LIVE MATCHES & EVENTS (READ & WRITE)
    // =========================================================================

    suspend fun saveLiveMatchEvent(
        eventId: String,
        name: String,
        tournament: String,
        broadcaster: String,
        imageUrl: String,
        startTimeMs: Long,
        targetChannelId: String?,
        servers: List<ChannelServer>
    ): Result<Unit> {
        checkAdminAuth("حفظ مباراة").onFailure { return Result.failure(it) }
        return try {
            val cleanId = eventId.ifBlank { UUID.randomUUID().toString() }
            val teams = name.split("ضد", "vs", "VS", "-").map { it.trim() }.filter { it.isNotBlank() }
            val home = teams.getOrNull(0) ?: name
            val away = teams.getOrNull(1) ?: ""

            val map = mapOf(
                "id" to cleanId,
                "name" to name,
                "homeTeam" to home,
                "awayTeam" to away,
                "tournament" to tournament,
                "leagueName" to tournament,
                "broadcaster" to broadcaster,
                "imageUrl" to imageUrl,
                "homeLogo" to imageUrl,
                "startTimeMs" to startTimeMs,
                "startTimeMillis" to startTimeMs,
                "targetChannelId" to (targetChannelId ?: ""),
                "isLive" to true,
                "servers" to servers.map { s ->
                    mapOf(
                        "id" to s.id.ifBlank { UUID.randomUUID().toString() },
                        "name" to s.name,
                        "streamUrl" to s.streamUrl,
                        "streamType" to s.streamType
                    )
                }
            )
            eventsRef?.child(cleanId)?.setValue(map)?.await()
            liveMatchesRef?.child(cleanId)?.setValue(map)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLiveMatchEvent(eventId: String): Result<Unit> {
        checkAdminAuth("حذف مباراة").onFailure { return Result.failure(it) }
        val cleanId = eventId.removePrefix("firebase_").trim()
        if (cleanId.isBlank()) return Result.success(Unit)
        return try {
            eventsRef?.child(cleanId)?.removeValue()?.await()
            liveMatchesRef?.child(cleanId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. XTREME ACCOUNTS (READ & WRITE)
    // =========================================================================

    fun getXtremeAccountsFlow(): Flow<List<XtremeAccount>> = observeXtremeAccounts()

    fun observeXtremeAccounts(): Flow<List<XtremeAccount>> = callbackFlow {
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<XtremeAccount>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: "Xtreme Server"
                        val host = child.child("host").getValue(String::class.java) ?: ""
                        val username = child.child("username").getValue(String::class.java) ?: ""
                        val password = child.child("password").getValue(String::class.java) ?: ""
                        val streamFormat = child.child("streamFormat").getValue(String::class.java) ?: "m3u8"
                        val targetSection = child.child("targetSection").getValue(String::class.java) ?: "both"
                        val isActive = child.child("isActive").getValue(Boolean::class.java) ?: true
                        if (host.isNotBlank() && username.isNotBlank()) {
                            val acc = XtremeAccount(
                                id = id,
                                name = name,
                                host = host,
                                username = username,
                                password = password,
                                streamFormat = streamFormat,
                                targetSection = targetSection,
                                isActive = isActive
                            )
                            if (AppSecurityGuard.isSafeXtremeAccount(acc)) {
                                list.add(acc)
                            }
                        }
                    } catch (_: Exception) {}
                }
                trySend(list.distinctBy { it.id })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        xtremeAccountsRef?.addValueEventListener(rtdbListener)

        awaitClose {
            xtremeAccountsRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun saveXtremeAccount(account: XtremeAccount): Result<Unit> {
        checkAdminAuth("حفظ حساب Xtreme").onFailure { return Result.failure(it) }
        return try {
            val cleanId = account.id.ifBlank { UUID.randomUUID().toString() }
            val map = mapOf(
                "id" to cleanId,
                "name" to account.name.trim().ifBlank { "سيرفر IPTV" },
                "host" to account.host.trim(),
                "username" to account.username.trim(),
                "password" to account.password.trim(),
                "streamFormat" to account.streamFormat.trim().ifBlank { "m3u8" },
                "targetSection" to account.targetSection.trim().ifBlank { "both" },
                "isActive" to account.isActive,
                "addedAt" to account.addedAt
            )
            xtremeAccountsRef?.child(cleanId)?.setValue(map)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteXtremeAccount(accountId: String): Result<Unit> {
        checkAdminAuth("حذف حساب Xtreme").onFailure { return Result.failure(it) }
        if (accountId.isBlank()) return Result.success(Unit)
        return try {
            xtremeAccountsRef?.child(accountId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 6. EVENT BANNERS (READ & WRITE)
    // =========================================================================

    fun getEventBannersFlow(): Flow<List<EventBanner>> = observeEventBanners()

    fun observeEventBanners(): Flow<List<EventBanner>> = callbackFlow {
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<EventBanner>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val map = child.value as? Map<String, Any?>
                        if (map != null) {
                            val banner = EventBanner.fromMap(id, map)
                            if (AppSecurityGuard.isSafeBanner(banner)) {
                                list.add(banner)
                            }
                        }
                    } catch (_: Exception) {}
                }
                trySend(list.distinctBy { it.id })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        eventBannersRef?.addValueEventListener(rtdbListener)

        awaitClose {
            eventBannersRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun saveEventBanner(banner: EventBanner): Result<Unit> {
        checkAdminAuth("حفظ بانر إعلاني").onFailure { return Result.failure(it) }
        return try {
            val cleanId = banner.id.ifBlank { UUID.randomUUID().toString() }
            val map = banner.copy(id = cleanId).toMap()
            eventBannersRef?.child(cleanId)?.setValue(map)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEventBanner(bannerId: String): Result<Unit> {
        checkAdminAuth("حذف بانر إعلاني").onFailure { return Result.failure(it) }
        if (bannerId.isBlank()) return Result.success(Unit)
        return try {
            eventBannersRef?.child(bannerId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 7. APP CONFIG & BOTTOM NAV VISIBILITY (READ & WRITE)
    // =========================================================================

    fun getAppConfigFlow(): Flow<Map<String, Any?>> = observeAppConfig()

    fun observeAppConfig(): Flow<Map<String, Any?>> = callbackFlow {
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Any?>()
                for (child in snapshot.children) {
                    child.key?.let { key ->
                        map[key] = child.value
                    }
                }
                trySend(AppSecurityGuard.sanitizeAppConfig(map))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        }
        appConfigRef?.addValueEventListener(rtdbListener)

        awaitClose {
            appConfigRef?.removeEventListener(rtdbListener)
        }
    }

    suspend fun updateAppConfig(updates: Map<String, Any>): Result<Unit> {
        checkAdminAuth("تحديث إعدادات التطبيق").onFailure { return Result.failure(it) }
        return try {
            appConfigRef?.updateChildren(updates)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
