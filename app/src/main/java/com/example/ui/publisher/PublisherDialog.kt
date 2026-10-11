package com.example.ui.publisher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.matches.MatchItem
import com.example.data.model.Channel
import com.example.data.model.ChannelBranch
import com.example.data.model.ChannelCategory
import com.example.data.model.ChannelServer
import com.example.data.model.EventBanner
import com.example.data.model.FoulTvGlobalConfig
import com.example.data.model.FoulVideo
import com.example.data.model.XtremeAccount
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.UUID

/**
 * Full-Featured Developer Control Panel (Admin Publisher Dialog).
 * Strictly protected by Firebase Authentication (AdminAuthManager & FirebaseAuth).
 */
@Composable
fun PublisherDialog(
    initialTabIndex: Int = 0,
    initialEditingChannel: Channel? = null,
    adminEmail: String? = null,
    channels: List<Channel>,
    categories: List<ChannelCategory>,
    branches: List<ChannelBranch>,
    firebaseMatches: List<MatchItem>,
    xtremeAccounts: List<XtremeAccount>,
    eventBanners: List<EventBanner>,
    foulVideos: List<FoulVideo>,
    foulGlobalConfig: FoulTvGlobalConfig,
    installedAppVersion: String = "4.0.0",
    latestAppVersion: String,
    forceUpdateRequired: Boolean,
    optionalUpdateAvailable: Boolean = false,
    updateMessage: String,
    updateDownloadUrl: String,
    telegramSupportUrl: String,
    developerStatusMessage: String? = null,
    showChannelsTab: Boolean = true,
    showMatchesTab: Boolean = true,
    showIptvTab: Boolean = true,
    showFoulTab: Boolean = true,
    showLibraryTab: Boolean = true,
    onDismiss: () -> Unit,
    onSignOutAdmin: () -> Unit = {},
    onSaveChannel: (Channel) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onClearAllChannels: () -> Unit,
    onSaveCategory: (ChannelCategory) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveBranch: (ChannelBranch) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onSaveLiveMatch: (
        eventId: String,
        name: String,
        tournament: String,
        broadcaster: String,
        imageUrl: String,
        startTimeMs: Long,
        targetChannelId: String?,
        servers: List<ChannelServer>
    ) -> Unit,
    onDeleteLiveMatch: (String) -> Unit,
    onSaveXtremeAccount: (XtremeAccount) -> Unit,
    onDeleteXtremeAccount: (String) -> Unit,
    onSaveEventBanner: (EventBanner) -> Unit,
    onDeleteEventBanner: (String) -> Unit,
    onUpdateFoulGlobalConfig: (FoulTvGlobalConfig) -> Unit,
    onOpenPublishFoulVideo: () -> Unit,
    onEditFoulVideo: (FoulVideo) -> Unit,
    onDeleteFoulVideo: (String) -> Unit,
    onSaveAppConfig: (
        latestVersion: String,
        forceUpdate: Boolean,
        optionalUpdate: Boolean,
        updateMessage: String,
        updateUrl: String,
        telegramSupportUrl: String,
        newDeveloperPass: String?
    ) -> Unit,
    onSaveBottomBarVisibility: (
        showChannelsTab: Boolean,
        showMatchesTab: Boolean,
        showIptvTab: Boolean,
        showFoulTab: Boolean,
        showLibraryTab: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    onPlayChannelPreview: (Channel, ChannelServer?) -> Unit = { _, _ -> }
) {
    var selectedTab by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .testTag("developer_admin_dialog"),
            color = DarkBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = CardSurfaceElevated,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(PitchGreen, AccentCyan)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "لوحة تحكم المطور (Admin Panel)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "متصل كـ: ${adminEmail ?: "Admin"} • صلاحيات قراءة وكتابة كاملة",
                                        color = PitchGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onSignOutAdmin,
                                    modifier = Modifier.testTag("admin_logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "تسجيل خروج المطور",
                                        tint = ElectricGold
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("close_publisher_dialog_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق لوحة التحكم",
                                        tint = TextPrimary
                                    )
                                }
                            }
                        }

                        if (!developerStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PitchGreen.copy(alpha = 0.16f),
                                border = BorderStroke(0.8.dp, PitchGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = developerStatusMessage,
                                    color = PitchGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Admin Navigation Tabs (9 Tabs including the new Bottom Bar Control Tab)
                        val tabs = listOf(
                            Triple(0, "القنوات (${channels.size})", Icons.Default.LiveTv),
                            Triple(1, "الأقسام (${categories.size})", Icons.Default.Category),
                            Triple(2, "الفروع (${branches.size})", Icons.Default.AccountTree),
                            Triple(3, "المباريات (${firebaseMatches.size})", Icons.Default.SportsSoccer),
                            Triple(4, "البانرات (${eventBanners.size})", Icons.Default.ViewCarousel),
                            Triple(5, "حسابات IPTV/المكتبة (${xtremeAccounts.size})", Icons.Default.Dns),
                            Triple(6, "الشريط السفلي", Icons.Default.ViewHeadline),
                            Triple(7, "فاول TV (${foulVideos.size})", Icons.Default.PlayCircle),
                            Triple(8, "الإصدار والتحديثات", Icons.Default.Settings)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            itemsIndexed(tabs) { idx, (_, title, icon) ->
                                FilterChip(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    label = {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PitchGreen.copy(alpha = 0.22f),
                                        selectedLabelColor = PitchGreen,
                                        selectedLeadingIconColor = PitchGreen
                                    ),
                                    modifier = Modifier.testTag("admin_tab_$idx")
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))

                // Selected Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> ChannelsAdminTab(
                            initialEditingChannel = initialEditingChannel,
                            channels = channels,
                            categories = categories,
                            branches = branches,
                            onSaveChannel = onSaveChannel,
                            onDeleteChannel = onDeleteChannel,
                            onClearAllChannels = onClearAllChannels
                        )
                        1 -> CategoryPublisherTab(
                            categories = categories,
                            xtremeAccounts = xtremeAccounts,
                            onSaveCategory = onSaveCategory,
                            onDeleteCategory = onDeleteCategory,
                            onPublishChannelFromPreview = { ch -> onPlayChannelPreview(ch, null) }
                        )
                        2 -> BranchPublisherTab(
                            branches = branches,
                            categories = categories,
                            onSaveBranch = onSaveBranch,
                            onDeleteBranch = onDeleteBranch
                        )
                        3 -> MatchesEventsAdminTab(
                            matches = firebaseMatches,
                            channels = channels,
                            onSaveLiveMatch = onSaveLiveMatch,
                            onDeleteLiveMatch = onDeleteLiveMatch
                        )
                        4 -> BannersAdminTab(
                            banners = eventBanners,
                            channels = channels,
                            onSaveBanner = onSaveEventBanner,
                            onDeleteBanner = onDeleteEventBanner
                        )
                        5 -> XtremeAccountsAdminTab(
                            accounts = xtremeAccounts,
                            onSaveAccount = onSaveXtremeAccount,
                            onDeleteAccount = onDeleteXtremeAccount
                        )
                        6 -> BottomBarAdminTab(
                            showChannelsTab = showChannelsTab,
                            showMatchesTab = showMatchesTab,
                            showIptvTab = showIptvTab,
                            showFoulTab = showFoulTab,
                            showLibraryTab = showLibraryTab,
                            onSaveBottomBarVisibility = onSaveBottomBarVisibility
                        )
                        7 -> FoulTvManagerTab(
                            videos = foulVideos,
                            globalConfig = foulGlobalConfig,
                            onUpdateGlobalConfig = onUpdateFoulGlobalConfig,
                            onOpenPublishVideo = onOpenPublishFoulVideo,
                            onEditVideo = onEditFoulVideo,
                            onDeleteVideo = onDeleteFoulVideo
                        )
                        8 -> AppConfigAdminTab(
                            installedAppVersion = installedAppVersion,
                            latestAppVersion = latestAppVersion,
                            forceUpdateRequired = forceUpdateRequired,
                            optionalUpdateAvailable = optionalUpdateAvailable,
                            updateMessage = updateMessage,
                            updateDownloadUrl = updateDownloadUrl,
                            telegramSupportUrl = telegramSupportUrl,
                            onSaveAppConfig = onSaveAppConfig
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 0: CHANNELS ADMIN TAB
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelsAdminTab(
    initialEditingChannel: Channel?,
    channels: List<Channel>,
    categories: List<ChannelCategory>,
    branches: List<ChannelBranch>,
    onSaveChannel: (Channel) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onClearAllChannels: () -> Unit
) {
    var editingId by remember(initialEditingChannel) { mutableStateOf(initialEditingChannel?.id ?: "") }
    var name by remember(initialEditingChannel) { mutableStateOf(initialEditingChannel?.name ?: "") }
    var imageUrl by remember(initialEditingChannel) { mutableStateOf(initialEditingChannel?.imageUrl ?: "") }
    var category by remember(initialEditingChannel) {
        mutableStateOf(initialEditingChannel?.category ?: categories.firstOrNull()?.name ?: "عام")
    }
    var branchId by remember(initialEditingChannel) { mutableStateOf(initialEditingChannel?.branchId ?: "") }

    val serverInputs = remember(initialEditingChannel) {
        val list = mutableStateListOf<ChannelServer>()
        if (initialEditingChannel != null && initialEditingChannel.servers.isNotEmpty()) {
            list.addAll(initialEditingChannel.servers)
        } else {
            list.add(ChannelServer(id = "s1", name = "سيرفر 1 (HD)", streamUrl = "", streamType = "m3u8"))
        }
        list
    }

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedBranch by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PitchGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (editingId.isBlank()) "إضافة قناة بث مباشر جديدة" else "تعديل القناة: $name",
                        color = ElectricGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القناة (مثال: beIN Sports 1 HD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PitchGreen,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("رابط شعار القناة (Logo URL)") },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("القسم") },
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            category = cat.name
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedBranch,
                            onExpandedChange = { expandedBranch = !expandedBranch },
                            modifier = Modifier.weight(1f)
                        ) {
                            val selectedBranchName = branches.find { it.id == branchId }?.name ?: "بدون فرع"
                            OutlinedTextField(
                                value = selectedBranchName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الفرع (اختياري)") },
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBranch) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricGold,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedBranch,
                                onDismissRequest = { expandedBranch = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون فرع") },
                                    onClick = {
                                        branchId = ""
                                        expandedBranch = false
                                    }
                                )
                                branches.forEach { br ->
                                    DropdownMenuItem(
                                        text = { Text(br.name) },
                                        onClick = {
                                            branchId = br.id
                                            expandedBranch = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سيرفرات البث (${serverInputs.size})",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        OutlinedButton(
                            onClick = {
                                serverInputs.add(
                                    ChannelServer(
                                        id = UUID.randomUUID().toString().take(6),
                                        name = "سيرفر ${serverInputs.size + 1}",
                                        streamUrl = "",
                                        streamType = "m3u8"
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة سيرفر", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    serverInputs.forEachIndexed { idx, srv ->
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = srv.name,
                                        onValueChange = { newName ->
                                            serverInputs[idx] = srv.copy(name = newName)
                                        },
                                        label = { Text("اسم السيرفر") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (serverInputs.size > 1) {
                                        IconButton(onClick = { serverInputs.removeAt(idx) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف السيرفر", tint = LiveRed)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = srv.streamUrl,
                                    onValueChange = { newUrl ->
                                        serverInputs[idx] = srv.copy(streamUrl = newUrl)
                                    },
                                    label = { Text("رابط البث المباشر (M3U8 / MP4 / TS)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("server_url_input_$idx")
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
                                    imageUrl = ""
                                    branchId = ""
                                    serverInputs.clear()
                                    serverInputs.add(ChannelServer(id = "s1", name = "سيرفر 1 (HD)", streamUrl = ""))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء التعديل", color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                val validServers = serverInputs.filter { it.streamUrl.isNotBlank() }
                                if (name.isNotBlank() && validServers.isNotEmpty()) {
                                    val ch = Channel(
                                        id = editingId,
                                        name = name.trim(),
                                        imageUrl = imageUrl.trim(),
                                        category = category.trim().ifBlank { "عام" },
                                        branchId = branchId,
                                        servers = validServers
                                    )
                                    onSaveChannel(ch)
                                    editingId = ""
                                    name = ""
                                    imageUrl = ""
                                    branchId = ""
                                    serverInputs.clear()
                                    serverInputs.add(ChannelServer(id = "s1", name = "سيرفر 1 (HD)", streamUrl = ""))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("save_channel_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (editingId.isBlank()) "نشر وحفظ القناة" else "حفظ تعديلات القناة",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "القنوات المنشورة (${channels.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (channels.isNotEmpty()) {
                    OutlinedButton(onClick = onClearAllChannels) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = LiveRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح الكل", color = LiveRed, fontSize = 12.sp)
                    }
                }
            }
        }

        items(channels, key = { it.id }) { ch ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ch.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "القسم: ${ch.category} | السيرفرات: ${ch.servers.size}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Row {
                        IconButton(
                            onClick = {
                                editingId = ch.id
                                name = ch.name
                                imageUrl = ch.imageUrl
                                category = ch.category
                                branchId = ch.branchId
                                serverInputs.clear()
                                serverInputs.addAll(ch.servers)
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل القناة", tint = ElectricGold)
                        }
                        IconButton(onClick = { onDeleteChannel(ch.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف القناة", tint = LiveRed)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: MATCHES & EVENTS ADMIN TAB
// =========================================================================
@Composable
private fun MatchesEventsAdminTab(
    matches: List<MatchItem>,
    channels: List<Channel>,
    onSaveLiveMatch: (
        eventId: String,
        name: String,
        tournament: String,
        broadcaster: String,
        imageUrl: String,
        startTimeMs: Long,
        targetChannelId: String?,
        servers: List<ChannelServer>
    ) -> Unit,
    onDeleteLiveMatch: (String) -> Unit
) {
    var editingId by remember { mutableStateOf("") }
    var matchName by remember { mutableStateOf("") }
    var tournament by remember { mutableStateOf("دوري أبطال أوروبا") }
    var broadcaster by remember { mutableStateOf("beIN Sports 1 HD") }
    var imageUrl by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var selectedChannelId by remember { mutableStateOf(channels.firstOrNull()?.id ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (editingId.isBlank()) "إضافة مباراة / حدث مباشر جديد" else "تعديل المباراة المحددة",
                        color = ElectricGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = matchName,
                        onValueChange = { matchName = it },
                        label = { Text("الفريقان (مثال: ريال مدريد ضد برشلونة)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tournament,
                            onValueChange = { tournament = it },
                            label = { Text("البطولة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = broadcaster,
                            onValueChange = { broadcaster = it },
                            label = { Text("القناة الناقلة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("رابط بث مباشر للمباراة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("رابط شعار البطولة / المباراة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (matchName.isNotBlank()) {
                                val servers = if (streamUrl.isNotBlank()) {
                                    listOf(ChannelServer(id = "m_srv_1", name = "سيرفر المباراة HD", streamUrl = streamUrl.trim()))
                                } else emptyList()
                                onSaveLiveMatch(
                                    editingId,
                                    matchName.trim(),
                                    tournament.trim(),
                                    broadcaster.trim(),
                                    imageUrl.trim(),
                                    System.currentTimeMillis(),
                                    selectedChannelId.ifBlank { null },
                                    servers
                                )
                                editingId = ""
                                matchName = ""
                                streamUrl = ""
                                imageUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_match_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (editingId.isBlank()) "نشر المباراة في الجدول" else "تحديث المباراة",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "المباريات المضافة من لوحة المطور (${matches.size})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        items(matches, key = { it.id }) { m ->
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val title = if (m.displayAwayName.isNotBlank()) {
                            "${m.displayHomeName} ضد ${m.displayAwayName}"
                        } else m.displayHomeName
                        Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${m.leagueName} | ${m.broadcaster}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Row {
                        IconButton(
                            onClick = {
                                editingId = m.id
                                matchName = if (m.displayAwayName.isNotBlank()) {
                                    "${m.displayHomeName} ضد ${m.displayAwayName}"
                                } else m.displayHomeName
                                tournament = m.leagueName
                                broadcaster = m.broadcaster
                                streamUrl = m.servers.firstOrNull()?.streamUrl ?: ""
                                imageUrl = m.homeTeamLogo
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = ElectricGold)
                        }
                        IconButton(onClick = { onDeleteLiveMatch(m.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = LiveRed)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: BANNERS ADMIN TAB
// =========================================================================
@Composable
private fun BannersAdminTab(
    banners: List<EventBanner>,
    channels: List<Channel>,
    onSaveBanner: (EventBanner) -> Unit,
    onDeleteBanner: (String) -> Unit
) {
    var editingId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var watchNowUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (editingId.isBlank()) "إضافة بانر إعلاني / حدث رئيسي" else "تعديل البانر",
                        color = AccentCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان البانر (مثال: كلاسيكو الأرض الليلة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("رابط صورة البانر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = watchNowUrl,
                        onValueChange = { watchNowUrl = it },
                        label = { Text("رابط البث أو اسم القناة عند الضغط على البانر (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val firstCh = channels.firstOrNull()
                                onSaveBanner(
                                    EventBanner(
                                        id = editingId.ifBlank { UUID.randomUUID().toString() },
                                        title = title.trim(),
                                        imageUrl = imageUrl.trim(),
                                        showWatchNow = true,
                                        watchNowUrl = watchNowUrl.trim(),
                                        channelId = firstCh?.id ?: "",
                                        channelName = firstCh?.name ?: ""
                                    )
                                )
                                editingId = ""
                                title = ""
                                imageUrl = ""
                                watchNowUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (editingId.isBlank()) "نشر البانر" else "حفظ التعديل", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        items(banners, key = { it.id }) { b ->
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = b.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (b.channelName.isNotBlank()) {
                            Text(text = "القناة: ${b.channelName}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Row {
                        IconButton(
                            onClick = {
                                editingId = b.id
                                title = b.title
                                imageUrl = b.imageUrl
                                watchNowUrl = b.watchNowUrl
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = ElectricGold)
                        }
                        IconButton(onClick = { onDeleteBanner(b.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = LiveRed)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 5: SEPARATE IPTV vs LIBRARY XTREME ACCOUNTS PUBLISHER & SWITCHER
// =========================================================================
@Composable
private fun XtremeAccountsAdminTab(
    accounts: List<XtremeAccount>,
    onSaveAccount: (XtremeAccount) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var editingId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var streamFormat by remember { mutableStateOf("m3u8") }
    var targetSection by remember { mutableStateOf(XtremeAccount.SECTION_IPTV) }
    var isActive by remember { mutableStateOf(true) }
    var filterTab by remember { mutableStateOf("all") }

    val filteredAccounts = remember(accounts, filterTab) {
        when (filterTab) {
            XtremeAccount.SECTION_IPTV -> accounts.filter { it.isForIptv() }
            XtremeAccount.SECTION_LIBRARY -> accounts.filter { it.isForLibrary() }
            else -> accounts
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, PitchGreen.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = PitchGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (editingId.isBlank()) "نشر حساب Xtreme / IPTV جديد" else "تعديل بيانات الحساب المحدد",
                                color = PitchGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }

                        if (editingId.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ElectricGold.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, ElectricGold)
                            ) {
                                Text(
                                    text = "وضع التعديل",
                                    color = ElectricGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك نشر الحساب بشكل منفصل لقسم (بث IPTV المباشر) أو لقسم (المكتبة - أفلام ومسلسلات) أو كليهما معاً، والتبديل والتعديل بين الحسابات في أي وقت.",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "1. اختر القسم المستهدف لنشر الحساب:",
                        color = ElectricGold,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sectionOptions = listOf(
                            Triple(XtremeAccount.SECTION_IPTV, "قسم IPTV فقط", Icons.Default.LiveTv),
                            Triple(XtremeAccount.SECTION_LIBRARY, "قسم المكتبة فقط", Icons.Default.Movie),
                            Triple(XtremeAccount.SECTION_BOTH, "IPTV + المكتبة", Icons.Default.SwapHoriz)
                        )

                        sectionOptions.forEach { (secKey, secTitle, secIcon) ->
                            val selected = targetSection == secKey
                            val accentColor = when (secKey) {
                                XtremeAccount.SECTION_IPTV -> AccentCyan
                                XtremeAccount.SECTION_LIBRARY -> NeonPurple
                                else -> PitchGreen
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) accentColor.copy(alpha = 0.18f) else CardSurface,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) accentColor else TextMuted.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { targetSection = secKey }
                                    .testTag("xtreme_target_section_$secKey")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = secIcon,
                                        contentDescription = null,
                                        tint = if (selected) accentColor else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = secTitle,
                                        color = if (selected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم السيرفر التعريفي (مثال: سيرفر VIP الرئيسي)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("xtreme_account_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("عنوان السيرفر (Host URL مثال: http://server.com:8080)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("xtreme_account_host_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("اسم المستخدم (Username)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("xtreme_account_user_input")
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور (Password)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("xtreme_account_pass_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "صيغة البث:",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            listOf("m3u8", "ts").forEach { fmt ->
                                FilterChip(
                                    selected = streamFormat.equals(fmt, ignoreCase = true),
                                    onClick = { streamFormat = fmt },
                                    label = { Text(fmt.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isActive) "مفعّل" else "موقوف",
                                color = if (isActive) PitchGreen else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PitchGreen)
                            )
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
                                    host = ""
                                    username = ""
                                    password = ""
                                    streamFormat = "m3u8"
                                    targetSection = XtremeAccount.SECTION_IPTV
                                    isActive = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("إلغاء التعديل", color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                if (host.isNotBlank() && username.isNotBlank()) {
                                    val defaultName = when (targetSection) {
                                        XtremeAccount.SECTION_IPTV -> "سيرفر IPTV مباشر"
                                        XtremeAccount.SECTION_LIBRARY -> "سيرفر مكتبة الأفلام"
                                        else -> "سيرفر شامل"
                                    }
                                    onSaveAccount(
                                        XtremeAccount(
                                            id = editingId.ifBlank { UUID.randomUUID().toString() },
                                            name = name.trim().ifBlank { defaultName },
                                            host = host.trim(),
                                            username = username.trim(),
                                            password = password.trim(),
                                            streamFormat = streamFormat,
                                            targetSection = targetSection,
                                            isActive = isActive
                                        )
                                    )
                                    editingId = ""
                                    name = ""
                                    host = ""
                                    username = ""
                                    password = ""
                                    streamFormat = "m3u8"
                                    isActive = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1.6f)
                                .height(48.dp)
                                .testTag("save_xtreme_account_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val targetLabel = when (targetSection) {
                                XtremeAccount.SECTION_IPTV -> "لقسم IPTV"
                                XtremeAccount.SECTION_LIBRARY -> "لقسم المكتبة"
                                else -> "لـ IPTV والمكتبة"
                            }
                            Text(
                                text = if (editingId.isBlank()) "نشر الحساب ($targetLabel)" else "حفظ تعديلات الحساب",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val iptvCount = accounts.count { it.isForIptv() }
                val libCount = accounts.count { it.isForLibrary() }

                Text(
                    text = "الحسابات المنشورة حالياً (${accounts.size}) — يمكنك التعديل أو تحويل القسم بضغطة واحدة:",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        Triple("all", "الكل (${accounts.size})", ElectricGold),
                        Triple(XtremeAccount.SECTION_IPTV, "حسابات IPTV ($iptvCount)", AccentCyan),
                        Triple(XtremeAccount.SECTION_LIBRARY, "حسابات المكتبة ($libCount)", NeonPurple)
                    )
                    filters.forEach { (key, label, color) ->
                        val isSelected = filterTab == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color.copy(alpha = 0.2f) else CardSurface,
                            border = BorderStroke(1.dp, if (isSelected) color else TextMuted.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { filterTab = key }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) color else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        items(filteredAccounts, key = { it.id }) { acc ->
            val sectionBadgeColor = when (acc.targetSection.lowercase()) {
                XtremeAccount.SECTION_IPTV -> AccentCyan
                XtremeAccount.SECTION_LIBRARY -> NeonPurple
                else -> PitchGreen
            }
            val sectionBadgeText = when (acc.targetSection.lowercase()) {
                XtremeAccount.SECTION_IPTV -> "مخصص لقسم IPTV"
                XtremeAccount.SECTION_LIBRARY -> "مخصص لقسم المكتبة"
                else -> "مشترك: IPTV + المكتبة"
            }

            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (editingId == acc.id) 1.5.dp else 1.dp,
                    color = if (editingId == acc.id) ElectricGold else sectionBadgeColor.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = acc.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = sectionBadgeColor.copy(alpha = 0.16f),
                                    border = BorderStroke(0.8.dp, sectionBadgeColor)
                                ) {
                                    Text(
                                        text = sectionBadgeText,
                                        color = sectionBadgeColor,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${acc.host} • User: ${acc.username} • ${acc.streamFormat.uppercase()}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    editingId = acc.id
                                    name = acc.name
                                    host = acc.host
                                    username = acc.username
                                    password = acc.password
                                    streamFormat = acc.streamFormat
                                    targetSection = acc.targetSection.ifBlank { XtremeAccount.SECTION_BOTH }
                                    isActive = acc.isActive
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب", tint = ElectricGold)
                            }
                            IconButton(onClick = { onDeleteAccount(acc.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف الحساب", tint = LiveRed)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E0E18))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحويل سريع للقسم:",
                            color = TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                XtremeAccount.SECTION_IPTV to "IPTV فقط",
                                XtremeAccount.SECTION_LIBRARY to "المكتبة فقط",
                                XtremeAccount.SECTION_BOTH to "الاثنين معاً"
                            ).forEach { (secKey, label) ->
                                val isCurrent = acc.targetSection.equals(secKey, ignoreCase = true) ||
                                    (acc.targetSection.isBlank() && secKey == XtremeAccount.SECTION_BOTH)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) PitchGreen.copy(alpha = 0.22f) else Color(0xFF181826),
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (isCurrent) PitchGreen else Color(0xFF2A2A3C)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!isCurrent) {
                                                onSaveAccount(acc.copy(targetSection = secKey))
                                            }
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isCurrent) PitchGreen else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

// =========================================================================
// TAB 6: ONLINE BOTTOM NAVIGATION BAR CONTROL TAB
// =========================================================================
@Composable
private fun BottomBarAdminTab(
    showChannelsTab: Boolean,
    showMatchesTab: Boolean,
    showIptvTab: Boolean,
    showFoulTab: Boolean,
    showLibraryTab: Boolean,
    onSaveBottomBarVisibility: (
        showChannelsTab: Boolean,
        showMatchesTab: Boolean,
        showIptvTab: Boolean,
        showFoulTab: Boolean,
        showLibraryTab: Boolean
    ) -> Unit
) {
    var channelsVisible by remember(showChannelsTab) { mutableStateOf(showChannelsTab) }
    var matchesVisible by remember(showMatchesTab) { mutableStateOf(showMatchesTab) }
    var iptvVisible by remember(showIptvTab) { mutableStateOf(showIptvTab) }
    var foulVisible by remember(showFoulTab) { mutableStateOf(showFoulTab) }
    var libraryVisible by remember(showLibraryTab) { mutableStateOf(showLibraryTab) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewHeadline,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "التحكم في الشريط السفلي (Bottom Bar Online)",
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = "إخفاء أو إظهار أي قسم في الشريط السفلي فوراً لجميع المستخدمين أونلاين",
                                color = AccentCyan,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "معاينة حية للشريط السفلي كما يظهر للمستخدمين الآن:",
                        color = ElectricGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val activePreviewItems = listOfNotNull(
                        if (channelsVisible) "الرئيسية" to Icons.Default.Tv else null,
                        if (iptvVisible) "IPTV" to Icons.Default.LiveTv else null,
                        if (foulVisible) "FOUL TV" to Icons.Default.PlayCircle else null,
                        if (libraryVisible) "المكتبة" to Icons.Default.Movie else null,
                        if (matchesVisible) "المباريات" to Icons.Default.SportsSoccer else null
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0A0A12),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (activePreviewItems.isEmpty()) {
                            Text(
                                text = "تنبيه: يجب إبقاء قسم واحد على الأقل ظاهراً في الشريط السفلي",
                                color = LiveRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                activePreviewItems.forEach { (title, icon) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = BrightLavender,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = title,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BottomBarToggleItem(
                        title = "قسم الرئيسية (القنوات المباشرة)",
                        subtitle = "إظهار أو إخفاء تبويب الرئيسية والقنوات من الشريط السفلي",
                        icon = Icons.Default.Tv,
                        checked = channelsVisible,
                        onCheckedChange = { newValue ->
                            channelsVisible = newValue
                            statusMessage = null
                        },
                        testTag = "toggle_bottom_tab_channels"
                    )

                    BottomBarToggleItem(
                        title = "قسم بث IPTV المباشر",
                        subtitle = "إظهار أو إخفاء تبويب قنوات اشتراكات IPTV",
                        icon = Icons.Default.LiveTv,
                        checked = iptvVisible,
                        onCheckedChange = { newValue ->
                            iptvVisible = newValue
                            statusMessage = null
                        },
                        testTag = "toggle_bottom_tab_iptv"
                    )

                    BottomBarToggleItem(
                        title = "قسم FOUL TV (مقاطع الفيديو والملخصات)",
                        subtitle = "إظهار أو إخفاء تبويب فيديوهات فاول التفاعلي",
                        icon = Icons.Default.PlayCircle,
                        checked = foulVisible,
                        onCheckedChange = { newValue ->
                            foulVisible = newValue
                            statusMessage = null
                        },
                        testTag = "toggle_bottom_tab_foul"
                    )

                    BottomBarToggleItem(
                        title = "قسم المكتبة (الأفلام والمسلسلات)",
                        subtitle = "إظهار أو إخفاء تبويب مكتبة الفيديو حسب الطلب VOD",
                        icon = Icons.Default.Movie,
                        checked = libraryVisible,
                        onCheckedChange = { newValue ->
                            libraryVisible = newValue
                            statusMessage = null
                        },
                        testTag = "toggle_bottom_tab_library"
                    )

                    BottomBarToggleItem(
                        title = "قسم جدول المباريات",
                        subtitle = "إظهار أو إخفاء تبويب المباريات المباشرة والنتائج",
                        icon = Icons.Default.SportsSoccer,
                        checked = matchesVisible,
                        onCheckedChange = { newValue ->
                            matchesVisible = newValue
                            statusMessage = null
                        },
                        testTag = "toggle_bottom_tab_matches"
                    )

                    if (!statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage!!,
                            color = PitchGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                channelsVisible = true
                                matchesVisible = true
                                iptvVisible = true
                                foulVisible = true
                                libraryVisible = true
                                onSaveBottomBarVisibility(true, true, true, true, true)
                                statusMessage = "تم إظهار جميع الأقسام ومزامنتها أونلاين بنجاح!"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("إظهار الكل", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val anyVisible = channelsVisible || matchesVisible || iptvVisible || foulVisible || libraryVisible
                                if (!anyVisible) {
                                    channelsVisible = true
                                }
                                onSaveBottomBarVisibility(
                                    channelsVisible,
                                    matchesVisible,
                                    iptvVisible,
                                    foulVisible,
                                    libraryVisible
                                )
                                statusMessage = "تم حفظ ومزامنة الشريط السفلي أونلاين لجميع المستخدمين فوراً!"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1.7f)
                                .height(50.dp)
                                .testTag("save_bottom_bar_visibility_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حفظ ومزامنة الشريط السفلي أونلاين",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (checked) PitchGreen.copy(alpha = 0.4f) else LiveRed.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (checked) PitchGreen.copy(alpha = 0.16f) else LiveRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) PitchGreen else LiveRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (checked) PitchGreen.copy(alpha = 0.18f) else LiveRed.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (checked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (checked) PitchGreen else LiveRed,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (checked) "ظاهر" else "مخفي",
                                    color = if (checked) PitchGreen else LiveRed,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PitchGreen,
                    checkedTrackColor = PitchGreen.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// =========================================================================
// TAB 8: PROFESSIONAL APP VERSION & UPDATE MANAGEMENT TAB
// =========================================================================
@Composable
private fun AppConfigAdminTab(
    installedAppVersion: String,
    latestAppVersion: String,
    forceUpdateRequired: Boolean,
    optionalUpdateAvailable: Boolean,
    updateMessage: String,
    updateDownloadUrl: String,
    telegramSupportUrl: String,
    onSaveAppConfig: (
        latestVersion: String,
        forceUpdate: Boolean,
        optionalUpdate: Boolean,
        updateMessage: String,
        updateUrl: String,
        telegramSupportUrl: String,
        newDeveloperPass: String?
    ) -> Unit
) {
    var version by remember(latestAppVersion) { mutableStateOf(latestAppVersion.ifBlank { installedAppVersion }) }
    var updateMode by remember(forceUpdateRequired, optionalUpdateAvailable) {
        mutableStateOf(
            when {
                forceUpdateRequired -> "forced"
                optionalUpdateAvailable -> "optional"
                else -> "none"
            }
        )
    }
    var msg by remember(updateMessage) { mutableStateOf(updateMessage) }
    var url by remember(updateDownloadUrl) { mutableStateOf(updateDownloadUrl) }
    var tgUrl by remember(telegramSupportUrl) { mutableStateOf(telegramSupportUrl) }
    var newDevPass by remember { mutableStateOf("") }
    var savedBannerVisible by remember { mutableStateOf(false) }

    fun bumpVersion(majorDelta: Int, minorDelta: Int, patchDelta: Int) {
        val parts = version.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
        val maj = (parts.getOrElse(0) { 4 } + majorDelta).coerceAtLeast(1)
        val min = if (majorDelta > 0) 0 else (parts.getOrElse(1) { 0 } + minorDelta).coerceAtLeast(0)
        val pat = if (majorDelta > 0 || minorDelta > 0) 0 else (parts.getOrElse(2) { 0 } + patchDelta).coerceAtLeast(0)
        version = "$maj.$min.$pat"
        savedBannerVisible = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, ElectricGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = ElectricGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إدارة إصدار التطبيق ونظام التحديثات الاحترافي",
                                color = ElectricGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = "تحكم كامل في رقم الإصدار ونوع التحديث (إجباري أو اختياري أو بدون تحديث) أونلاين",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0E0E18),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إصدار التطبيق المثبت حالياً", color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "v$installedAppVersion",
                                    color = AccentCyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(TextMuted.copy(alpha = 0.25f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الإصدار المنشور في السيرفر", color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "v$version",
                                    color = PitchGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. تحديد رقم الإصدار الجديد (أو رفعه بضغطة زر):",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = version,
                        onValueChange = {
                            version = it
                            savedBannerVisible = false
                        },
                        label = { Text("رقم أحدث إصدار (مثال: 4.1.0)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_config_version_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { bumpVersion(0, 0, 1) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+0.0.1 فرعي", fontSize = 11.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { bumpVersion(0, 1, 0) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+0.1.0 متوسط", fontSize = 11.sp, color = PitchGreen, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { bumpVersion(1, 0, 0) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1.0.0 رئيسي", fontSize = 11.sp, color = ElectricGold, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                version = installedAppVersion
                                savedBannerVisible = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("مطابقة الحالي", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "2. اختر نوع التحديث للمستخدمين:",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val modes = listOf(
                        Triple(
                            "none",
                            "إيقاف إشعار التحديث (الوضع العادي)",
                            "يعمل التطبيق بشكل طبيعي لجميع المستخدمين بدون ظهور شاشة تحديث"
                        ),
                        Triple(
                            "optional",
                            "تحديث اختياري (Optional Update)",
                            "تظهر للمستخدم شاشة التحديث مع زر (تخطي والمتابعة للتطبيق) إن أراد التأجيل"
                        ),
                        Triple(
                            "forced",
                            "تحديث إجباري (Force Update)",
                            "يلزم جميع المستخدمين بتحميل التحديث الجديد ولا يسمح بتخطي الشاشة"
                        )
                    )

                    modes.forEach { (modeKey, modeTitle, modeDesc) ->
                        val isSelected = updateMode == modeKey
                        val modeColor = when (modeKey) {
                            "forced" -> ElectricGold
                            "optional" -> AccentCyan
                            else -> PitchGreen
                        }
                        val modeIcon = when (modeKey) {
                            "forced" -> Icons.Default.Security
                            "optional" -> Icons.Default.NewReleases
                            else -> Icons.Default.CheckCircle
                        }

                        Surface(
                            color = if (isSelected) modeColor.copy(alpha = 0.15f) else CardSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) modeColor else TextMuted.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    updateMode = modeKey
                                    savedBannerVisible = false
                                }
                                .testTag("update_mode_$modeKey")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) modeColor else TextMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = modeTitle,
                                        color = if (isSelected) modeColor else TextPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = modeDesc,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. رسالة التحديث وروابط التحميل:",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = msg,
                        onValueChange = {
                            msg = it
                            savedBannerVisible = false
                        },
                        label = { Text("رسالة التحديث للمستخدمين (ما الجديد في هذا الإصدار؟)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            savedBannerVisible = false
                        },
                        label = { Text("رابط تحميل التحديث المباشر (APK أو متجر أو تلجرام)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_config_update_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tgUrl,
                        onValueChange = {
                            tgUrl = it
                            savedBannerVisible = false
                        },
                        label = { Text("رابط قناة التلجرام الرسمية للدعم والتحديثات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newDevPass,
                        onValueChange = {
                            newDevPass = it
                            savedBannerVisible = false
                        },
                        label = { Text("تغيير الرمز السري للمطور (اتركه فارغاً للإبقاء على الحالي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (savedBannerVisible) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SuccessGreen.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, SuccessGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "تم حفظ ونشر إعدادات الإصدار والتحديث أونلاين بنجاح!",
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val isForce = updateMode == "forced"
                            val isOptional = updateMode == "optional"
                            onSaveAppConfig(
                                version.trim().ifBlank { installedAppVersion },
                                isForce,
                                isOptional,
                                msg.trim(),
                                url.trim(),
                                tgUrl.trim(),
                                newDevPass.trim().ifBlank { null }
                            )
                            savedBannerVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchGreen,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_app_config_button")
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفظ ونشر إعدادات الإصدار والتحديث أونلاين",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
