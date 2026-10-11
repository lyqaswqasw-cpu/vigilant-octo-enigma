package com.example.ui.publisher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import com.example.ui.theme.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.Channel
import com.example.data.model.ChannelCategory
import com.example.data.model.XtremeAccount
import com.example.data.util.ChannelClusteringEngine
import com.example.data.util.M3uParser
import com.example.data.xtreme.XtremeApiClient
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import kotlinx.coroutines.launch

private enum class CategorySourceMode(val label: String) {
    NONE("بدون مصدر (يدوي / اختياري)"),
    M3U("رابط قائمة M3U"),
    XTREME("حساب Xtream Codes")
}

@Composable
fun CategoryPublisherTab(
    categories: List<ChannelCategory>,
    xtremeAccounts: List<XtremeAccount> = emptyList(),
    onSaveCategory: (ChannelCategory) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onPublishChannelFromPreview: (Channel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val xtremeClient = remember { XtremeApiClient() }

    var editingId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var orderText by remember { mutableStateOf("0") }
    var sourceMode by remember { mutableStateOf(CategorySourceMode.NONE) }

    // M3U Source
    var m3uUrl by remember { mutableStateOf("") }

    // Xtreme Source
    var xtremeHost by remember { mutableStateOf("") }
    var xtremeUsername by remember { mutableStateOf("") }
    var xtremePassword by remember { mutableStateOf("") }
    var xtremeExtension by remember { mutableStateOf("m3u8") }

    // Preview & Search state for all channels (unlimited count) + merged quality servers at top
    var previewChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var rawStreamsCount by remember { mutableIntStateOf(0) }
    var isFetchingPreview by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var channelSearchQuery by remember { mutableStateOf("") }
    var selectedPreviewChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedPreviewServerId by remember { mutableStateOf<String?>(null) }

    val filteredPreviewChannels = remember(previewChannels, channelSearchQuery) {
        val q = channelSearchQuery.trim()
        if (q.isBlank()) {
            previewChannels
        } else {
            previewChannels.filter { ch ->
                ch.name.contains(q, ignoreCase = true) ||
                        ch.category.contains(q, ignoreCase = true) ||
                        ch.servers.any { srv -> srv.name.contains(q, ignoreCase = true) }
            }
        }
    }

    // Active channel for top merged quality servers bar
    val topFeaturedChannel = remember(selectedPreviewChannel, filteredPreviewChannels, channelSearchQuery) {
        if (selectedPreviewChannel != null && filteredPreviewChannels.any { it.id == selectedPreviewChannel?.id }) {
            selectedPreviewChannel
        } else if (channelSearchQuery.isNotBlank() && filteredPreviewChannels.isNotEmpty()) {
            filteredPreviewChannels.first()
        } else {
            selectedPreviewChannel ?: filteredPreviewChannels.firstOrNull { it.servers.size > 1 } ?: filteredPreviewChannels.firstOrNull()
        }
    }

    fun loadCategoryChannelsPreview(
        targetCategoryName: String,
        mode: CategorySourceMode,
        targetM3u: String,
        host: String,
        user: String,
        pass: String,
        ext: String
    ) {
        coroutineScope.launch {
            isFetchingPreview = true
            previewError = null
            try {
                when (mode) {
                    CategorySourceMode.M3U -> {
                        if (targetM3u.isBlank()) {
                            previewError = "يرجى إدخال رابط M3U أولاً لعرض القنوات"
                        } else {
                            val parsed = M3uParser.parse(targetM3u.trim(), targetCategoryName.ifBlank { "عام" })
                            previewChannels = parsed
                            rawStreamsCount = parsed.sumOf { it.servers.size }
                            selectedPreviewChannel = parsed.firstOrNull()
                            selectedPreviewServerId = parsed.firstOrNull()?.servers?.firstOrNull()?.id
                            if (parsed.isEmpty()) {
                                previewError = "لم يتم العثور على قنوات في رابط M3U المحدد"
                            }
                        }
                    }
                    CategorySourceMode.XTREME -> {
                        if (host.isBlank() || user.isBlank() || pass.isBlank()) {
                            previewError = "يرجى إدخال بيانات حساب Xtream (السيرفر واسم المستخدم وكلمة المرور)"
                        } else {
                            val tempAcc = XtremeAccount(
                                name = targetCategoryName.ifBlank { "حساب القسم" },
                                host = host.trim(),
                                username = user.trim(),
                                password = pass.trim(),
                                streamExtension = ext.ifBlank { "m3u8" }
                            )
                            val liveItems = xtremeClient.fetchLiveStreams(tempAcc)
                            rawStreamsCount = liveItems.size
                            val clustered = ChannelClusteringEngine.clusterXtremeLiveItems(
                                items = liveItems,
                                forcedCategoryName = targetCategoryName.ifBlank { "عام" }
                            )
                            previewChannels = clustered
                            selectedPreviewChannel = clustered.firstOrNull()
                            selectedPreviewServerId = clustered.firstOrNull()?.servers?.firstOrNull()?.id
                            if (clustered.isEmpty()) {
                                previewError = "تعذر جلب قنوات من حساب Xtreme أو الحساب لا يحتوي على قنوات مباشرة"
                            }
                        }
                    }
                    CategorySourceMode.NONE -> {
                        previewChannels = emptyList()
                        rawStreamsCount = 0
                    }
                }
            } catch (e: Exception) {
                previewError = "خطأ أثناء جلب القنوات: ${e.localizedMessage ?: "تأكد من الاتصال والبيانات"}"
            } finally {
                isFetchingPreview = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. CATEGORY CREATION / EDITING FORM CARD
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingId.isBlank()) "نشر قسم قنوات جديد (يدوي / M3U / Xtreme)" else "تعديل القسم المحدد",
                            color = ElectricGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Surface(
                            color = PitchGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, PitchGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "متصل أونلاين",
                                color = PitchGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم القسم (مثال: دوري أبطال أوروبا)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PitchGreen,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("category_name_input")
                        )

                        OutlinedTextField(
                            value = orderText,
                            onValueChange = { orderText = it },
                            label = { Text("الترتيب") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SOURCE MODE SELECTOR (بدون مصدر / رابط M3U / حساب Xtreme)
                    Text(
                        text = "نوع مصدر القنوات لهذا القسم (اختياري):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategorySourceMode.values().forEach { mode ->
                            val isSelected = sourceMode == mode
                            val icon = when (mode) {
                                CategorySourceMode.NONE -> Icons.Default.Category
                                CategorySourceMode.M3U -> Icons.Default.Link
                                CategorySourceMode.XTREME -> Icons.Default.Dns
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ElectricPurple.copy(alpha = 0.28f) else CardSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonPurple else TextMuted.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { sourceMode = mode }
                                    .testTag("category_source_mode_${mode.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) ElectricGold else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = mode.label,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // SOURCE MODE 1: NONE (STANDALONE CATEGORY)
                    AnimatedVisibility(visible = sourceMode == CategorySourceMode.NONE) {
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .border(1.dp, TextMuted.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = "سيتم نشر هذا القسم بشكل مستقل بدون رابط M3U وبدون حساب Xtreme (يمكنك إضافة القنوات أو الفروع إليه يدوياً في أي وقت).",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // SOURCE MODE 2: M3U PLAYLIST URL
                    AnimatedVisibility(visible = sourceMode == CategorySourceMode.M3U) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text("رابط قائمة M3U للقسم (يقبل أي عدد من القنوات)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_m3u_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    loadCategoryChannelsPreview(
                                        targetCategoryName = name,
                                        mode = CategorySourceMode.M3U,
                                        targetM3u = m3uUrl,
                                        host = "",
                                        user = "",
                                        pass = "",
                                        ext = "m3u8"
                                    )
                                },
                                border = BorderStroke(1.dp, AccentCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جلب وعرض جميع القنوات ودمج الجودات (معاينة وبحث)", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // SOURCE MODE 3: XTREME CODES ACCOUNT
                    AnimatedVisibility(visible = sourceMode == CategorySourceMode.XTREME) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            // Quick fill from existing Xtreme accounts if available
                            if (xtremeAccounts.isNotEmpty()) {
                                Text(
                                    text = "اختيار سريع من الحسابات المحفوظة (اختياري):",
                                    color = BrightLavender,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(xtremeAccounts, key = { it.id }) { acc ->
                                        val isCurrentAcc = xtremeHost == acc.host && xtremeUsername == acc.username
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrentAcc) PitchGreen.copy(alpha = 0.22f) else CardSurface,
                                            border = BorderStroke(1.dp, if (isCurrentAcc) PitchGreen else TextMuted.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    xtremeHost = acc.host
                                                    xtremeUsername = acc.username
                                                    xtremePassword = acc.password
                                                    xtremeExtension = acc.streamExtension.ifBlank { "m3u8" }
                                                    if (name.isBlank()) name = acc.name
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Dns, contentDescription = null, tint = ElectricGold, modifier = Modifier.size(13.dp))
                                                Text(acc.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            OutlinedTextField(
                                value = xtremeHost,
                                onValueChange = { xtremeHost = it },
                                label = { Text("عنوان سيرفر Xtreme (مثال: http://server.com:8080)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricGold,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_xtreme_host_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = xtremeUsername,
                                    onValueChange = { xtremeUsername = it },
                                    label = { Text("اسم المستخدم (Username)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricGold,
                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("category_xtreme_user_input")
                                )

                                OutlinedTextField(
                                    value = xtremePassword,
                                    onValueChange = { xtremePassword = it },
                                    label = { Text("كلمة المرور (Password)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricGold,
                                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("category_xtreme_pass_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("امتداد البث:", color = TextSecondary, fontSize = 11.5.sp)
                                listOf("m3u8", "ts").forEach { ext ->
                                    val selected = xtremeExtension.equals(ext, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) AccentCyan.copy(alpha = 0.22f) else CardSurface,
                                        border = BorderStroke(1.dp, if (selected) AccentCyan else TextMuted.copy(alpha = 0.3f)),
                                        modifier = Modifier.clickable { xtremeExtension = ext }
                                    ) {
                                        Text(
                                            text = ext.uppercase(),
                                            color = if (selected) AccentCyan else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    loadCategoryChannelsPreview(
                                        targetCategoryName = name,
                                        mode = CategorySourceMode.XTREME,
                                        targetM3u = "",
                                        host = xtremeHost,
                                        user = xtremeUsername,
                                        pass = xtremePassword,
                                        ext = xtremeExtension
                                    )
                                },
                                border = BorderStroke(1.dp, ElectricGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fetch_xtreme_category_channels_button")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = ElectricGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "جلب وعرض جميع قنوات حساب Xtreme ودمج الجودات",
                                    color = ElectricGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editingId.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    editingId = ""
                                    name = ""
                                    orderText = "0"
                                    sourceMode = CategorySourceMode.NONE
                                    m3uUrl = ""
                                    xtremeHost = ""
                                    xtremeUsername = ""
                                    xtremePassword = ""
                                    xtremeExtension = "m3u8"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء التعديل", color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val finalM3u = if (sourceMode == CategorySourceMode.M3U) m3uUrl.trim() else ""
                                    val finalHost = if (sourceMode == CategorySourceMode.XTREME) xtremeHost.trim() else ""
                                    val finalUser = if (sourceMode == CategorySourceMode.XTREME) xtremeUsername.trim() else ""
                                    val finalPass = if (sourceMode == CategorySourceMode.XTREME) xtremePassword.trim() else ""

                                    val cat = ChannelCategory(
                                        id = editingId,
                                        name = name.trim(),
                                        order = orderText.toIntOrNull() ?: 0,
                                        m3uUrl = finalM3u,
                                        xtremeHost = finalHost,
                                        xtremeUsername = finalUser,
                                        xtremePassword = finalPass,
                                        xtremeExtension = xtremeExtension.ifBlank { "m3u8" }
                                    )
                                    onSaveCategory(cat)
                                    editingId = ""
                                    name = ""
                                    orderText = "0"
                                    sourceMode = CategorySourceMode.NONE
                                    m3uUrl = ""
                                    xtremeHost = ""
                                    xtremeUsername = ""
                                    xtremePassword = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("save_category_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (editingId.isBlank()) "نشر وحفظ القسم" else "تحديث القسم",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // 2. LOADING / ERROR FOR CHANNEL BROWSER & QUALITY SERVER MERGER
        if (isFetchingPreview) {
            item {
                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricGold, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                        Column {
                            Text("جاري جلب جميع القنوات ودمج الجودات تلقائياً...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("يدعم جلب الآلاف من القنوات وفرز جوداتها (4K / FHD / HD / SD / LOW) في سيرفرات مدمجة", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (!previewError.isNullOrBlank()) {
            item {
                Surface(
                    color = LiveRed.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = previewError ?: "",
                        color = LiveRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 3. INTERACTIVE ALL-CHANNELS BROWSER + SEARCH + TOP MERGED SERVERS BAR
        if (previewChannels.isNotEmpty()) {
            item {
                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ElectricGold, AccentCyan, NeonPurple))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header with total counts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "مستعرض القنوات والجودات المدمجة (${previewChannels.size} قناة مدمجة)",
                                    color = ElectricGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "تم دمج $rawStreamsCount بث مباشر في ${previewChannels.size} قناة موحدة مع كافة جوداتها",
                                    color = PitchGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { previewChannels = emptyList() }) {
                                Icon(Icons.Default.Clear, contentDescription = "إغلاق المعاينة", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search bar for finding any channel among thousands
                        OutlinedTextField(
                            value = channelSearchQuery,
                            onValueChange = { channelSearchQuery = it },
                            placeholder = { Text("ابحث عن أي قناة لعرضها مع جميع جوداتها المدمجة...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan)
                            },
                            trailingIcon = {
                                if (channelSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { channelSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_preview_search_input")
                        )

                        // TOP MERGED QUALITY SERVERS BAR FOR SELECTED / SEARCHED CHANNEL
                        if (topFeaturedChannel != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFF101322),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, ElectricGold.copy(alpha = 0.65f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HighQuality,
                                                contentDescription = null,
                                                tint = ElectricGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = topFeaturedChannel.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "السيرفرات والجودات المدمجة في الأعلى (${topFeaturedChannel.servers.size} جودات متوفرة)",
                                                    color = AccentCyan,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val channelToPublish = topFeaturedChannel.copy(
                                                    category = name.trim().ifBlank { topFeaturedChannel.category }
                                                )
                                                onPublishChannelFromPreview(channelToPublish)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PitchGreen,
                                                contentColor = Color.Black
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نشر القناة", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Merged Servers Row at the top
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(topFeaturedChannel.servers, key = { it.id }) { srv ->
                                            val isSelectedSrv = (selectedPreviewServerId ?: topFeaturedChannel.servers.firstOrNull()?.id) == srv.id
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelectedSrv) VividViolet else CardSurface,
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelectedSrv) ElectricGold else NeonPurple.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { selectedPreviewServerId = srv.id }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    if (isSelectedSrv) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(PitchGreen, CircleShape)
                                                        )
                                                    }
                                                    Text(
                                                        text = srv.name,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "نتائج القنوات (${filteredPreviewChannels.size} قناة) — اضغط على أي قناة لعرض جوداتها المدمجة في الأعلى:",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Virtualized LazyColumn inside bounded height so even 50,000 channels scroll smoothly at 60fps
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 380.dp)
                                .background(CardSurface, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredPreviewChannels, key = { it.id }) { ch ->
                                val isSelected = topFeaturedChannel?.id == ch.id
                                Surface(
                                    color = if (isSelected) ElectricPurple.copy(alpha = 0.22f) else CardSurfaceElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) ElectricGold else TextMuted.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedPreviewChannel = ch
                                            selectedPreviewServerId = ch.servers.firstOrNull()?.id
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (ch.imageUrl.isNotBlank()) {
                                                    SubcomposeAsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(ch.imageUrl)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = ch.name,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.Black),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.LiveTv,
                                                        contentDescription = null,
                                                        tint = AccentCyan,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = ch.name,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${ch.servers.size} سيرفرات جودة مدمجة",
                                                        color = PitchGreen,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // Quality server pills inline preview
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ch.servers.take(3).forEach { srv ->
                                                    Surface(
                                                        color = DarkPurpleContainer,
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(0.6.dp, NeonPurple.copy(alpha = 0.6f))
                                                    ) {
                                                        Text(
                                                            text = srv.name.replace("سيرفر ", ""),
                                                            color = BrightLavender,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (ch.servers.size > 3) {
                                                    Text(
                                                        text = "+${ch.servers.size - 3}",
                                                        color = ElectricGold,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. EXISTING PUBLISHED CATEGORIES LIST
        item {
            Text(
                text = "الأقسام المنشورة حالياً (${categories.size})",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        items(categories, key = { it.id.ifBlank { it.name } }) { cat ->
            val sourceBadgeText = when {
                cat.hasXtremeSource -> "حساب Xtreme (${cat.xtremeUsername})"
                cat.hasM3uSource -> "رابط قائمة M3U"
                else -> "قسم يدوي (بدون رابط)"
            }
            val sourceBadgeColor = when {
                cat.hasXtremeSource -> ElectricGold
                cat.hasM3uSource -> AccentCyan
                else -> TextSecondary
            }

            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TextMuted.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = when {
                                cat.hasXtremeSource -> Icons.Default.Dns
                                cat.hasM3uSource -> Icons.Default.Link
                                else -> Icons.Default.Category
                            },
                            contentDescription = null,
                            tint = sourceBadgeColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = cat.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "الترتيب: ${cat.order}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• $sourceBadgeText",
                                    color = sourceBadgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (cat.hasXtremeSource || cat.hasM3uSource) {
                            IconButton(
                                onClick = {
                                    name = cat.name
                                    if (cat.hasXtremeSource) {
                                        sourceMode = CategorySourceMode.XTREME
                                        xtremeHost = cat.xtremeHost
                                        xtremeUsername = cat.xtremeUsername
                                        xtremePassword = cat.xtremePassword
                                        xtremeExtension = cat.xtremeExtension
                                        loadCategoryChannelsPreview(
                                            targetCategoryName = cat.name,
                                            mode = CategorySourceMode.XTREME,
                                            targetM3u = "",
                                            host = cat.xtremeHost,
                                            user = cat.xtremeUsername,
                                            pass = cat.xtremePassword,
                                            ext = cat.xtremeExtension
                                        )
                                    } else {
                                        sourceMode = CategorySourceMode.M3U
                                        m3uUrl = cat.m3uUrl
                                        loadCategoryChannelsPreview(
                                            targetCategoryName = cat.name,
                                            mode = CategorySourceMode.M3U,
                                            targetM3u = cat.m3uUrl,
                                            host = "",
                                            user = "",
                                            pass = "",
                                            ext = "m3u8"
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "عرض وبحث قنوات القسم",
                                    tint = AccentCyan
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                editingId = cat.id
                                name = cat.name
                                orderText = cat.order.toString()
                                m3uUrl = cat.m3uUrl
                                xtremeHost = cat.xtremeHost
                                xtremeUsername = cat.xtremeUsername
                                xtremePassword = cat.xtremePassword
                                xtremeExtension = cat.xtremeExtension.ifBlank { "m3u8" }
                                sourceMode = when {
                                    cat.hasXtremeSource -> CategorySourceMode.XTREME
                                    cat.hasM3uSource -> CategorySourceMode.M3U
                                    else -> CategorySourceMode.NONE
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل القسم",
                                tint = ElectricGold
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCategory(cat.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف القسم",
                                tint = LiveRed
                            )
                        }
                    }
                }
            }
        }
    }
}
