package com.example.data.repository

import android.util.Log
import com.example.data.model.FoulComment
import com.example.data.model.FoulTvGlobalConfig
import com.example.data.model.FoulVideo
import com.example.data.security.AdminAuthManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class FoulTvRepository {

    companion object {
        const val FIREBASE_RTDB_URL = "https://foul-ddbb8-default-rtdb.firebaseio.com"
        private const val TAG = "FoulTvRepository"
    }

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(FIREBASE_RTDB_URL)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Firebase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
    }

    private val videosRef by lazy {
        database.getReference("v4_foul_tv/videos").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val commentsRef by lazy {
        database.getReference("v4_foul_tv/comments").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val configRef by lazy {
        database.getReference("v4_foul_tv/config").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private fun requireAdminWriteAuth(): Result<Unit> {
        return if (AdminAuthManager.isAuthorizedForWrite()) {
            Result.success(Unit)
        } else {
            Result.failure(SecurityException("يجب تسجيل الدخول بحساب المطور أولاً لتنفيذ عمليات النشر أو التعديل أو الحذف"))
        }
    }

    /**
     * Realtime flow of all FOUL TV videos sorted by creation timestamp
     */
    fun getVideosFlow(): Flow<List<FoulVideo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FoulVideo>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val video = FoulVideo.fromMap(key, value as Map<String, Any?>)
                        if (com.example.data.security.AppSecurityGuard.isMaliciousText(video.title) ||
                            com.example.data.security.AppSecurityGuard.isMaliciousText(video.description)) {
                            continue
                        }
                        list.add(video)
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Foul TV videos listener error: ${error.message}")
                trySend(emptyList())
            }
        }
        videosRef.addValueEventListener(listener)
        awaitClose { videosRef.removeEventListener(listener) }
    }

    /**
     * Realtime flow of comments for a specific video
     */
    fun getCommentsFlow(videoId: String): Flow<List<FoulComment>> = callbackFlow {
        if (videoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val videoCommentsRef = commentsRef.child(videoId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FoulComment>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(FoulComment.fromMap(key, value as Map<String, Any?>))
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Foul TV comments listener error: ${error.message}")
                trySend(emptyList())
            }
        }
        videoCommentsRef.addValueEventListener(listener)
        awaitClose { videoCommentsRef.removeEventListener(listener) }
    }

    /**
     * Realtime flow of FOUL TV global settings
     */
    fun getGlobalConfigFlow(): Flow<FoulTvGlobalConfig> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hideViews = snapshot.child("hide_views_all").getValue(Boolean::class.java) ?: false
                val hideLikes = snapshot.child("hide_likes_all").getValue(Boolean::class.java) ?: false
                val disableComments = snapshot.child("disable_comments_all").getValue(Boolean::class.java) ?: false
                trySend(FoulTvGlobalConfig(hideViewsAll = hideViews, hideLikesAll = hideLikes, disableCommentsAll = disableComments))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(FoulTvGlobalConfig())
            }
        }
        configRef.addValueEventListener(listener)
        awaitClose { configRef.removeEventListener(listener) }
    }

    // =========================================================================
    // DEVELOPER / ADMIN WRITE OPERATIONS (Gated by Firebase Auth / Admin Session)
    // =========================================================================

    suspend fun publishVideo(video: FoulVideo): Result<String> {
        requireAdminWriteAuth().onFailure { return Result.failure(it) }
        return suspendCancellableCoroutine { cont ->
            val key = video.id.ifBlank { videosRef.push().key ?: "vid_${UUID.randomUUID().toString().take(8)}" }
            val updated = video.copy(id = key)
            videosRef.child(key).setValue(updated.toMap())
                .addOnSuccessListener { cont.resume(Result.success(key)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun updateVideo(video: FoulVideo): Result<Unit> {
        requireAdminWriteAuth().onFailure { return Result.failure(it) }
        if (video.id.isBlank()) return Result.failure(IllegalArgumentException("معرف الفيديو غير صالح"))
        return suspendCancellableCoroutine { cont ->
            videosRef.child(video.id).setValue(video.toMap())
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun deleteVideo(videoId: String): Result<Unit> {
        requireAdminWriteAuth().onFailure { return Result.failure(it) }
        if (videoId.isBlank()) return Result.failure(IllegalArgumentException("معرف الفيديو غير صالح"))
        return suspendCancellableCoroutine { cont ->
            videosRef.child(videoId).removeValue()
                .addOnSuccessListener {
                    commentsRef.child(videoId).removeValue()
                    cont.resume(Result.success(Unit))
                }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun updateGlobalConfig(config: FoulTvGlobalConfig): Result<Unit> {
        requireAdminWriteAuth().onFailure { return Result.failure(it) }
        return suspendCancellableCoroutine { cont ->
            val map = mapOf(
                "hide_views_all" to config.hideViewsAll,
                "hide_likes_all" to config.hideLikesAll,
                "disable_comments_all" to config.disableCommentsAll
            )
            configRef.setValue(map)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    fun incrementVideoViews(videoId: String) {
        if (videoId.isBlank() || !AdminAuthManager.isAuthorizedForWrite()) return
        videosRef.child(videoId).child("viewsCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = (currentData.value as? Number)?.toLong() ?: 0L
                currentData.value = current + 1L
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
    }

    fun toggleVideoLike(videoId: String, username: String) {
        if (videoId.isBlank() || username.isBlank() || !AdminAuthManager.isAuthorizedForWrite()) return
        val cleanUser = username.replace(Regex("[.#$\\[\\]/]"), "_")
        val videoRef = videosRef.child(videoId)
        videoRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val likedByNode = currentData.child("likedBy").child(cleanUser)
                val likesCountNode = currentData.child("likesCount")
                val currentLikes = (likesCountNode.value as? Number)?.toLong() ?: 0L
                val isLiked = likedByNode.value as? Boolean ?: false
                if (isLiked) {
                    likedByNode.value = null
                    likesCountNode.value = maxOf(0L, currentLikes - 1L)
                } else {
                    likedByNode.value = true
                    likesCountNode.value = currentLikes + 1L
                }
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
    }

    suspend fun addComment(comment: FoulComment): Result<Unit> {
        requireAdminWriteAuth().onFailure { return Result.failure(it) }
        if (comment.videoId.isBlank() || comment.text.isBlank()) {
            return Result.failure(IllegalArgumentException("نص التعليق مطلوب"))
        }
        return suspendCancellableCoroutine { cont ->
            val ref = commentsRef.child(comment.videoId)
            val key = comment.id.ifBlank { ref.push().key ?: "cmt_${UUID.randomUUID().toString().take(8)}" }
            val updated = comment.copy(id = key)
            ref.child(key).setValue(updated.toMap())
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun deleteComment(videoId: String, commentId: String): Result<Unit> {
        requireAdminWriteAuth().onFailure { return Result.failure(it) }
        if (videoId.isBlank() || commentId.isBlank()) return Result.failure(IllegalArgumentException("بيانات التعليق غير صالحة"))
        return suspendCancellableCoroutine { cont ->
            commentsRef.child(videoId).child(commentId).removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }
}
