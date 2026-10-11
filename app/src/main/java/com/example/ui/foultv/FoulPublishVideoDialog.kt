package com.example.ui.foultv

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FoulVideo
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FoulPublishVideoDialog(
    initialVideo: FoulVideo? = null,
    onDismiss: () -> Unit,
    onPublish: (FoulVideo) -> Unit
) {
    var title by remember { mutableStateOf(initialVideo?.title ?: "") }
    var description by remember { mutableStateOf(initialVideo?.description ?: "") }
    var videoUrl by remember { mutableStateOf(initialVideo?.videoUrl ?: "") }
    var thumbnailUrl by remember { mutableStateOf(initialVideo?.thumbnailUrl ?: "") }
    var duration by remember { mutableStateOf(initialVideo?.duration ?: "") }
    var category by remember { mutableStateOf(initialVideo?.category ?: "أهداف وملخصات") }
    var isViewsHidden by remember { mutableStateOf(initialVideo?.isViewsHidden ?: false) }
    var isLikesDisabled by remember { mutableStateOf(initialVideo?.isLikesDisabled ?: false) }
    var isCommentsDisabled by remember { mutableStateOf(initialVideo?.isCommentsDisabled ?: false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(AccentCyan, PitchGreen)),
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialVideo == null) "نشر فيديو جديد في FOUL TV" else "تعديل فيديو FOUL TV",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricGold,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorText = null },
                    label = { Text("عنوان الفيديو") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PitchGreen,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it; errorText = null },
                    label = { Text("رابط الفيديو المباشر (MP4 / M3U8)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text("رابط الصورة المصغرة (اختياري)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("المدة (مثال: 05:30)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الفيديو") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إخفاء المشاهدات لهذا الفيديو", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isViewsHidden,
                        onCheckedChange = { isViewsHidden = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricGold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تعطيل/إخفاء الإعجابات لهذا الفيديو", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isLikesDisabled,
                        onCheckedChange = { isLikesDisabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricGold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تعطيل التعليقات لهذا الفيديو", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isCommentsDisabled,
                        onCheckedChange = { isCommentsDisabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricGold)
                    )
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorText!!, color = Color(0xFFFF6B81), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank() || videoUrl.isBlank()) {
                                errorText = "يرجى إدخال عنوان الفيديو والرابط المباشر"
                            } else {
                                val video = (initialVideo ?: FoulVideo()).copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    videoUrl = videoUrl.trim(),
                                    thumbnailUrl = thumbnailUrl.trim(),
                                    duration = duration.trim(),
                                    category = category.trim(),
                                    isViewsHidden = isViewsHidden,
                                    isLikesDisabled = isLikesDisabled,
                                    isCommentsDisabled = isCommentsDisabled
                                )
                                onPublish(video)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchGreen,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_publish_video_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialVideo == null) "نشر الآن" else "حفظ التعديلات",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
