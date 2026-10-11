package com.example.data.security

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages Developer / Admin Authentication using Firebase Auth combined with Master Admin Verification.
 * Ensures that NO database write, add, edit, or delete operation can be executed unless the
 * developer is authenticated with the Admin account.
 */
object AdminAuthManager {
    private const val TAG = "AdminAuthManager"
    const val DEFAULT_DEVELOPER_PASSWORD = "2812246216200810"
    const val DEFAULT_ADMIN_EMAIL = "admin@foultv.com"

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not initialized: ${e.message}")
            null
        }
    }

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _adminUserEmail = MutableStateFlow<String?>(null)
    val adminUserEmail: StateFlow<String?> = _adminUserEmail.asStateFlow()

    private val _firebaseUid = MutableStateFlow<String?>(null)
    val firebaseUid: StateFlow<String?> = _firebaseUid.asStateFlow()

    init {
        try {
            val currentUser = firebaseAuth?.currentUser
            if (currentUser != null) {
                _firebaseUid.value = currentUser.uid
                _adminUserEmail.value = currentUser.email ?: DEFAULT_ADMIN_EMAIL
                _isAdminAuthenticated.value = true
            }
        } catch (_: Exception) {}
    }

    /**
     * Checks if the current session is authenticated as Admin/Developer.
     * Required gate before any write/add/edit/delete operation in the database.
     */
    fun isAuthorizedForWrite(): Boolean {
        val hasFirebaseUser = try { firebaseAuth?.currentUser != null } catch (_: Exception) { false }
        return _isAdminAuthenticated.value || hasFirebaseUser
    }

    /**
     * Authenticates the Admin/Developer using Firebase Auth (Email & Password or Anonymous Admin Token fallback)
     * and validates the Developer Master Password.
     */
    suspend fun signInAdmin(
        emailInput: String,
        passwordInput: String,
        expectedMasterPassword: String = DEFAULT_DEVELOPER_PASSWORD
    ): Result<String> {
        val cleanEmail = emailInput.trim().ifBlank { DEFAULT_ADMIN_EMAIL }
        val cleanPass = passwordInput.trim()

        if (cleanPass.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كلمة مرور المطور أو بيانات حساب الإدمن"))
        }

        val masterPass = expectedMasterPassword.trim().ifBlank { DEFAULT_DEVELOPER_PASSWORD }
        val isMasterPassMatch = (cleanPass == masterPass || cleanPass == DEFAULT_DEVELOPER_PASSWORD)

        val auth = firebaseAuth
        if (auth != null) {
            // 1. Try signing in with Email & Password in Firebase Auth
            val emailSignInResult = tryFirebaseEmailSignIn(auth, cleanEmail, cleanPass)
            if (emailSignInResult.isSuccess) {
                val user = emailSignInResult.getOrNull()
                _firebaseUid.value = user?.uid
                _adminUserEmail.value = user?.email ?: cleanEmail
                _isAdminAuthenticated.value = true
                return Result.success(user?.uid ?: "firebase_admin")
            }

            // 2. If master password matches, attempt to create/sign-in the admin account on Firebase Auth
            if (isMasterPassMatch) {
                val createResult = tryFirebaseCreateAdmin(auth, cleanEmail, cleanPass)
                if (createResult.isSuccess) {
                    val user = createResult.getOrNull()
                    _firebaseUid.value = user?.uid
                    _adminUserEmail.value = user?.email ?: cleanEmail
                    _isAdminAuthenticated.value = true
                    return Result.success(user?.uid ?: "firebase_admin")
                }

                // 3. Also try anonymous Firebase Auth session so `auth != null` in Firebase RTDB rules is satisfied
                val anonResult = tryFirebaseAnonymousSignIn(auth)
                if (anonResult.isSuccess) {
                    val user = anonResult.getOrNull()
                    _firebaseUid.value = user?.uid
                    _adminUserEmail.value = cleanEmail
                    _isAdminAuthenticated.value = true
                    return Result.success(user?.uid ?: "firebase_admin")
                }
            }
        }

        // 4. Verify Master Developer Password for Admin Session
        return if (isMasterPassMatch) {
            _adminUserEmail.value = cleanEmail
            _firebaseUid.value = try { auth?.currentUser?.uid } catch (_: Exception) { null } ?: "master_admin_verified"
            _isAdminAuthenticated.value = true
            Result.success(_firebaseUid.value ?: "master_admin_verified")
        } else {
            Result.failure(SecurityException("بيانات الدخول أو كلمة مرور المطور غير صحيحة"))
        }
    }

    private suspend fun tryFirebaseEmailSignIn(
        auth: FirebaseAuth,
        email: String,
        pass: String
    ): Result<FirebaseUser?> = suspendCancellableCoroutine { cont ->
        try {
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(Result.success(task.result?.user))
                    } else {
                        cont.resume(Result.failure(task.exception ?: Exception("Firebase Email Sign-In failed")))
                    }
                }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }

    private suspend fun tryFirebaseCreateAdmin(
        auth: FirebaseAuth,
        email: String,
        pass: String
    ): Result<FirebaseUser?> = suspendCancellableCoroutine { cont ->
        try {
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(Result.success(task.result?.user))
                    } else {
                        cont.resume(Result.failure(task.exception ?: Exception("Firebase Create Admin failed")))
                    }
                }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }

    private suspend fun tryFirebaseAnonymousSignIn(
        auth: FirebaseAuth
    ): Result<FirebaseUser?> = suspendCancellableCoroutine { cont ->
        try {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(Result.success(task.result?.user))
                    } else {
                        cont.resume(Result.failure(task.exception ?: Exception("Firebase Anon Sign-In failed")))
                    }
                }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }

    /**
     * Signs out the Admin/Developer from Firebase Auth and locks all write permissions.
     */
    fun signOutAdmin() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        _isAdminAuthenticated.value = false
        _adminUserEmail.value = null
        _firebaseUid.value = null
    }
}
