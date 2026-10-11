package com.example.ui.iptv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.XtremeAccount
import com.example.data.model.XtremeContentType
import com.example.data.model.XtremeItem
import com.example.data.util.ChannelClusteringEngine
import com.example.ui.components.CategoryChip
import com.example.ui.components.ChannelLogoPlaceholder
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.DarkPurpleContainer
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * Dedicated Live IPTV Channels Screen (قسم بث IPTV المباشر):
 * - Displays all published IPTV accounts with instant Account Switcher
 * - Supports unlimited channels with fast category filtering & search
 * - Automatically clusters matching channel qualities (4K, FHD, HD, SD, Backup) into a Top Merged Quality Servers Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvScreen(
    items: List<XtremeItem>,
    categories: List<String>,
    accounts: List<XtremeAccount>,
    selectedAccount: XtremeAccount?,
    isLoading: Boolean,
    errorMessage: String?,
    gridColumns: Int = 3,
    onSelectAccount: (XtremeAccount) -> Unit,
    onRefresh: () -> Unit,
    onPlayChannel: (XtremeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }
    var selectedClusteredChannel by remember { mutableStateOf<XtremeItem?>(null) }

    // Filter only LIVE channels for the IPTV screen
    val liveChannels = remember(items) {
        items.filter { it.type == XtremeContentType.LIVE }
    }

    // Build dynamic categories list from live channels
    val availableCategories = remember(liveChannels, categories) {
        val set = linkedSetOf("الكل")
        liveChannels.forEach { ch ->
            if (ch.categoryName.isNotBlank()) {
                set.add(ch.categoryName.trim())
            }
        }
        set.toList()
    }

    // Reset category if it no longer exists when switching accounts
    LaunchedEffect(availableCategories) {
        if (selectedCategory !in availableCategories) {
            selectedCategory = "الكل"
        }
    }

    // Filtered live channels by category & search query (unlimited count)
    val filteredChannels = remember(liveChannels, selectedCategory, searchQuery) {
        val byCat = if (selectedCategory == "الكل") {
            liveChannels
        } else {
            liveChannels.filter { it.categoryName.trim().equals(selectedCategory, ignoreCase = true) }
        }

        if (searchQuery.isBlank()) {
            byCat
        } else {
            val q = searchQuery.trim()
            byCat.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.categoryName.contains(q, ignoreCase = true)
            }
        }
    }

    // Compute quality variants for the currently searched or selected channel
    val activeQualityVariants = remember(liveChannels, filteredChannels, searchQuery, selectedClusteredChannel) {
        val targetItem = when {
            selectedClusteredChannel != null -> selectedClusteredChannel
            searchQuery.isNotBlank() && filteredChannels.isNotEmpty() -> filteredChannels.first()
            else -> null
        }
        if (targetItem == null) {
            emptyList()
        } else {
            val targetBaseKey = ChannelClusteringEngine.normalizeComparisonKey(
                ChannelClusteringEngine.extractCleanBaseName(targetItem.title)
            )
            if (targetBaseKey.isBlank()) {
                listOf(targetItem)
            } else {
                val matches = liveChannels.filter { candidate ->
                    val candKey = ChannelClusteringEngine.normalizeComparisonKey(
                        ChannelClusteringEngine.extractCleanBaseName(candidate.title)
                    )
                    candKey == targetBaseKey
                }
                matches.ifEmpty { listOf(targetItem) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // 1. COMPACT TOP HEADER BAR: Title + Account Switcher Pill + Search + Refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LiveRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Text(
                            text = "بث مباشر IPTV",
                            color = LiveRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (liveChannels.isNotEmpty()) {
                    Text(
                        text = "(${filteredChannels.size} قناة)",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Action Buttons: Account Switcher & Search & Refresh
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (accounts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F0F16),
                        border = BorderStroke(1.dp, if (accounts.size > 1) NeonPurple.copy(alpha = 0.8f) else Color(0xFF222230)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAccountSwitcherSheet = true }
                            .testTag("iptv_account_switcher_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (accounts.size > 1) Icons.Default.SwapHoriz else Icons.Default.Dns,
                                contentDescription = "تبديل حساب IPTV",
                                tint = BrightLavender,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = selectedAccount?.name?.ifBlank { "حساب IPTV" } ?: "اختر سيرفر",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (accounts.size > 1) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonPurple,
                                    modifier = Modifier.size(15.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${accounts.size}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Toggle Button
                Surface(
                    shape = CircleShape,
                    color = if (isSearchExpanded) ElectricPurple else Color(0xFF161624),
                    border = BorderStroke(0.8.dp, Color(0xFF2A2A3E)),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) {
                                searchQuery = ""
                                selectedClusteredChannel = null
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Refresh Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF161624),
                    border = BorderStroke(0.8.dp, Color(0xFF2A2A3E)),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onRefresh() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. EXPANDABLE SEARCH BAR
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن أي قناة لعرضها مع جميع جوداتها المدمجة في الأعلى...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0C0C14),
                        unfocusedContainerColor = Color(0xFF08080E),
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0xFF1E1E2C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("iptv_search_input")
                )
            }
        }

        // 3. TOP MERGED QUALITY SERVERS BAR (تظهر القناة وجميع الجودات الخاصة بها بشكل سيرفرات مدمجة في الأعلى بشكل مرتب)
        AnimatedVisibility(
            visible = activeQualityVariants.isNotEmpty() && (searchQuery.isNotBlank() || selectedClusteredChannel != null),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val baseChannelTitle = remember(activeQualityVariants) {
                val first = activeQualityVariants.firstOrNull()?.title.orEmpty()
                ChannelClusteringEngine.extractCleanBaseName(first).ifBlank { first }
            }
            var activeQualityIdx by remember(activeQualityVariants) { mutableIntStateOf(0) }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10101C),
                border = BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(listOf(AccentCyan, NeonPurple, PitchGreen))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("iptv_top_merged_qualities_bar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PitchGreen.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, PitchGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = PitchGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${activeQualityVariants.size} جودات مدمجة",
                                        color = PitchGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = baseChannelTitle,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selectedClusteredChannel != null) {
                            IconButton(
                                onClick = { selectedClusteredChannel = null },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق شريط الجودات",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(activeQualityVariants) { idx, variant ->
                            val isSelected = activeQualityIdx == idx
                            val qualityBadge = remember(variant.title, idx) {
                                ChannelClusteringEngine.extractQualityBadge(variant.title, variant.streamUrl)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ElectricPurple else Color(0xFF1A1A2B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentCyan else Color(0xFF2D2D44)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        activeQualityIdx = idx
                                        onPlayChannel(variant)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isSelected) ElectricGold else AccentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = qualityBadge,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CATEGORIES RIBBON (Fast category filtering)
        if (availableCategories.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("iptv_categories_ribbon"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                items(availableCategories, key = { it }) { cat ->
                    CategoryChip(
                        text = cat,
                        isSelected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }
        }

        // 5. MAIN CONTENT AREA: Channels Grid, Loading or Empty State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading && liveChannels.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonPurple,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(
                                text = "جاري تحميل جميع قنوات البث المباشر بسرعة فائقة...",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedAccount?.name?.ifBlank { "سيرفر IPTV" } ?: "الاتصال بالخادم",
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                accounts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(DarkPurpleContainer, CircleShape)
                                        .border(1.dp, NeonPurple, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = BrightLavender,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Text(
                                    text = "لا يوجد حساب مخصص لقسم IPTV حالياً",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "يمكنك نشر حساب Xtreme مخصص لقسم IPTV من لوحة تحكم المطور ليظهر هنا فوراً.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                filteredChannels.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = errorMessage ?: "لا توجد قنوات مطابقة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkPurpleContainer),
                                border = BorderStroke(1.dp, NeonPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("إعادة المحاولة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("iptv_live_channels_grid"),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 86.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredChannels, key = { it.id }) { channel ->
                            IptvChannelGridCard(
                                channel = channel,
                                onClick = {
                                    selectedClusteredChannel = channel
                                    onPlayChannel(channel)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. ACCOUNT SWITCHER MODAL BOTTOM SHEET
    if (showAccountSwitcherSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAccountSwitcherSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF08080C),
            scrimColor = Color.Black.copy(alpha = 0.75f),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "تبديل حساب IPTV (${accounts.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showAccountSwitcherSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }

                Text(
                    text = "اختر السيرفر المطلوب لعرض قنوات البث المباشر الخاصة به فوراً:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val isCurrentActive = acc.id == selectedAccount?.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentActive) Color(0xFF1E1734) else DarkSurfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (isCurrentActive) 1.5.dp else 1.dp,
                                color = if (isCurrentActive) NeonPurple else DarkSurfaceBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectAccount(acc)
                                    showAccountSwitcherSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrentActive) NeonPurple.copy(alpha = 0.25f) else Color(0xFF161622),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Dns,
                                                contentDescription = null,
                                                tint = if (isCurrentActive) BrightLavender else TextMuted,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = acc.name.ifBlank { "حساب IPTV" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${acc.host} • ${acc.streamExtension.uppercase()}",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isCurrentActive) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = SuccessGreen.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, SuccessGreen)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "نشط",
                                                color = SuccessGreen,
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

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun IptvChannelGridCard(
    channel: XtremeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF14141E),
                Color(0xFF090910)
            )
        )
    }

    val borderColor = if (isFocused) ElectricPurple else Color(0xFF282838)
    val borderWidth = if (isFocused) 2.dp else 1.1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.Spacebar)) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .testTag("iptv_channel_card_${channel.streamId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(if (isFocused) 1.05f else 1f)
                .clip(RoundedCornerShape(22.dp))
                .background(cardBrush)
                .border(borderWidth, borderColor, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.streamIcon.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.streamIcon)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = channel.title.take(3).trim(),
                                color = TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    error = {
                        ChannelLogoPlaceholder(channelName = channel.title)
                    }
                )
            } else {
                ChannelLogoPlaceholder(channelName = channel.title)
            }

            Surface(
                shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 20.dp),
                color = ElectricPurple.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = channel.containerExtension.ifBlank { "m3u8" }.uppercase(),
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = channel.title,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}
