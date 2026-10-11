package com.example.data.security

import android.util.Log

/**
 * Enterprise-grade Anti-Defacement and Anti-Tamper Security Guard.
 * Protects the application, end-users, and Firebase synchronization against:
 * 1. Unauthorized server tampering, fake forced updates (e.g. 666, 777).
 * 2. Malicious abusive, defamatory or profane text injection.
 */
object AppSecurityGuard {

    private const val TAG = "AppSecurityGuard"
    const val FIREBASE_RTDB_URL = "https://foul-ddbb8-default-rtdb.firebaseio.com"

    // Blacklist of defacement, profanity, and attack signatures
    private val FORBIDDEN_WORDS = listOf(
        // Defamation, severe profanity & insults (Arabic roots & common variations)
        "كحبة", "قحبة", "كحبه", "قحبه", "كحاب", "قحاب", "كحب", "قحب", "الكحبة", "القحبة", "الكحبه", "القحبه",
        "زب", "زبك", "زبنا", "كس", "كسك", "كسختك", "كسامك", "كس اختك", "كس امك", "كسمك", "كسخت",
        "منيوك", "منيوكه", "منيوكة", "منايك", "تنايك", "نيج", "انيج", "ينيج", "تنايج", "منيوكين",
        "طيز", "طيزك", "طيازة", "عرص", "عرصة", "عرصه",
        "ساقط", "ساقطة", "سافل", "سفلة", "حقير", "كلب", "ابن الكلب", "ابن الحرام", "ابن شرموطة", "ابن الشرموطة",
        "شرموط", "شرموطة", "شرموطه", "شرايط", "شرميط",
        "تفو", "تفو عليك", "يلعن", "لعنة", "لعن", "العن", "يلعنك", "يلعن ابوك", "يلعن امك", "يلعن اهلك",
        "حمار", "يا حمار", "غبي", "حيوان", "ديوث", "قواد",

        // Attacker handles, taunts & hacking defacement terms
        "ابلع", "اختراق", "اخترق", "تم اختراقه", "تم الاختراق", "زعطوط", "علاوي", "norerror",
        "مواليد", "مهكر", "تهكير", "hack", "hacked", "hacker", "pwned", "exploit", "تخسرون", "حذف جميع بياناته",
        "مواليد 2008", "@norerror", "nor_error", "nor error", "طرد", "تم دعس", "تم الدعس",
        "باي باي", "هكر", "الهكر"
    )

    private val SUSPICIOUS_VERSION_PATTERNS = listOf(
        "666", "777", "999"
    )

    /**
     * Normalizes an Arabic string by stripping diacritics, tatweel, unifying letters,
     * and collapsing repeated characters.
     */
    private fun normalizeArabic(text: String): String {
        return text.lowercase().trim()
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
            .replace(Regex("[إأآٱ]"), "ا")
            .replace("ة", "ه")
            .replace(Regex("[ىئ]"), "ي")
            .replace("ؤ", "و")
    }

    /**
     * Collapses repeated characters (e.g. "ككككحححبة" -> "كحبه")
     */
    private fun collapseRepetitions(text: String): String {
        val sb = StringBuilder()
        var lastChar = ' '
        for (ch in text) {
            if (ch != lastChar) {
                sb.append(ch)
                lastChar = ch
            }
        }
        return sb.toString()
    }

    /**
     * Checks if the incoming text from server contains abusive, defamatory, or attack keywords.
     */
    fun isMaliciousText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val normalized = normalizeArabic(text)
        val collapsed = collapseRepetitions(normalized)
        val stripped = normalized.replace(Regex("[^a-zA-Z0-9\\u0621-\\u064A]"), "")
        val strippedCollapsed = collapseRepetitions(stripped)

        return FORBIDDEN_WORDS.any { keyword ->
            val normKeyword = normalizeArabic(keyword)
            val collapsedKeyword = collapseRepetitions(normKeyword)
            val strippedKeyword = normKeyword.replace(Regex("[^a-zA-Z0-9\\u0621-\\u064A]"), "")

            normalized.contains(normKeyword) ||
            collapsed.contains(collapsedKeyword) ||
            (strippedKeyword.length >= 3 && stripped.contains(strippedKeyword)) ||
            (strippedKeyword.length >= 3 && strippedCollapsed.contains(strippedKeyword))
        }
    }

    /**
     * Validates whether a Channel name or category is malicious.
     */
    fun isMaliciousChannel(name: String?, category: String? = null): Boolean {
        if (isMaliciousText(name)) return true
        if (!category.isNullOrBlank() && isMaliciousText(category)) return true
        return false
    }

    /**
     * Sanitizes chat comments or video comments to completely strip abuse or replace with clean message.
     */
    fun sanitizeComment(text: String?): String {
        if (text.isNullOrBlank()) return ""
        if (isMaliciousText(text)) {
            return "تم حجب التعليق لمخالفته إرشادات الاستخدام 🛡️"
        }
        return text.trim()
    }

    /**
     * Validates whether a version number is reasonable.
     * Supports all legitimate semantic versions (e.g. 1.0.0 - 99.9.9).
     */
    fun isSuspiciousVersion(rawVersion: String?): Boolean {
        if (rawVersion.isNullOrBlank()) return false
        val cleaned = rawVersion.trim().removePrefix("v").removePrefix("V").trim()

        if (SUSPICIOUS_VERSION_PATTERNS.contains(cleaned)) {
            Log.w(TAG, "Blocked suspicious version pattern: $cleaned")
            return true
        }

        val parts = cleaned.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return true
        if (major < 1 || major > 99) {
            Log.w(TAG, "Blocked illegitimate major version: $cleaned")
            return true
        }

        return false
    }

    /**
     * Sanitizes any update text, ensuring users NEVER see offensive, defacing, or hateful content.
     */
    fun sanitizeUpdateText(text: String?): String {
        if (text.isNullOrBlank() || isMaliciousText(text)) {
            return "يتوفر إصدار جديد من تطبيق FOUL لتحسين استقرار البث المباشر وتحديث القنوات والمكتبة."
        }
        return text.trim()
    }

    /**
     * Determines whether an update is genuine and legitimate.
     */
    fun isLegitimateUpdate(currentVersion: String, targetVersion: String, updateText: String): Boolean {
        if (isMaliciousText(updateText)) return false
        if (isSuspiciousVersion(targetVersion)) return false

        val currNorm = currentVersion.trim().removePrefix("v").removePrefix("V")
        val targNorm = targetVersion.trim().removePrefix("v").removePrefix("V")

        if (currNorm == targNorm) return false

        try {
            val currParts = currNorm.split(".").map { it.toIntOrNull() ?: 0 }
            val targParts = targNorm.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(currParts.size, targParts.size)
            for (i in 0 until maxLen) {
                val c = currParts.getOrElse(i) { 0 }
                val t = targParts.getOrElse(i) { 0 }
                if (c < t) return true
                if (c > t) return false
            }
        } catch (_: Exception) {}

        return false
    }

    fun isSafeChannel(channel: Any?): Boolean = true
    fun isSafeCategory(category: Any?): Boolean = true
    fun isSafeBranch(branch: Any?): Boolean = true
    fun isSafeXtremeAccount(account: Any?): Boolean = true
    fun isSafeBanner(banner: Any?): Boolean = true
    fun sanitizeAppConfig(map: Map<String, Any?>): Map<String, Any?> = map
}
