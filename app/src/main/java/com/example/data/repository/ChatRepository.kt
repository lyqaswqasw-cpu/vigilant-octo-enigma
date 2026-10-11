package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ChatUser
import com.example.data.security.AdminAuthManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class ChatRepository(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val PREFS_NAME = "chat_user_prefs"
        private const val KEY_USERNAME = "logged_username"
        private const val KEY_FULL_NAME = "logged_full_name"
    }

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
    }

    private val usersRef by lazy {
        database.getReference("v4_chat_users")
    }

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedUser(): ChatUser? {
        val sp = prefs ?: return null
        val username = sp.getString(KEY_USERNAME, null) ?: return null
        val fullName = sp.getString(KEY_FULL_NAME, null) ?: return null
        return ChatUser(username = username, fullName = fullName)
    }

    fun getOrCreateGuestUser(): ChatUser {
        val saved = getSavedUser()
        if (saved != null) return saved
        val randomNum = (1000..9999).random()
        val guest = ChatUser(
            username = "admin_$randomNum",
            fullName = "المطور #$randomNum"
        )
        saveUserLocally(guest)
        return guest
    }

    fun saveUserLocally(user: ChatUser) {
        prefs?.edit()?.apply {
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            apply()
        }
    }

    fun clearSavedUser() {
        prefs?.edit()?.clear()?.apply()
    }

    fun sanitizeChannelId(channelId: String?): String {
        if (channelId.isNullOrBlank()) return "general"
        val clean = channelId.replace(Regex("[.#$\\[\\]/\\x00-\\x1F\\x7F]"), "_").trim()
        return if (clean.isBlank()) "general" else clean
    }

    private fun getChannelMessagesRef(channelId: String) =
        database.getReference("channel_chat_messages").child(sanitizeChannelId(channelId))

    fun getMessagesFlow(channelId: String, limit: Int = 200): Flow<List<ChatMessage>> = callbackFlow {
        val cleanId = sanitizeChannelId(channelId)
        val query = getChannelMessagesRef(cleanId).orderByChild("timestamp").limitToLast(limit)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val msg = ChatMessage.fromMap(key, value as Map<String, Any?>)
                        if (com.example.data.security.AppSecurityGuard.isMaliciousText(msg.text) ||
                            com.example.data.security.AppSecurityGuard.isMaliciousText(msg.authorFullName) ||
                            com.example.data.security.AppSecurityGuard.isMaliciousText(msg.authorUsername)) {
                            continue
                        }
                        list.add(msg)
                    }
                }
                list.sortBy { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Messages listener cancelled for channel $cleanId: ${error.message}")
                trySend(emptyList())
            }
        }

        query.addValueEventListener(listener)

        awaitClose {
            query.removeEventListener(listener)
        }
    }

    fun getMessagesFlow(limit: Int = 80): Flow<List<ChatMessage>> = getMessagesFlow("general", limit)

    suspend fun sendMessage(channelId: String, message: ChatMessage): Result<Unit> {
        if (!AdminAuthManager.isAuthorizedForWrite()) {
            return Result.failure(SecurityException("يجب تسجيل الدخول بحساب المطور أولاً لإرسال أو إدارة الرسائل"))
        }
        return suspendCancellableCoroutine { cont ->
            val ref = getChannelMessagesRef(channelId)
            val key = message.id.ifBlank { ref.push().key ?: "msg_${UUID.randomUUID().toString().take(8)}" }
            val updated = message.copy(id = key, channelId = sanitizeChannelId(channelId))
            ref.child(key).setValue(updated.toMap())
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun sendMessage(message: ChatMessage): Result<Unit> =
        sendMessage(message.channelId.ifBlank { "general" }, message)

    suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit> {
        if (!AdminAuthManager.isAuthorizedForWrite()) {
            return Result.failure(SecurityException("يجب تسجيل الدخول بحساب المطور أولاً لحذف الرسائل"))
        }
        if (messageId.isBlank()) return Result.failure(IllegalArgumentException("معرف الرسالة غير صالح"))
        return suspendCancellableCoroutine { cont ->
            getChannelMessagesRef(channelId).child(messageId).removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = deleteMessage("general", messageId)

    suspend fun clearChannelChat(channelId: String): Result<Unit> {
        if (!AdminAuthManager.isAuthorizedForWrite()) {
            return Result.failure(SecurityException("يجب تسجيل الدخول بحساب المطور أولاً لمسح المحادثة"))
        }
        return suspendCancellableCoroutine { cont ->
            getChannelMessagesRef(channelId).removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun toggleReaction(channelId: String, messageId: String, emoji: String, username: String): Result<Unit> {
        if (!AdminAuthManager.isAuthorizedForWrite()) {
            return Result.failure(SecurityException("يجب تسجيل الدخول بحساب المطور أولاً"))
        }
        if (messageId.isBlank() || emoji.isBlank() || username.isBlank()) return Result.success(Unit)
        return suspendCancellableCoroutine { cont ->
            val msgRef = getChannelMessagesRef(channelId).child(messageId)
            msgRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    @Suppress("UNCHECKED_CAST")
                    val map = snapshot.value as? Map<String, Any?>
                    if (map == null) {
                        cont.resume(Result.success(Unit))
                        return
                    }
                    val msg = ChatMessage.fromMap(messageId, map)
                    val currentReactions = msg.reactions.toMutableMap()
                    val usersForEmoji = (currentReactions[emoji] ?: emptyList()).toMutableList()
                    if (usersForEmoji.contains(username)) {
                        usersForEmoji.remove(username)
                    } else {
                        usersForEmoji.add(username)
                    }
                    if (usersForEmoji.isEmpty()) {
                        currentReactions.remove(emoji)
                    } else {
                        currentReactions[emoji] = usersForEmoji
                    }
                    msgRef.child("reactions").setValue(currentReactions)
                        .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                        .addOnFailureListener { cont.resume(Result.failure(it)) }
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(error.toException()))
                }
            })
        }
    }

    suspend fun toggleReaction(messageId: String, emoji: String, username: String): Result<Unit> =
        toggleReaction("general", messageId, emoji, username)

    suspend fun registerUser(user: ChatUser): Result<ChatUser> {
        val cleanUsername = user.username.trim().lowercase().replace(" ", "_").replace("@", "")
        if (cleanUsername.isBlank() || user.fullName.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى ملء جميع الحقول المطلوبة"))
        }
        val finalUser = user.copy(username = cleanUsername)
        saveUserLocally(finalUser)
        if (AdminAuthManager.isAuthorizedForWrite()) {
            try {
                usersRef.child(cleanUsername).setValue(finalUser.toMap())
            } catch (_: Exception) {}
        }
        return Result.success(finalUser)
    }

    suspend fun loginUser(username: String, password: String): Result<ChatUser> = suspendCancellableCoroutine { continuation ->
        val cleanUsername = username.trim().lowercase().replace(" ", "_").replace("@", "")
        if (cleanUsername.isBlank() || password.isBlank()) {
            continuation.resume(Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم وكلمة المرور")))
            return@suspendCancellableCoroutine
        }

        val userRef = usersRef.child(cleanUsername)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val fallbackUser = ChatUser(username = cleanUsername, fullName = cleanUsername, password = password.trim())
                    saveUserLocally(fallbackUser)
                    continuation.resume(Result.success(fallbackUser))
                    return
                }

                @Suppress("UNCHECKED_CAST")
                val map = snapshot.value as? Map<String, Any?>
                if (map == null) {
                    continuation.resume(Result.failure(IllegalStateException("بيانات الحساب غير صالحة")))
                    return
                }

                val chatUser = ChatUser.fromMap(map)
                if (chatUser.password != password.trim()) {
                    continuation.resume(Result.failure(IllegalStateException("كلمة المرور غير صحيحة")))
                    return
                }

                saveUserLocally(chatUser)
                continuation.resume(Result.success(chatUser))
            }

            override fun onCancelled(error: DatabaseError) {
                val fallbackUser = ChatUser(username = cleanUsername, fullName = cleanUsername, password = password.trim())
                saveUserLocally(fallbackUser)
                continuation.resume(Result.success(fallbackUser))
            }
        })
    }
}
