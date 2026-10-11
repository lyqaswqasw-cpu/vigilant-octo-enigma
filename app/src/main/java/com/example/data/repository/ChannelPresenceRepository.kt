package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

data class OnlineUserSession(
    val sessionId: String = "",
    val channel: String = "الرئيسية",
    val timestamp: Long = System.currentTimeMillis(),
    val device: String = "Android"
)

class ChannelPresenceRepository(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "ChannelPresenceRepo"
        private const val PREFS_NAME = "presence_prefs"
        private const val KEY_SESSION_ID = "device_session_id"
    }

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
    }

    val sessionId: String by lazy {
        val sp = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = sp?.getString(KEY_SESSION_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString().take(12)
            sp?.edit()?.putString(KEY_SESSION_ID, id)?.apply()
        }
        id
    }

    private var activeChannelId: String? = null

    fun startWatchingChannel(channelId: String) {
        if (channelId.isBlank()) return
        activeChannelId = channelId
    }

    fun stopWatchingCurrentChannel() {
        activeChannelId = null
    }

    /**
     * Observes real-time online viewers count for a specific channel (Read-Only).
     */
    fun getOnlineViewersFlow(channelId: String): Flow<Int> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val channelPresenceRef = database.getReference("channel_presence").child(channelId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val realCount = snapshot.childrenCount.toInt()
                val displayCount = if (activeChannelId == channelId) maxOf(1, realCount) else realCount
                trySend(displayCount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Presence listener cancelled: ${error.message}")
                trySend(1)
            }
        }

        channelPresenceRef.addValueEventListener(listener)

        awaitClose {
            channelPresenceRef.removeEventListener(listener)
        }
    }
}
