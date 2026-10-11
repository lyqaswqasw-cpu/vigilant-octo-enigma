package com.example.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Professional Full-Screen Update Experience.
 * Supports both:
 * 1. Mandatory (Force) Update (`isMandatory = true`): Blocks back navigation and requires updating.
 *    (Also provides a Developer Admin Access button so the developer is never locked out of the admin panel).
 * 2. Optional Update (`isMandatory = false`): Displays update notes & download button with a clean
 *    "تخطي والمتابعة للتطبيق" (Skip & Continue) button.
 */
@Composable
fun ForceUpdateScreen(
    currentVersion: String,
    latestVersion: String,
    updateMessage: String = "",
    downloadUrl: String = "",
    telegramSupportUrl: String = "https://t.me/jdj_q",
    isMandatory: Boolean = true,
    onDismissOptional: () -> Unit = {},
    onDeveloperBypass: () -> Unit = {},
    updateText: String = updateMessage,
    updateUrl: String = downloadUrl,
    telegramUrl: String = telegramSupportUrl,
    onOpenSettings: () -> Unit = {},
    onDismiss: () -> Unit = onDismissOptional,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    // Handle back button according to update mode
    BackHandler(enabled = true) {
        if (!isMandatory) {
            onDismissOptional()
        }
        // When mandatory, back press is intentionally blocked
    }

    val infiniteTransition = rememberInfiniteTransition(label = "update_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val badgeColor = if (isMandatory) ElectricGold else AccentCyan

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0614),
                        PitchBlack,
                        Color(0xFF090410)
                    )
                )
            )
            .padding(24.dp)
            .testTag("force_update_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    colors = if (isMandatory) {
                        listOf(NeonPurple, ElectricGold, ElectricPurple)
                    } else {
                        listOf(AccentCyan, NeonPurple, ElectricPurple)
                    }
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Mode Badge (إجباري vs اختياري)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isMandatory) Icons.Default.Security else Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isMandatory) "تحديث إجباري مطلوب" else "تحديث جديد متاح (اختياري)",
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Animated Rocket / Update Icon Badge
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ElectricPurple.copy(alpha = 0.38f),
                                    Color(0xFF140B29)
                                )
                            )
                        )
                        .border(2.dp, NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "تحديث التطبيق",
                        tint = BrightLavender,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Version Comparison Pill (Current vs New)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF161622),
                        border = BorderStroke(1.dp, Color(0xFF2A2A3C))
                    ) {
                        Text(
                            text = "الحالي: v$currentVersion",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = "⬅",
                        color = AccentCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PitchGreen.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PitchGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = PitchGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "الجديد: v$latestVersion",
                                color = PitchGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Headline
                Text(
                    text = if (isMandatory) "تحديث هام وإجباري للتطبيق" else "يتوفر إصدار أحدث من التطبيق",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Update Message Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0E0E18),
                    border = BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = updateMessage.ifBlank {
                            "لقد قمنا بإصدار نسخة جديدة تتضمن تحسينات كبيرة في سرعة البث المباشر، قنوات إضافية، وإصلاحات هامة للاستقرار."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Primary Update Download Action Button
                val targetUrl = downloadUrl.ifBlank { telegramSupportUrl }.ifBlank { "https://t.me/Foul_tv" }

                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(targetUrl)
                        } catch (_: Exception) {
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricPurple,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("force_update_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تحديث التطبيق الآن (v$latestVersion)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Optional Update Skip Button
                if (!isMandatory) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onDismissOptional,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("optional_update_skip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تخطي والمتابعة للتطبيق",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Secondary Telegram Channel Button if different from downloadUrl
                if (telegramSupportUrl.isNotBlank() && telegramSupportUrl != downloadUrl) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                uriHandler.openUri(telegramSupportUrl)
                            } catch (_: Exception) {
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("force_update_telegram_button")
                    ) {
                        Text(
                            text = "تحميل التحديث عبر قناة التلجرام الرسمية",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Developer Admin Quick Bypass so the developer can always edit version config
                TextButton(
                    onClick = onDeveloperBypass,
                    modifier = Modifier.testTag("developer_update_bypass_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "دخول لوحة المطور لتعديل إعدادات الإصدار",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
