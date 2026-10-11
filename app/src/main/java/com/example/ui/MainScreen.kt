package com.example.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Channel
import com.example.ui.chat.ChatAuthDialog
import com.example.ui.chat.ChatScreen
import com.example.ui.components.ChannelGrid
import com.example.ui.components.PremiumBottomNav
import com.example.ui.components.PremiumHeader
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.iptv.IptvScreen
import com.example.ui.language.AppStrings
import com.example.ui.language.LanguageDialog
import com.example.ui.language.LocalAppStrings
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryVideoPlayer
import com.example.ui.matches.MatchesScheduleScreen
import com.example.ui.player.CustomVideoPlayer
import com.example.ui.player.PlayerFactory
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.viewmodel.FootballViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: FootballViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isLanguageDialogOpen by viewModel.isLanguageDialogOpen.collectAsStateWithLifecycle()
    val strings = AppStrings.forLanguage(currentLanguage)

    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val isChannelsLoading by viewModel.isChannelsLoading.collectAsStateWithLifecycle()
    val mostWatchedChannels by viewModel.mostWatchedChannels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val xtremeAccounts by viewModel.xtremeAccounts.collectAsStateWithLifecycle()
    val eventBanners by viewModel.eventBanners.collectAsStateWithLifecycle()
    val iptvLiveChannels by viewModel.iptvLiveChannels.collectAsStateWithLifecycle()
    val selectedIptvAccount by viewModel.selectedIptvAccount.collectAsStateWithLifecycle()
    val isIptvLoading by viewModel.isIptvLoading.collectAsStateWithLifecycle()
    val iptvErrorMessage by viewModel.iptvErrorMessage.collectAsStateWithLifecycle()
    val playingIptvChannel by viewModel.playingIptvChannel.collectAsStateWithLifecycle()
    val isIptvFullscreen by viewModel.isIptvFullscreen.collectAsStateWithLifecycle()
    val currentNavIndex by viewModel.currentNavIndex.collectAsStateWithLifecycle()
    val libraryItems by viewModel.libraryItems.collectAsStateWithLifecycle()
    val isLibraryLoading by viewModel.isLibraryLoading.collectAsStateWithLifecycle()
    val isAccountUpdatedError by viewModel.isAccountUpdatedError.collectAsStateWithLifecycle()
    val selectedLibraryAccount by viewModel.selectedLibraryAccount.collectAsStateWithLifecycle()
    val isLibraryAllAccountsMode by viewModel.isLibraryAllAccountsMode.collectAsStateWithLifecycle()

    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val selectedMatchDate by viewModel.selectedMatchDate.collectAsStateWithLifecycle()
    val selectedMatchLeagueFilter by viewModel.selectedMatchLeagueFilter.collectAsStateWithLifecycle()
    val isMatchesLoading by viewModel.isMatchesLoading.collectAsStateWithLifecycle()
    val matchesErrorMessage by viewModel.matchesErrorMessage.collectAsStateWithLifecycle()
    val selectedMatchDetail by viewModel.selectedMatchDetail.collectAsStateWithLifecycle()
    val matchSummary by viewModel.matchSummary.collectAsStateWithLifecycle()
    val isSummaryLoading by viewModel.isSummaryLoading.collectAsStateWithLifecycle()

    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    val hideHomeAll by viewModel.hideHomeAll.collectAsStateWithLifecycle()
    val hideHomeMode by viewModel.hideHomeMode.collectAsStateWithLifecycle()
    val hideHomeCategory by viewModel.hideHomeCategory.collectAsStateWithLifecycle()
    val hideChatInPlayer by viewModel.hideChatInPlayer.collectAsStateWithLifecycle()
    val hideOnlineViewers by viewModel.hideOnlineViewers.collectAsStateWithLifecycle()
    val hideMatchesSchedule by viewModel.hideMatchesSchedule.collectAsStateWithLifecycle()
    val hideFoulTv by viewModel.hideFoulTv.collectAsStateWithLifecycle()
    val lockOldVersions by viewModel.lockOldVersions.collectAsStateWithLifecycle()
    val appVersion = viewModel.appVersion
    val latestVersion by viewModel.latestVersion.collectAsStateWithLifecycle()
    val updateType by viewModel.updateType.collectAsStateWithLifecycle()
    val updateText by viewModel.updateText.collectAsStateWithLifecycle()
    val updateUrl by viewModel.updateUrl.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val scaleMode by viewModel.scaleMode.collectAsStateWithLifecycle()
    val isLibraryFullscreen by viewModel.isLibraryFullscreen.collectAsStateWithLifecycle()
    val selectedLibraryItem by viewModel.selectedLibraryItem.collectAsStateWithLifecycle()
    val playingLibraryItem by viewModel.playingLibraryItem.collectAsStateWithLifecycle()
    val seriesInfo by viewModel.seriesInfo.collectAsStateWithLifecycle()
    val isSeriesInfoLoading by viewModel.isSeriesInfoLoading.collectAsStateWithLifecycle()
    val vodInfo by viewModel.vodInfo.collectAsStateWithLifecycle()
    val isVodInfoLoading by viewModel.isVodInfoLoading.collectAsStateWithLifecycle()

    val isChatOpen by viewModel.isChatOpen.collectAsStateWithLifecycle()
    val isLandscapeChatOpen by viewModel.isLandscapeChatOpen.collectAsStateWithLifecycle()
    val unreadChatCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()
    val activeChatChannel by viewModel.activeChatChannel.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val currentChatUser by viewModel.currentChatUser.collectAsStateWithLifecycle()
    val isChatAuthDialogOpen by viewModel.isChatAuthDialogOpen.collectAsStateWithLifecycle()
    val onlineViewersCount by viewModel.onlineViewersCount.collectAsStateWithLifecycle()
    val branches by viewModel.branches.collectAsStateWithLifecycle()

    val foulVideos by viewModel.foulVideos.collectAsStateWithLifecycle()
    val foulGlobalConfig by viewModel.foulGlobalConfig.collectAsStateWithLifecycle()
    val playingFoulVideo by viewModel.playingFoulVideo.collectAsStateWithLifecycle()
    val isFoulVideoFullscreen by viewModel.isFoulVideoFullscreen.collectAsStateWithLifecycle()
    val foulComments by viewModel.foulComments.collectAsStateWithLifecycle()

    val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsStateWithLifecycle()
    val adminUserEmail by viewModel.adminUserEmail.collectAsStateWithLifecycle()
    val firebaseMatches by viewModel.firebaseMatches.collectAsStateWithLifecycle()
    val isDeveloperAuthDialogOpen by viewModel.isDeveloperAuthDialogOpen.collectAsStateWithLifecycle()
    val isAdminAuthenticating by viewModel.isAdminAuthenticating.collectAsStateWithLifecycle()
    val adminAuthErrorMessage by viewModel.adminAuthErrorMessage.collectAsStateWithLifecycle()
    val isPublisherDialogOpen by viewModel.isPublisherDialogOpen.collectAsStateWithLifecycle()
    val publisherInitialTab by viewModel.publisherInitialTab.collectAsStateWithLifecycle()
    val editingChannel by viewModel.editingChannel.collectAsStateWithLifecycle()
    val isFoulPublishVideoDialogOpen by viewModel.isFoulPublishVideoDialogOpen.collectAsStateWithLifecycle()
    val editingFoulVideo by viewModel.editingFoulVideo.collectAsStateWithLifecycle()
    val developerStatusMessage by viewModel.developerStatusMessage.collectAsStateWithLifecycle()

    var isQuickChannelsOpen by remember { mutableStateOf(false) }
    var isDownloadsOpen by remember { mutableStateOf(false) }
    var eventBannerToChooseChannels by remember { mutableStateOf<com.example.data.model.EventBanner?>(null) }
    var eventMatchedChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }

    BackHandler(enabled = isDownloadsOpen) {
        isDownloadsOpen = false
    }

    BackHandler(enabled = isFullscreen && isQuickChannelsOpen) {
        isQuickChannelsOpen = false
    }

    BackHandler(enabled = isChatOpen) {
        viewModel.closeChat()
    }

    BackHandler(enabled = isFullscreen && isLandscapeChatOpen) {
        viewModel.closeLandscapeChat()
    }

    val telegramChannelUrl = "https://t.me/jdj_q"

    val lifecycleOwner = LocalLifecycleOwner.current
    val homeExoPlayer = remember {
        PlayerFactory.createExoPlayer(context)
    }
    DisposableEffect(lifecycleOwner, homeExoPlayer) {
        onDispose {
            homeExoPlayer.stop()
            homeExoPlayer.clearMediaItems()
            homeExoPlayer.release()
        }
    }

    LaunchedEffect(currentNavIndex, playingLibraryItem, isFullscreen, playingFoulVideo) {
        if ((currentNavIndex != 0 && currentNavIndex != 1) || playingLibraryItem != null || playingFoulVideo != null || !isFullscreen) {
            homeExoPlayer.stop()
            homeExoPlayer.clearMediaItems()
            viewModel.pauseHomePlayer()
        } else if ((currentNavIndex == 0 || currentNavIndex == 1) && playingLibraryItem == null && playingFoulVideo == null && isFullscreen) {
            viewModel.stopLibraryPlayback()
            viewModel.stopFoulVideo()
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides (if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr),
        LocalAppStrings provides strings
    ) {
        if (isDownloadsOpen) {
            DownloadsScreen(
                onBack = { isDownloadsOpen = false },
                onPlayDownload = { downloadItem ->
                    isDownloadsOpen = false
                    val localUriOrPath = downloadItem.localUri ?: downloadItem.fileName
                    val movieItem = com.example.data.model.XtremeItem(
                        id = downloadItem.downloadId.toString(),
                        title = downloadItem.title,
                        streamId = downloadItem.downloadId.toString(),
                        streamIcon = "",
                        type = com.example.data.model.XtremeContentType.MOVIE,
                        categoryId = "downloads",
                        categoryName = strings.downloads,
                        streamUrl = localUriOrPath,
                        accountName = strings.downloads,
                        accountId = "local_downloads"
                    )
                    viewModel.playLibraryItem(movieItem)
                },
                modifier = modifier.fillMaxSize()
            )
            return@CompositionLocalProvider
        }

        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CustomVideoPlayer(
                        channel = selectedChannel,
                        selectedServer = selectedServer,
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        isFullscreen = true,
                        scaleMode = scaleMode,
                        onServerSelected = { server ->
                            viewModel.selectServer(server)
                        },
                        onTogglePlay = {
                            viewModel.togglePlay()
                        },
                        onToggleMute = {
                            viewModel.toggleMute()
                        },
                        onToggleFullscreen = {
                            viewModel.toggleFullscreen()
                        },
                        onCycleScaleMode = {
                            viewModel.toggleNextScaleMode()
                        },
                        onNextChannel = {
                            viewModel.selectNextChannel()
                        },
                        onPreviousChannel = {
                            viewModel.selectPreviousChannel()
                        },
                        exoPlayer = homeExoPlayer,
                        onlineViewersCount = onlineViewersCount,
                        hideOnlineViewers = hideOnlineViewers,
                        onOpenChat = {
                            if (isQuickChannelsOpen) isQuickChannelsOpen = false
                            viewModel.toggleLandscapeChat()
                        },
                        hideChatInPlayer = hideChatInPlayer,
                        unreadChatCount = unreadChatCount,
                        isChatOpen = isLandscapeChatOpen,
                        onToggleQuickChannels = {
                            if (isLandscapeChatOpen) viewModel.closeLandscapeChat()
                            isQuickChannelsOpen = !isQuickChannelsOpen
                        },
                        isQuickChannelsOpen = isQuickChannelsOpen,
                        modifier = if (isLandscapeChatOpen || isQuickChannelsOpen) Modifier.weight(0.55f).fillMaxHeight() else Modifier.fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = isQuickChannelsOpen,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it },
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    ) {
                        com.example.ui.components.MainChannelSplitPane(
                            currentChannel = selectedChannel,
                            channels = allChannels,
                            categories = categories,
                            branches = branches,
                            onSelectChannel = { ch ->
                                viewModel.selectChannel(ch)
                            },
                            onClose = { isQuickChannelsOpen = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AnimatedVisibility(
                        visible = isLandscapeChatOpen,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it },
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    ) {
                        Surface(
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ChatScreen(
                                messages = chatMessages,
                                currentUser = currentChatUser,
                                channel = activeChatChannel,
                                allChannels = allChannels,
                                onSelectChannel = { ch -> viewModel.selectChatChannel(ch) },
                                onSendMessage = { text, replyTo -> viewModel.sendChatMessage(text, replyTo) },
                                onDeleteMessage = { msgId -> viewModel.deleteChatMessage(msgId) },
                                onToggleReaction = { msgId, emoji -> viewModel.toggleChatReaction(msgId, emoji) },
                                onRequireAuth = { viewModel.openChatAuthDialog() },
                                onLogout = { viewModel.logoutChatUser() },
                                onBack = { viewModel.closeLandscapeChat() },
                                isCompactLandscape = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            return@CompositionLocalProvider
        }

        if (isLibraryFullscreen && playingLibraryItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                LibraryVideoPlayer(
                    item = playingLibraryItem!!,
                    onClosePlayer = {
                        viewModel.playLibraryItem(null)
                    },
                    isFullscreen = true,
                    onToggleFullscreen = {
                        viewModel.playLibraryItem(null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@CompositionLocalProvider
        }

        if (isIptvFullscreen && playingIptvChannel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                com.example.ui.iptv.IptvVideoPlayer(
                    item = playingIptvChannel!!,
                    availableChannels = iptvLiveChannels,
                    onSelectChannel = { ch -> viewModel.playIptvChannel(ch) },
                    onClosePlayer = {
                        viewModel.stopIptvPlayback()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@CompositionLocalProvider
        }

        if (isFoulVideoFullscreen && playingFoulVideo != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                com.example.ui.foultv.FoulVideoPlayer(
                    video = playingFoulVideo!!,
                    currentUser = currentChatUser,
                    comments = foulComments,
                    globalConfig = foulGlobalConfig,
                    onClosePlayer = {
                        viewModel.stopFoulVideo()
                    },
                    onToggleLike = { videoId ->
                        viewModel.toggleFoulVideoLike(videoId)
                    },
                    onAddComment = { comment ->
                        viewModel.addFoulComment(comment)
                    },
                    onDeleteComment = { videoId, commentId ->
                        viewModel.deleteFoulComment(videoId, commentId)
                    },
                    onIncrementViews = { videoId ->
                        viewModel.incrementFoulVideoViews(videoId)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@CompositionLocalProvider
        }

        var isSettingsOpenOnForceUpdate by remember { mutableStateOf(false) }
        var isForceUpdateDismissed by remember { mutableStateOf(false) }

        val isForceUpdateRequired = remember(appVersion, latestVersion, updateType, lockOldVersions, updateText, isForceUpdateDismissed) {
            if (isForceUpdateDismissed) return@remember false

            if (com.example.data.security.AppSecurityGuard.isMaliciousText(updateText) ||
                com.example.data.security.AppSecurityGuard.isSuspiciousVersion(latestVersion)) {
                return@remember false
            }

            val isVersionOutdated = com.example.ui.viewmodel.FootballViewModel.isVersionOlder(appVersion, latestVersion)
            val isLegitimate = com.example.data.security.AppSecurityGuard.isLegitimateUpdate(appVersion, latestVersion, updateText)

            if (isVersionOutdated && !isLegitimate) {
                return@remember false
            }

            val isExplicitForced = updateType.equals("forced", ignoreCase = true) ||
                                  updateType.equals("required", ignoreCase = true) ||
                                  updateType.equals("mandatory", ignoreCase = true)
            val isOptionalWithOlder = isVersionOutdated && updateType.equals("optional", ignoreCase = true)
            val isLockEnabled = lockOldVersions || isExplicitForced

            (isVersionOutdated && (isLockEnabled || isOptionalWithOlder)) || (lockOldVersions && isVersionOutdated) || (isExplicitForced && appVersion != latestVersion)
        }

        if (isForceUpdateRequired) {
            com.example.ui.update.ForceUpdateScreen(
                currentVersion = appVersion,
                latestVersion = latestVersion,
                updateText = updateText,
                updateUrl = updateUrl,
                telegramUrl = "https://t.me/jdj_q",
                onOpenSettings = { isSettingsOpenOnForceUpdate = true },
                onDismiss = { isForceUpdateDismissed = true },
                modifier = Modifier.fillMaxSize()
            )

            if (isSettingsOpenOnForceUpdate) {
                androidx.activity.compose.BackHandler {
                    isSettingsOpenOnForceUpdate = false
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { isSettingsOpenOnForceUpdate = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkSurfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "الإعدادات العامة",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.size(40.dp))
                        }

                        SettingsScreen(
                            onOpenDeveloperPanel = {
                                isSettingsOpenOnForceUpdate = false
                                viewModel.openDeveloperPanel(0)
                            },
                            onOpenBranchPublisher = {
                                isSettingsOpenOnForceUpdate = false
                                viewModel.openDeveloperPanel(2)
                            },
                            channelsCount = allChannels.size,
                            xtremeAccountsCount = xtremeAccounts.size,
                            appVersion = appVersion,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            return@CompositionLocalProvider
        }

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isChatOpen && selectedMatchDetail == null) {
                    PremiumBottomNav(
                        currentIndex = currentNavIndex,
                        onTabSelected = { index -> viewModel.setNavIndex(index) },
                        hideMatchesSchedule = hideMatchesSchedule,
                        hideFoulTv = hideFoulTv
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
                if (selectedMatchDetail == null) {
                    PremiumHeader(
                        currentLanguage = currentLanguage,
                        onOpenLanguageSelector = { viewModel.openLanguageDialog() },
                        onOpenDownloads = { isDownloadsOpen = true },
                        onOpenDeveloper = { viewModel.openDeveloperPanel(0) },
                        isAdminAuthenticated = isAdminAuthenticated,
                        telegramUrl = telegramChannelUrl
                    )
                }

                when (currentNavIndex) {
                    0 -> {
                        if (isChatOpen) {
                            ChatScreen(
                                messages = chatMessages,
                                currentUser = currentChatUser,
                                channel = activeChatChannel,
                                allChannels = allChannels,
                                onSelectChannel = { ch ->
                                    viewModel.selectChatChannel(ch)
                                },
                                onSendMessage = { text, replyTo ->
                                    viewModel.sendChatMessage(text, replyTo)
                                },
                                onDeleteMessage = { msgId ->
                                    viewModel.deleteChatMessage(msgId)
                                },
                                onToggleReaction = { msgId, emoji ->
                                    viewModel.toggleChatReaction(msgId, emoji)
                                },
                                onRequireAuth = {
                                    viewModel.openChatAuthDialog()
                                },
                                onLogout = {
                                    viewModel.logoutChatUser()
                                },
                                onBack = {
                                    viewModel.closeChat()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            var homeCategory by remember { mutableStateOf("الكل") }
                            var homeBranch by remember { mutableStateOf<com.example.data.model.ChannelBranch?>(null) }
                            var homeSearchQuery by remember { mutableStateOf("") }

                            ChannelGrid(
                                channels = allChannels,
                                categories = categories,
                                branches = branches,
                                isLoadingCategories = isChannelsLoading,
                                isLoadingChannels = isChannelsLoading,
                                upcomingEvents = firebaseMatches,
                                eventBanners = eventBanners,
                                selectedCategory = homeCategory,
                                selectedBranch = homeBranch,
                                selectedChannel = selectedChannel,
                                searchQuery = homeSearchQuery,
                                onSearchQueryChange = { homeSearchQuery = it },
                                onCategorySelected = { homeCategory = it },
                                onBranchSelected = { homeBranch = it },
                                onChannelSelected = { channel ->
                                    viewModel.playChannelFullscreen(channel)
                                },
                                onPlayChannelWithServer = { channel, server ->
                                    viewModel.playChannelFullscreen(channel)
                                    viewModel.selectServer(server)
                                },
                                onEventClick = { match ->
                                    viewModel.playMatchBroadcast(match)
                                },
                                onBannerClick = { banner ->
                                    val allTargetChannelIds = banner.getAllChannelIds()
                                    val matched = allChannels.filter { ch -> allTargetChannelIds.contains(ch.id) }
                                    if (matched.size > 1) {
                                        eventBannerToChooseChannels = banner
                                        eventMatchedChannels = matched
                                    } else if (matched.size == 1) {
                                        viewModel.playChannelFullscreen(matched.first())
                                    } else {
                                        val targetChannel = if (banner.channelId.isNotBlank()) {
                                            allChannels.find { it.id == banner.channelId }
                                        } else if (banner.watchNowUrl.isNotBlank() && !banner.watchNowUrl.startsWith("http")) {
                                            allChannels.find { it.id == banner.watchNowUrl || it.name.contains(banner.watchNowUrl, ignoreCase = true) }
                                        } else {
                                            allChannels.find { it.name.contains(banner.title, ignoreCase = true) }
                                        }
                                        if (targetChannel != null) {
                                            viewModel.playChannelFullscreen(targetChannel)
                                        }
                                    }
                                },
                                onOpenPublisher = { viewModel.openDeveloperPanel(0) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    1 -> {
                        IptvScreen(
                            items = iptvLiveChannels,
                            categories = emptyList(),
                            accounts = xtremeAccounts,
                            selectedAccount = selectedIptvAccount,
                            isLoading = isIptvLoading,
                            errorMessage = iptvErrorMessage,
                            onSelectAccount = { acc -> viewModel.selectIptvAccount(acc) },
                            onRefresh = { viewModel.loadIptvChannels() },
                            onPlayChannel = { channelItem -> viewModel.playIptvChannel(channelItem) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    2 -> {
                        com.example.ui.foultv.FoulTvScreen(
                            videos = foulVideos,
                            playingVideo = playingFoulVideo,
                            currentUser = currentChatUser,
                            comments = foulComments,
                            globalConfig = foulGlobalConfig,
                            onPlayVideo = { video -> viewModel.playFoulVideo(video, fullscreen = false) },
                            onToggleFullscreen = { viewModel.setFoulVideoFullscreen(true) },
                            onStopVideo = { viewModel.stopFoulVideo() },
                            onToggleLike = { videoId -> viewModel.toggleFoulVideoLike(videoId) },
                            onIncrementViews = { videoId -> viewModel.incrementFoulVideoViews(videoId) },
                            onAddComment = { comment -> viewModel.addFoulComment(comment) },
                            onDeleteComment = { videoId, commentId -> viewModel.deleteFoulComment(videoId, commentId) },
                            onOpenPublishVideoDialog = { viewModel.openFoulPublishVideoDialog(null) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    3 -> {
                        LibraryScreen(
                            libraryItems = libraryItems,
                            selectedItem = selectedLibraryItem,
                            seriesInfo = seriesInfo,
                            isSeriesInfoLoading = isSeriesInfoLoading,
                            vodInfo = vodInfo,
                            isVodInfoLoading = isVodInfoLoading,
                            onSelectItem = { item ->
                                viewModel.selectLibraryItem(item)
                            },
                            onPlayMovie = { movie ->
                                viewModel.playLibraryItem(movie)
                            },
                            onPlayEpisode = { episode, series ->
                                viewModel.playEpisode(episode, series)
                            },
                            isLoading = isLibraryLoading,
                            isAccountUpdatedError = isAccountUpdatedError,
                            onRefresh = { viewModel.refreshLibraryContent() },
                            accounts = xtremeAccounts,
                            selectedAccount = selectedLibraryAccount,
                            isAllAccountsMode = isLibraryAllAccountsMode,
                            onSelectAccount = { account -> viewModel.selectLibraryAccount(account) },
                            onSelectAllAccounts = { viewModel.setLibraryAllAccountsMode() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    4 -> {
                        MatchesScheduleScreen(
                            matches = matches,
                            selectedDate = selectedMatchDate,
                            selectedFilter = selectedMatchLeagueFilter,
                            isLoading = isMatchesLoading,
                            errorMessage = matchesErrorMessage,
                            selectedMatchDetail = selectedMatchDetail,
                            matchSummary = matchSummary,
                            isSummaryLoading = isSummaryLoading,
                            onSelectDate = { date -> viewModel.selectMatchDate(date) },
                            onSelectFilter = { filter -> viewModel.selectMatchLeagueFilter(filter) },
                            onRefresh = { viewModel.refreshMatches() },
                            onOpenMatchDetail = { match -> viewModel.openMatchDetail(match) },
                            onCloseMatchDetail = { viewModel.closeMatchDetail() },
                            onRefreshSummary = { viewModel.loadMatchSummary() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> Unit
                }
            }

            LanguageDialog(
                isOpen = isLanguageDialogOpen,
                currentLanguage = currentLanguage,
                onSelectLanguage = { lang ->
                    viewModel.selectLanguage(lang)
                },
                onDismiss = {
                    viewModel.closeLanguageDialog()
                }
            )

            ChatAuthDialog(
                isOpen = isChatAuthDialogOpen,
                onDismiss = { viewModel.closeChatAuthDialog() },
                onRegister = { user, callback ->
                    viewModel.registerChatUser(user, callback)
                },
                onLogin = { username, password, callback ->
                    viewModel.loginChatUser(username, password, callback)
                }
            )

            com.example.ui.components.EventChannelChooserDialog(
                isOpen = (eventBannerToChooseChannels != null && eventMatchedChannels.isNotEmpty()),
                eventBanner = eventBannerToChooseChannels,
                channels = eventMatchedChannels,
                onSelectChannel = { channel ->
                    eventBannerToChooseChannels = null
                    eventMatchedChannels = emptyList()
                    viewModel.playChannelFullscreen(channel)
                },
                onDismiss = {
                    eventBannerToChooseChannels = null
                    eventMatchedChannels = emptyList()
                }
            )

            if (isDeveloperAuthDialogOpen) {
                com.example.ui.publisher.DeveloperAuthDialog(
                    isAuthenticating = isAdminAuthenticating,
                    authErrorMessage = adminAuthErrorMessage,
                    onDismiss = { viewModel.closeDeveloperAuthDialog() },
                    onAuthenticate = { email, password ->
                        viewModel.authenticateDeveloper(email, password)
                    }
                )
            }

            if (isPublisherDialogOpen) {
                com.example.ui.publisher.PublisherDialog(
                    initialTabIndex = publisherInitialTab,
                    initialEditingChannel = editingChannel,
                    adminEmail = adminUserEmail,
                    channels = allChannels,
                    categories = categories,
                    branches = branches,
                    eventBanners = eventBanners,
                    firebaseMatches = firebaseMatches,
                    xtremeAccounts = xtremeAccounts,
                    foulVideos = foulVideos,
                    foulGlobalConfig = foulGlobalConfig,
                    latestAppVersion = latestVersion,
                    forceUpdateRequired = lockOldVersions,
                    updateMessage = updateText,
                    updateDownloadUrl = updateUrl,
                    telegramSupportUrl = telegramChannelUrl,
                    developerStatusMessage = developerStatusMessage,
                    onDismiss = { viewModel.closePublisherDialog() },
                    onSignOutAdmin = { viewModel.signOutDeveloper() },
                    onSaveChannel = { ch -> viewModel.saveChannelAdmin(ch) },
                    onDeleteChannel = { id -> viewModel.deleteChannelAdmin(id) },
                    onClearAllChannels = { viewModel.clearAllChannelsAdmin() },
                    onSaveCategory = { cat -> viewModel.saveCategoryAdmin(cat) },
                    onDeleteCategory = { id -> viewModel.deleteCategoryAdmin(id) },
                    onSaveBranch = { br -> viewModel.saveBranchAdmin(br) },
                    onDeleteBranch = { id -> viewModel.deleteBranchAdmin(id) },
                    onSaveEventBanner = { banner -> viewModel.saveEventBannerAdmin(banner) },
                    onDeleteEventBanner = { id -> viewModel.deleteEventBannerAdmin(id) },
                    onSaveLiveMatch = { id, name, tournament, broadcaster, imageUrl, startTimeMs, targetChannelId, servers ->
                        viewModel.saveLiveMatchAdmin(id, name, tournament, broadcaster, imageUrl, startTimeMs, targetChannelId, servers)
                    },
                    onDeleteLiveMatch = { id -> viewModel.deleteLiveMatchAdmin(id) },
                    onSaveXtremeAccount = { acc -> viewModel.saveXtremeAccountAdmin(acc) },
                    onDeleteXtremeAccount = { id -> viewModel.deleteXtremeAccountAdmin(id) },
                    onOpenPublishFoulVideo = { viewModel.openFoulPublishVideoDialog(null) },
                    onEditFoulVideo = { video -> viewModel.openFoulPublishVideoDialog(video) },
                    onDeleteFoulVideo = { id -> viewModel.deleteFoulVideoAdmin(id) },
                    onUpdateFoulGlobalConfig = { cfg -> viewModel.updateFoulGlobalConfigAdmin(cfg) },
                    onSaveAppConfig = { ver, force, opt, msg, url, tg, newPass ->
                        viewModel.saveAppConfigAdmin(ver, force, opt, msg, url, tg, newPass)
                    },
                    onPlayChannelPreview = { ch, srv ->
                        viewModel.playChannelFullscreen(ch)
                        if (srv != null) viewModel.selectServer(srv)
                    }
                )
            }

            if (isFoulPublishVideoDialogOpen) {
                com.example.ui.foultv.FoulPublishVideoDialog(
                    initialVideo = editingFoulVideo,
                    onDismiss = { viewModel.closeFoulPublishVideoDialog() },
                    onPublish = { video -> viewModel.publishOrUpdateFoulVideoAdmin(video) }
                )
            }
        }
    }
}
