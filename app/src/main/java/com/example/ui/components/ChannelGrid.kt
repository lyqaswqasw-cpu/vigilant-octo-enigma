package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.Channel
import com.example.data.model.ChannelBranch
import com.example.data.model.ChannelCategory
import com.example.data.model.ChannelServer
import com.example.data.model.EventBanner
import com.example.data.matches.MatchItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.DarkPurpleContainer
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clean 3-column Broadcast Channels Grid with:
 * - Unlimited channel streaming & search (M3U + Xtreme + Manual)
 * - Top Merged Quality Servers Bar (سيرفرات الجودات المدمجة في الأعلى بشكل مرتب)
 * - Category pills & nested branches
 */
@Composable
fun ChannelGrid(
    channels: List<Channel>,
    categories: List<ChannelCategory> = emptyList(),
    branches: List<ChannelBranch> = emptyList(),
    isLoadingCategories: Boolean = false,
    isLoadingChannels: Boolean = false,
    upcomingEvents: List<MatchItem> = emptyList(),
    eventBanners: List<EventBanner> = emptyList(),
    selectedCategory: String,
    selectedBranch: ChannelBranch? = null,
    selectedChannel: Channel?,
    searchQuery: String,
    isCompact: Boolean = true,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onBranchSelected: (ChannelBranch?) -> Unit = {},
    onChannelSelected: (Channel) -> Unit,
    onPlayChannelWithServer: (Channel, ChannelServer) -> Unit = { ch, _ -> onChannelSelected(ch) },
    onEventClick: (MatchItem) -> Unit = {},
    onBannerClick: (EventBanner) -> Unit = {},
    onOpenPublisher: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Handle system Back button when inside a sub-branch to return to the parent category
    if (selectedBranch != null) {
        BackHandler {
            onBranchSelected(null)
        }
    }

    var isSearchBarVisible by remember { mutableStateOf(false) }

    val availableCategories = remember(categories, channels) {
        val dbNames = categories.map { it.name.trim() }.filter { it.isNotBlank() }
        val channelCategories = channels
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
        (dbNames + channelCategories).distinct()
    }

    // Effective active category
    val activeCategory = remember(selectedCategory, availableCategories) {
        if (selectedCategory.isNotBlank() && availableCategories.contains(selectedCategory)) {
            selectedCategory
        } else {
            availableCategories.firstOrNull().orEmpty()
        }
    }

    // Branches belonging to the active category
    val categoryBranches = remember(branches, activeCategory, searchQuery) {
        if (searchQuery.isNotBlank() || activeCategory.isBlank()) {
            emptyList()
        } else {
            branches.filter { it.categoryName.equals(activeCategory, ignoreCase = true) }
        }
    }

    // Channels to display at the current level (Root Category vs Inside Branch)
    val displayedChannels = remember(channels, activeCategory, selectedBranch, searchQuery) {
        if (searchQuery.isNotBlank()) {
            channels.filter { channel ->
                channel.name.contains(searchQuery, ignoreCase = true) ||
                    channel.category.contains(searchQuery, ignoreCase = true) ||
                    channel.effectiveServers.any { it.name.contains(searchQuery, ignoreCase = true) }
            }
        } else if (selectedBranch != null) {
            channels.filter { channel ->
                channel.branchId == selectedBranch.id
            }
        } else if (activeCategory.isNotBlank()) {
            channels.filter { channel ->
                channel.category.equals(activeCategory, ignoreCase = true) && channel.branchId.isBlank()
            }
        } else {
            emptyList()
        }
    }

    // Highlighted channel for the Top Merged Quality Servers Bar:
    // Automatically picks the selected channel or the top search match so the user sees its merged quality servers at the top immediately
    val highlightedQualityChannel = remember(displayedChannels, selectedChannel, searchQuery) {
        when {
            selectedChannel != null && displayedChannels.any { it.id == selectedChannel.id } -> {
                displayedChannels.first { it.id == selectedChannel.id }
            }
            searchQuery.isNotBlank() && displayedChannels.isNotEmpty() -> {
                displayedChannels.first()
            }
            else -> null
        }
    }

    val columnsCount = if (isCompact) 3 else 5

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Sleek Collapsible Search Bar (slides open when search icon in Top Bar is tapped)
            AnimatedVisibility(
                visible = isSearchBarVisible || searchQuery.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF14141E),
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(NeonPurple),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("channel_search_input"),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "ابحث عن القناة لعرضها مع جميع جوداتها وسيرفراتها المدمجة...",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                isSearchBarVisible = false
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق البحث",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. TOP MERGED QUALITY SERVERS BAR (شريط سيرفرات الجودات المدمجة في الأعلى بشكل مرتب)
            AnimatedVisibility(
                visible = highlightedQualityChannel != null && highlightedQualityChannel.effectiveServers.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                highlightedQualityChannel?.let { channel ->
                    TopMergedQualityServersBar(
                        channel = channel,
                        onPlayServer = { server ->
                            onPlayChannelWithServer(channel, server)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            // 3. Main Content Area
            if (isLoadingCategories && availableCategories.isEmpty() && channels.isEmpty()) {
                ChannelsLoadingView()
            } else if (availableCategories.isEmpty() && channels.isEmpty()) {
                EmptyChannelsView(onOpenPublisher = onOpenPublisher)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 6.dp,
                        bottom = 100.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("channels_vertical_grid")
                ) {
                    // A. Event Banner / Hero Card at the top if published
                    val visibleBanners = eventBanners.filter { it.isVisible }
                    if (visibleBanners.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "hero_banners_header") {
                            EventBannersCarousel(
                                banners = visibleBanners,
                                onBannerClick = onBannerClick
                            )
                        }
                    } else if (upcomingEvents.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "live_events_header") {
                            UpcomingEventsBannerRow(
                                events = upcomingEvents,
                                onEventClick = onEventClick
                            )
                        }
                    }

                    // B. Categories Ribbon + Integrated Search Icon
                    item(span = { GridItemSpan(maxLineSpan) }, key = "categories_ribbon_header") {
                        CategoriesRibbonRow(
                            categories = availableCategories,
                            activeCategory = activeCategory,
                            channelCount = displayedChannels.size,
                            onCategorySelected = { cat ->
                                onBranchSelected(null)
                                onCategorySelected(cat)
                            },
                            onToggleSearch = {
                                isSearchBarVisible = !isSearchBarVisible
                            }
                        )
                    }

                    // C. Active Branch Navigation Bar (when inside a sub-branch)
                    if (selectedBranch != null && searchQuery.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "active_branch_bar") {
                            ActiveBranchHeaderBar(
                                branch = selectedBranch,
                                onBackToCategory = { onBranchSelected(null) }
                            )
                        }
                    }

                    // D. Branches Cards inside the Root Category (3-column grid)
                    if (selectedBranch == null && searchQuery.isBlank() && categoryBranches.isNotEmpty()) {
                        items(
                            items = categoryBranches,
                            key = { branch -> "branch_${branch.id}" }
                        ) { branch ->
                            BranchGridCard(
                                branch = branch,
                                onClick = { onBranchSelected(branch) }
                            )
                        }
                    }

                    // E. Channels Grid Items (or Loading / Empty indicator)
                    if (isLoadingChannels && displayedChannels.isEmpty() && categoryBranches.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "channels_loading_span") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = NeonPurple,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "جاري جلب جميع قنوات القسم ودمج الجودات...",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (displayedChannels.isEmpty() && categoryBranches.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "channels_empty_span") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedBranch != null) {
                                        "لا توجد قنوات مضافة داخل فرع (${selectedBranch.name}) حالياً"
                                    } else {
                                        "لا توجد قنوات في هذا القسم حالياً"
                                    },
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        items(
                            items = displayedChannels,
                            key = { channel -> "ch_${channel.id}" }
                        ) { channel ->
                            val isSelected = selectedChannel?.id == channel.id
                            ChannelGridCard(
                                channel = channel,
                                isSelected = isSelected,
                                onClick = { onChannelSelected(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top Merged Quality Servers Bar (سيرفرات الجودات المدمجة في الأعلى بشكل مرتب):
 * Displays the channel name and all of its merged quality servers (4K, FHD, HD, SD, Low, Backup)
 * neatly at the top so the user can launch any quality immediately.
 */
@Composable
fun TopMergedQualityServersBar(
    channel: Channel,
    onPlayServer: (ChannelServer) -> Unit,
    modifier: Modifier = Modifier
) {
    val servers = channel.effectiveServers
    var selectedServerIndex by remember(channel.id) { mutableIntStateOf(0) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF10101C),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(listOf(AccentCyan, NeonPurple, PitchGreen))
        ),
        modifier = modifier.testTag("top_merged_quality_servers_bar")
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
                                text = "${servers.size} جودات مدمجة",
                                color = PitchGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "اختر الجودة للتشغيل",
                    color = AccentCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(servers) { idx, srv ->
                    val isSelected = selectedServerIndex == idx
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
                                selectedServerIndex = idx
                                onPlayServer(srv)
                            }
                            .testTag("merged_quality_chip_$idx")
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
                                text = srv.name.ifBlank { "جودة ${idx + 1}" },
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

/**
 * Categories Ribbon Row:
 * - Search icon button on the left side
 * - Horizontally scrollable rounded category pills on the right
 * - Total channels badge
 */
@Composable
private fun CategoriesRibbonRow(
    categories: List<String>,
    activeCategory: String,
    channelCount: Int = 0,
    onCategorySelected: (String) -> Unit,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Categories horizontal pills
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories, key = { it }) { category ->
                val isSelected = category.equals(activeCategory, ignoreCase = true)
                CategoryPillItem(
                    title = category,
                    count = if (isSelected) channelCount else 0,
                    isSelected = isSelected,
                    onClick = { onCategorySelected(category) }
                )
            }
        }

        // Circular Dark Search Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF14141F))
                .border(1.dp, Color(0xFF252538), CircleShape)
                .clickable(onClick = onToggleSearch)
                .testTag("toggle_search_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "بحث القنوات",
                tint = Color(0xFF8C8CA1),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CategoryPillItem(
    title: String,
    count: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF7B2CBF), Color(0xFF5A189A))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF14141F), Color(0xFF14141F))
        )
    }

    val borderColor = if (isSelected) {
        Color(0xFF9D4EDD)
    } else {
        Color(0xFF262638)
    }

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundBrush)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFFB5B5C8),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
            if (isSelected && count > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "$count",
                        color = ElectricGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top Event Banners Carousel:
 * Displays rich rounded promotional/match cards at the top of the screen.
 */
@Composable
fun EventBannersCarousel(
    banners: List<EventBanner>,
    onBannerClick: (EventBanner) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(banners, key = { it.id }) { banner ->
                val cardModifier = if (banners.size == 1) {
                    Modifier
                        .fillParentMaxWidth()
                        .height(175.dp)
                } else {
                    Modifier
                        .width(330.dp)
                        .height(175.dp)
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141F)),
                    border = BorderStroke(1.dp, Color(0xFF2D2D44)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = cardModifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onBannerClick(banner) }
                        .testTag("event_banner_${banner.id}")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (banner.imageUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(banner.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF2B134E),
                                                Color(0xFF130924),
                                                Color(0xFF0A0612)
                                            )
                                        )
                                    )
                            )
                        }

                        // Subtle dark vignette at bottom only if text exists
                        if (banner.title.isNotBlank() || banner.subtitle.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Optional Badge
                        if (banner.badgeText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = LiveRed.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = banner.badgeText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Title & Subtitle
                        if (banner.title.isNotBlank() || banner.subtitle.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                if (banner.title.isNotBlank()) {
                                    Text(
                                        text = banner.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (banner.subtitle.isNotBlank()) {
                                    Text(
                                        text = banner.subtitle,
                                        color = Color(0xFFD0D0E0),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun UpcomingEventsBannerRow(
    events: List<MatchItem>,
    onEventClick: (MatchItem) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(events, key = { it.id }) { event ->
            val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
            val formattedTime = remember(event.startTimeMillis) {
                if (event.startTimeMillis > 0L) timeFormat.format(Date(event.startTimeMillis)) else event.matchTime
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141F)),
                border = BorderStroke(1.dp, Color(0xFF2D2D44)),
                modifier = Modifier
                    .width(320.dp)
                    .height(165.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onEventClick(event) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (event.homeTeamLogo.isNotBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(event.homeTeamLogo)
                                .crossfade(true)
                                .build(),
                            contentDescription = event.homeTeamArabicName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = event.homeTeamArabicName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${event.leagueName} • $formattedTime",
                            color = BrightLavender,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header bar shown when a user clicks into a Branch inside a Category.
 */
@Composable
private fun ActiveBranchHeaderBar(
    branch: ChannelBranch,
    onBackToCategory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF14141F),
        border = BorderStroke(1.dp, Color(0xFF2E2E42)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF232336),
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBackToCategory)
                        .testTag("back_from_branch_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة للقسم",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = branch.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElectricPurple.copy(alpha = 0.2f),
                border = BorderStroke(0.8.dp, ElectricPurple.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackToCategory)
            ) {
                Text(
                    text = "رجوع للقسم",
                    color = BrightLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Branch Card in the channel grid:
 * Renders in the EXACT same style as ChannelGridCard
 */
@Composable
fun BranchGridCard(
    branch: ChannelBranch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardBrush = remember(branch.name, branch.categoryName) {
        getChannelCardBrush(branch.name, branch.categoryName)
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
            .testTag("branch_card_${branch.id}")
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
            if (branch.imageUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(branch.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = branch.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    error = {
                        ChannelLogoPlaceholder(channelName = branch.name)
                    }
                )
            } else {
                ChannelLogoPlaceholder(channelName = branch.name)
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = branch.name,
            color = Color.White,
            fontSize = 12.sp,
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

/**
 * 3-Column Channel Item:
 * - Rounded squircle card with centered channel logo/graphic
 * - Multi-quality servers badge when channel has > 1 merged quality servers
 * - Channel title placed directly under the squircle card in clean white text
 */
@Composable
fun ChannelGridCard(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardBrush = remember(channel.name, channel.category) {
        getChannelCardBrush(channel.name, channel.category)
    }

    val borderColor = when {
        isSelected -> NeonPurple
        isFocused -> ElectricPurple
        else -> Color(0xFF282838)
    }
    val borderWidth = if (isSelected || isFocused) 2.dp else 1.1.dp
    val serversCount = channel.effectiveServers.size

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
            .testTag("channel_card_${channel.id}")
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
            val cleanImgUrl = remember(channel.imageUrl) {
                val raw = channel.imageUrl.trim().removeSurrounding("\"").removeSurrounding("'").trim()
                if (raw.startsWith("//")) "https:$raw" else raw
            }

            if (cleanImgUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cleanImgUrl)
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .build(),
                    contentDescription = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    error = {
                        ChannelLogoPlaceholder(channelName = channel.name)
                    }
                )
            } else {
                ChannelLogoPlaceholder(channelName = channel.name)
            }

            // Multi-server / Multi-quality badge in top-end corner
            if (serversCount > 1) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 20.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(0.8.dp, AccentCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "${serversCount}Q",
                            color = AccentCyan,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NeonPurple.copy(alpha = 0.18f))
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 12.sp,
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

/**
 * Fallback stylized logo placeholder if image is absent or failing to load
 */
@Composable
fun ChannelLogoPlaceholder(channelName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = channelName.take(3).trim(),
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private fun getChannelCardBrush(name: String, category: String): Brush {
    val lower = name.lowercase()
    return when {
        lower.contains("دبي") || lower.contains("dubai") -> {
            Brush.verticalGradient(listOf(Color(0xFFC62828), Color(0xFF8E0000)))
        }
        lower.contains("بحرين") || lower.contains("bahrain") -> {
            Brush.verticalGradient(listOf(Color(0xFFB71C1C), Color(0xFF6B0E17)))
        }
        lower.contains("كأس") || lower.contains("alkass") || lower.contains("kass") -> {
            Brush.verticalGradient(listOf(Color(0xFF881024), Color(0xFF4A0814)))
        }
        lower.contains("ad sport") || lower.contains("أبوظبي") || lower.contains("ابوظبي") -> {
            Brush.verticalGradient(listOf(Color(0xFF14141E), Color(0xFF0A0A10)))
        }
        lower.contains("bein") || lower.contains("بي ان") || lower.contains("بي إن") -> {
            Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF1E0638)))
        }
        lower.contains("ssc") -> {
            Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF051D44)))
        }
        lower.contains("شاهد") || lower.contains("mbc") -> {
            Brush.verticalGradient(listOf(Color(0xFF181826), Color(0xFF0E0E16)))
        }
        else -> {
            Brush.verticalGradient(listOf(Color(0xFF181826), Color(0xFF0F0F16)))
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChannelGridCard(
        channel = channel,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ChannelsLoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(DarkPurpleContainer, CircleShape)
                        .border(1.2.dp, VividViolet.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = NeonPurple,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "جاري تحميل القنوات...",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "يرجى الانتظار لحظات لمزامنة القنوات المباشرة",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmptyChannelsView(
    onOpenPublisher: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(DarkPurpleContainer, CircleShape)
                        .border(1.2.dp, VividViolet.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = "Empty Channels",
                        tint = BrightLavender,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "لا توجد قنوات حالياً",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "يمكنك إضافة القنوات والأقسام مباشرة من لوحة تحكم المطور.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                androidx.compose.material3.Button(
                    onClick = onOpenPublisher,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ElectricPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "فتح لوحة تحكم المطور",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
