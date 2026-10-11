package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cache.LibraryCacheManager
import com.example.data.matches.MatchItem
import com.example.data.matches.MatchRepository
import com.example.data.matches.MatchSummaryDetail
import com.example.data.model.Channel
import com.example.data.model.ChannelBranch
import com.example.data.model.ChannelCategory
import com.example.data.model.ChannelServer
import com.example.data.model.XtremeAccount
import com.example.data.model.XtremeContentType
import com.example.data.model.XtremeItem
import com.example.data.repository.ChannelRepository
import com.example.data.util.M3uParser
import com.example.data.xtreme.XtremeApiClient
import com.example.ui.language.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FootballViewModel(
    application: Application,
    private val repository: ChannelRepository,
    private val xtremeApiClient: XtremeApiClient
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = ChannelRepository(),
        xtremeApiClient = XtremeApiClient()
    )

    private val cacheManager = LibraryCacheManager(application.applicationContext)
    private val channelCacheManager = com.example.data.cache.ChannelCacheManager(application.applicationContext)
    private val chatRepository = com.example.data.repository.ChatRepository(application.applicationContext)
    private val presenceRepository = com.example.data.repository.ChannelPresenceRepository(application.applicationContext)
    private val foulTvRepository = com.example.data.repository.FoulTvRepository()

    // FOUL TV Videos & Configuration Flows from Firebase
    val foulVideos: StateFlow<List<com.example.data.model.FoulVideo>> = foulTvRepository.getVideosFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val foulGlobalConfig: StateFlow<com.example.data.model.FoulTvGlobalConfig> = foulTvRepository.getGlobalConfigFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.example.data.model.FoulTvGlobalConfig()
        )

    private val _playingFoulVideo = MutableStateFlow<com.example.data.model.FoulVideo?>(null)
    val playingFoulVideo: StateFlow<com.example.data.model.FoulVideo?> = _playingFoulVideo.asStateFlow()

    private val _isFoulVideoFullscreen = MutableStateFlow(false)
    val isFoulVideoFullscreen: StateFlow<Boolean> = _isFoulVideoFullscreen.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val foulComments: StateFlow<List<com.example.data.model.FoulComment>> = _playingFoulVideo
        .flatMapLatest { video ->
            if (video != null) {
                foulTvRepository.getCommentsFlow(video.id)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Synchronously load cached channels and categories for instant 0ms preloaded display upon opening
    private val initialCachedChannels: List<Channel> = channelCacheManager.loadCachedChannelsSync()
    private val initialCachedCategories: List<ChannelCategory> = channelCacheManager.loadCachedCategoriesSync()

    // Current Chat User (Instant direct guest user - No login required)
    private val _currentChatUser = MutableStateFlow<com.example.data.model.ChatUser?>(chatRepository.getOrCreateGuestUser())
    val currentChatUser: StateFlow<com.example.data.model.ChatUser?> = _currentChatUser.asStateFlow()

    // Auth Dialog State
    private val _isChatAuthDialogOpen = MutableStateFlow(false)
    val isChatAuthDialogOpen: StateFlow<Boolean> = _isChatAuthDialogOpen.asStateFlow()

    // Real-time Online Viewers Count for current channel (Only active for first 15 channels)
    private val _onlineViewersCount = MutableStateFlow<Int?>(null)
    val onlineViewersCount: StateFlow<Int?> = _onlineViewersCount.asStateFlow()

    private val _isChannelsLoading = MutableStateFlow(initialCachedChannels.isEmpty())
    val isChannelsLoading: StateFlow<Boolean> = _isChannelsLoading.asStateFlow()

    private var presenceJob: kotlinx.coroutines.Job? = null

    // Main Live Channels from Firebase with zero-delay instant preloading
    val channels: StateFlow<List<Channel>> = repository.getChannelsFlow()
        .onEach { _isChannelsLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialCachedChannels
        )

    // Top 15 Most Watched Channels dynamically sorted by views from Firebase
    val mostWatchedChannels: StateFlow<List<Channel>> = channels
        .map { list ->
            list.sortedWith(
                compareByDescending<Channel> { it.views }
                    .thenByDescending { it.createdAt }
            ).take(15)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialCachedChannels.sortedWith(
                compareByDescending<Channel> { it.views }
                    .thenByDescending { it.createdAt }
            ).take(15)
        )

    // Channel Categories from Firebase
    val categories: StateFlow<List<ChannelCategory>> = repository.getCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialCachedCategories
        )

    // Channel Branches (الفروع داخل التصنيفات) from Firebase
    val branches: StateFlow<List<ChannelBranch>> = repository.getBranchesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _m3uChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _categoryXtremeChannels = MutableStateFlow<List<Channel>>(emptyList())

    // Combined channels (Firebase + parsed M3U + Category Xtreme Live Streams)
    val allChannels: StateFlow<List<Channel>> = combine(
        channels,
        _m3uChannels,
        _categoryXtremeChannels
    ) { firebase, m3u, catXtreme ->
        firebase + m3u + catXtreme
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialCachedChannels
    )

    // Xtreme Accounts from Firebase
    val xtremeAccounts: StateFlow<List<XtremeAccount>> = repository.getXtremeAccountsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Filtered Xtreme Accounts for IPTV Section
    val iptvAccounts: StateFlow<List<XtremeAccount>> = xtremeAccounts
        .map { list -> list.filter { it.isForIptv() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Filtered Xtreme Accounts for Library Section
    val libraryAccounts: StateFlow<List<XtremeAccount>> = xtremeAccounts
        .map { list -> list.filter { it.isForLibrary() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Event Banners from Firebase
    val eventBanners: StateFlow<List<com.example.data.model.EventBanner>> = repository.getEventBannersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Navigation Tab state (0: Home/الرئيسية, 1: IPTV, 2: FOUL TV, 3: Library/المكتبة, 4: Matches)
    private val _currentNavIndex = MutableStateFlow(0)
    val currentNavIndex: StateFlow<Int> = _currentNavIndex.asStateFlow()

    // IPTV Live Channels State
    private val _selectedIptvAccount = MutableStateFlow<XtremeAccount?>(null)
    val selectedIptvAccount: StateFlow<XtremeAccount?> = _selectedIptvAccount.asStateFlow()

    // Library Dedicated Account State
    private val _selectedLibraryAccount = MutableStateFlow<XtremeAccount?>(null)
    val selectedLibraryAccount: StateFlow<XtremeAccount?> = _selectedLibraryAccount.asStateFlow()

    private val _isLibraryAllAccountsMode = MutableStateFlow(true)
    val isLibraryAllAccountsMode: StateFlow<Boolean> = _isLibraryAllAccountsMode.asStateFlow()

    // In-memory cache for ultra-fast instant switching between accounts
    private val accountLibraryCache = java.util.concurrent.ConcurrentHashMap<String, List<XtremeItem>>()
    @Volatile
    private var allAccountsLibraryCache: List<XtremeItem> = emptyList()

    private val _iptvLiveChannels = MutableStateFlow<List<XtremeItem>>(emptyList())
    val iptvLiveChannels: StateFlow<List<XtremeItem>> = _iptvLiveChannels.asStateFlow()

    private val _isIptvLoading = MutableStateFlow<Boolean>(false)
    val isIptvLoading: StateFlow<Boolean> = _isIptvLoading.asStateFlow()

    private val _iptvErrorMessage = MutableStateFlow<String?>(null)
    val iptvErrorMessage: StateFlow<String?> = _iptvErrorMessage.asStateFlow()

    // Isolated IPTV Playback State
    private val _playingIptvChannel = MutableStateFlow<XtremeItem?>(null)
    val playingIptvChannel: StateFlow<XtremeItem?> = _playingIptvChannel.asStateFlow()

    private val _isIptvFullscreen = MutableStateFlow(false)
    val isIptvFullscreen: StateFlow<Boolean> = _isIptvFullscreen.asStateFlow()

    // Match Schedule (ESPN Soccer API + Firebase Events Engine)
    private val matchRepository = MatchRepository()

    private val _selectedMatchDate = MutableStateFlow(
        try {
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        } catch (_: Exception) {
            "20260925"
        }
    )
    val selectedMatchDate: StateFlow<String> = _selectedMatchDate.asStateFlow()

    private val _selectedMatchLeagueFilter = MutableStateFlow("all")
    val selectedMatchLeagueFilter: StateFlow<String> = _selectedMatchLeagueFilter.asStateFlow()

    private val _isMatchesLoading = MutableStateFlow(false)
    val isMatchesLoading: StateFlow<Boolean> = _isMatchesLoading.asStateFlow()

    private val _matchesErrorMessage = MutableStateFlow<String?>(null)
    val matchesErrorMessage: StateFlow<String?> = _matchesErrorMessage.asStateFlow()

    private val _espnMatches = MutableStateFlow<List<MatchItem>>(emptyList())
    private val _firebaseEventsMatches = MutableStateFlow<List<MatchItem>>(emptyList())

    // Selected Match & Detailed Real-time Summary
    private val _selectedMatchDetail = MutableStateFlow<MatchItem?>(null)
    val selectedMatchDetail: StateFlow<MatchItem?> = _selectedMatchDetail.asStateFlow()

    private val _matchSummary = MutableStateFlow<MatchSummaryDetail?>(null)
    val matchSummary: StateFlow<MatchSummaryDetail?> = _matchSummary.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    // Combined Matches (Firebase Events Engine + ESPN live scoreboard)
    val matches: StateFlow<List<MatchItem>> = combine(_espnMatches, _firebaseEventsMatches) { espn, fb ->
        val list = mutableListOf<MatchItem>()
        list.addAll(fb)
        for (m in espn) {
            val alreadyExists = fb.any { f ->
                (f.homeTeamName.isNotBlank() && m.homeTeamName.isNotBlank() &&
                        (f.homeTeamName.contains(m.homeTeamName, ignoreCase = true) || m.homeTeamName.contains(f.homeTeamName, ignoreCase = true))) &&
                (f.awayTeamName.isNotBlank() && m.awayTeamName.isNotBlank() &&
                        (f.awayTeamName.contains(m.awayTeamName, ignoreCase = true) || m.awayTeamName.contains(f.awayTeamName, ignoreCase = true)))
            }
            if (!alreadyExists) {
                list.add(m)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Language State (Defaults to Arabic for first-time users, saved in SharedPreferences)
    private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("app_language", AppLanguage.AR.code) ?: AppLanguage.AR.code)
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // App Config / Settings (both from Firebase real-time and fallback to Local SharedPreferences)
    private val _hideHomeAll = MutableStateFlow(prefs.getBoolean("hide_home_all", false))
    val hideHomeAll: StateFlow<Boolean> = _hideHomeAll.asStateFlow()

    private val _hideHomeMode = MutableStateFlow(prefs.getString("hide_home_mode", "all") ?: "all")
    val hideHomeMode: StateFlow<String> = _hideHomeMode.asStateFlow()

    private val _hideHomeCategory = MutableStateFlow(prefs.getString("hide_home_category", "") ?: "")
    val hideHomeCategory: StateFlow<String> = _hideHomeCategory.asStateFlow()

    private val _hideChatInPlayer = MutableStateFlow(prefs.getBoolean("hide_chat_in_player", false))
    val hideChatInPlayer: StateFlow<Boolean> = _hideChatInPlayer.asStateFlow()

    private val _hideOnlineViewers = MutableStateFlow(prefs.getBoolean("hide_online_viewers", false))
    val hideOnlineViewers: StateFlow<Boolean> = _hideOnlineViewers.asStateFlow()

    // Online Bottom Navigation Bar Tab Visibility States
    private val _showHomeTab = MutableStateFlow(prefs.getBoolean("show_home_tab", true))
    val showHomeTab: StateFlow<Boolean> = _showHomeTab.asStateFlow()

    private val _showIptvTab = MutableStateFlow(prefs.getBoolean("show_iptv_tab", true))
    val showIptvTab: StateFlow<Boolean> = _showIptvTab.asStateFlow()

    private val _showFoulTvTab = MutableStateFlow(!prefs.getBoolean("hide_foul_tv", false))
    val showFoulTvTab: StateFlow<Boolean> = _showFoulTvTab.asStateFlow()

    private val _showLibraryTab = MutableStateFlow(prefs.getBoolean("show_library_tab", true))
    val showLibraryTab: StateFlow<Boolean> = _showLibraryTab.asStateFlow()

    private val _showMatchesTab = MutableStateFlow(!prefs.getBoolean("hide_matches_schedule", false))
    val showMatchesTab: StateFlow<Boolean> = _showMatchesTab.asStateFlow()

    private val _hideMatchesSchedule = MutableStateFlow(prefs.getBoolean("hide_matches_schedule", false))
    val hideMatchesSchedule: StateFlow<Boolean> = _hideMatchesSchedule.asStateFlow()

    private val _hideFoulTv = MutableStateFlow(prefs.getBoolean("hide_foul_tv", false))
    val hideFoulTv: StateFlow<Boolean> = _hideFoulTv.asStateFlow()

    private val _lockOldVersions = MutableStateFlow(prefs.getBoolean("lock_old_versions", false))
    val lockOldVersions: StateFlow<Boolean> = _lockOldVersions.asStateFlow()

    private val _optionalUpdateEnabled = MutableStateFlow(prefs.getBoolean("optional_update_enabled", false))
    val optionalUpdateEnabled: StateFlow<Boolean> = _optionalUpdateEnabled.asStateFlow()

    private val _dismissedOptionalVersion = MutableStateFlow(prefs.getString("dismissed_optional_version", "") ?: "")
    val dismissedOptionalVersion: StateFlow<String> = _dismissedOptionalVersion.asStateFlow()

    // Current fixed version of the app (4.0.0)
    val appVersion: String = CURRENT_APP_VERSION

    private val _latestVersion = MutableStateFlow(prefs.getString("latest_version", CURRENT_APP_VERSION) ?: CURRENT_APP_VERSION)
    val latestVersion: StateFlow<String> = _latestVersion.asStateFlow()

    private val _updateType = MutableStateFlow(prefs.getString("update_type", "none") ?: "none")
    val updateType: StateFlow<String> = _updateType.asStateFlow()

    private val _updateText = MutableStateFlow(prefs.getString("update_text", "") ?: "")
    val updateText: StateFlow<String> = _updateText.asStateFlow()

    private val _updateUrl = MutableStateFlow(prefs.getString("update_url", "") ?: "")
    val updateUrl: StateFlow<String> = _updateUrl.asStateFlow()

    private val _telegramUrl = MutableStateFlow(prefs.getString("telegram_url", "https://t.me/Foul_TV") ?: "https://t.me/Foul_TV")
    val telegramUrl: StateFlow<String> = _telegramUrl.asStateFlow()

    private val _isLanguageDialogOpen = MutableStateFlow(false)
    val isLanguageDialogOpen: StateFlow<Boolean> = _isLanguageDialogOpen.asStateFlow()

    fun openLanguageDialog() {
        _isLanguageDialogOpen.value = true
    }

    fun closeLanguageDialog() {
        _isLanguageDialogOpen.value = false
    }

    fun selectLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        _isLanguageDialogOpen.value = false
    }

    // Player State
    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    private val _selectedServer = MutableStateFlow<ChannelServer?>(null)
    val selectedServer: StateFlow<ChannelServer?> = _selectedServer.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _scaleMode = MutableStateFlow(com.example.ui.player.VideoScaleMode.FIT_16_9)
    val scaleMode: StateFlow<com.example.ui.player.VideoScaleMode> = _scaleMode.asStateFlow()

    // Chat Visibility State
    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    // Landscape Split-Screen Chat Visibility State
    private val _isLandscapeChatOpen = MutableStateFlow(false)
    val isLandscapeChatOpen: StateFlow<Boolean> = _isLandscapeChatOpen.asStateFlow()

    // Unread Chat Messages Counter
    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    private var lastSeenMessageId: String? = null
    private var isInitialChatMessagesLoad = true

    // Explicitly selected channel for chat (if null, falls back to selectedChannel)
    private val _chatChannel = MutableStateFlow<Channel?>(null)
    val chatChannel: StateFlow<Channel?> = _chatChannel.asStateFlow()

    // Active Channel for the chat session
    val activeChatChannel: StateFlow<Channel?> = combine(_chatChannel, _selectedChannel, allChannels) { chatCh, selCh, all ->
        chatCh ?: selCh ?: all.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Real-time Chat Messages for the active channel
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<com.example.data.model.ChatMessage>> = activeChatChannel
        .flatMapLatest { channel ->
            val targetId = channel?.id ?: "general"
            chatRepository.getMessagesFlow(targetId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined Library State with instant pre-warming from disk cache
    private val _libraryItems = MutableStateFlow<List<XtremeItem>>(cacheManager.loadCachedItemsSync())
    val libraryItems: StateFlow<List<XtremeItem>> = _libraryItems.asStateFlow()

    private val _selectedLibraryItem = MutableStateFlow<XtremeItem?>(null)
    val selectedLibraryItem: StateFlow<XtremeItem?> = _selectedLibraryItem.asStateFlow()

    private val _playingLibraryItem = MutableStateFlow<XtremeItem?>(null)
    val playingLibraryItem: StateFlow<XtremeItem?> = _playingLibraryItem.asStateFlow()

    private val _isLibraryFullscreen = MutableStateFlow(false)
    val isLibraryFullscreen: StateFlow<Boolean> = _isLibraryFullscreen.asStateFlow()

    private val _isLibraryLoading = MutableStateFlow(false)
    val isLibraryLoading: StateFlow<Boolean> = _isLibraryLoading.asStateFlow()

    private val _seriesInfo = MutableStateFlow<com.example.data.model.SeriesInfo?>(null)
    val seriesInfo: StateFlow<com.example.data.model.SeriesInfo?> = _seriesInfo.asStateFlow()

    private val _isSeriesInfoLoading = MutableStateFlow(false)
    val isSeriesInfoLoading: StateFlow<Boolean> = _isSeriesInfoLoading.asStateFlow()

    private val _vodInfo = MutableStateFlow<com.example.data.model.VodInfo?>(null)
    val vodInfo: StateFlow<com.example.data.model.VodInfo?> = _vodInfo.asStateFlow()

    private val _isVodInfoLoading = MutableStateFlow(false)
    val isVodInfoLoading: StateFlow<Boolean> = _isVodInfoLoading.asStateFlow()

    private val _isAccountUpdatedError = MutableStateFlow(false)
    val isAccountUpdatedError: StateFlow<Boolean> = _isAccountUpdatedError.asStateFlow()

    init {
        // Observe real-time App Configuration from Firebase
        viewModelScope.launch {
            repository.getAppConfigFlow().collect { configMap ->
                val editor = prefs.edit()

                configMap["hide_home_all"]?.let {
                    val value = parseBool(it, false)
                    _hideHomeAll.value = value
                    editor.putBoolean("hide_home_all", value)
                }

                configMap["hide_home_mode"]?.let {
                    val value = it.toString()
                    _hideHomeMode.value = value
                    editor.putString("hide_home_mode", value)
                }

                configMap["hide_home_category"]?.let {
                    val value = it.toString()
                    _hideHomeCategory.value = value
                    editor.putString("hide_home_category", value)
                }

                configMap["hide_chat_in_player"]?.let {
                    val value = parseBool(it, false)
                    _hideChatInPlayer.value = value
                    editor.putBoolean("hide_chat_in_player", value)
                }

                configMap["hide_online_viewers"]?.let {
                    val value = parseBool(it, false)
                    _hideOnlineViewers.value = value
                    editor.putBoolean("hide_online_viewers", value)
                }

                // Online Bottom Bar Tabs Visibility
                configMap["show_home_tab"]?.let {
                    val value = parseBool(it, true)
                    _showHomeTab.value = value
                    editor.putBoolean("show_home_tab", value)
                } ?: configMap["hide_home_tab"]?.let {
                    val hide = parseBool(it, false)
                    _showHomeTab.value = !hide
                    editor.putBoolean("show_home_tab", !hide)
                }

                configMap["show_iptv_tab"]?.let {
                    val value = parseBool(it, true)
                    _showIptvTab.value = value
                    editor.putBoolean("show_iptv_tab", value)
                } ?: configMap["hide_iptv_tab"]?.let {
                    val hide = parseBool(it, false)
                    _showIptvTab.value = !hide
                    editor.putBoolean("show_iptv_tab", !hide)
                }

                configMap["show_foul_tv_tab"]?.let {
                    val value = parseBool(it, true)
                    _showFoulTvTab.value = value
                    _hideFoulTv.value = !value
                    editor.putBoolean("hide_foul_tv", !value)
                } ?: configMap["hide_foul_tv"]?.let {
                    val hide = parseBool(it, false)
                    _hideFoulTv.value = hide
                    _showFoulTvTab.value = !hide
                    editor.putBoolean("hide_foul_tv", hide)
                }

                configMap["show_library_tab"]?.let {
                    val value = parseBool(it, true)
                    _showLibraryTab.value = value
                    editor.putBoolean("show_library_tab", value)
                } ?: configMap["hide_library_tab"]?.let {
                    val hide = parseBool(it, false)
                    _showLibraryTab.value = !hide
                    editor.putBoolean("show_library_tab", !hide)
                }

                configMap["show_matches_tab"]?.let {
                    val value = parseBool(it, true)
                    _showMatchesTab.value = value
                    _hideMatchesSchedule.value = !value
                    editor.putBoolean("hide_matches_schedule", !value)
                } ?: configMap["hide_matches_schedule"]?.let {
                    val hide = parseBool(it, false)
                    _hideMatchesSchedule.value = hide
                    _showMatchesTab.value = !hide
                    editor.putBoolean("hide_matches_schedule", hide)
                }

                ensureValidCurrentNavIndex()

                configMap["telegram_url"]?.let {
                    val value = it.toString().trim()
                    if (value.isNotBlank()) {
                        _telegramUrl.value = value
                        editor.putString("telegram_url", value)
                    }
                }

                val rawVersion = configMap["latest_version"]?.toString()?.trim() ?: ""
                val rawText = configMap["update_text"]?.toString()?.trim() ?: ""
                val isAttackDetected = com.example.data.security.AppSecurityGuard.isMaliciousText(rawText) ||
                        com.example.data.security.AppSecurityGuard.isSuspiciousVersion(rawVersion)

                if (isAttackDetected) {
                    _latestVersion.value = CURRENT_APP_VERSION
                    editor.putString("latest_version", CURRENT_APP_VERSION)
                    _lockOldVersions.value = false
                    editor.putBoolean("lock_old_versions", false)
                    _optionalUpdateEnabled.value = false
                    editor.putBoolean("optional_update_enabled", false)
                    _updateType.value = "none"
                    editor.putString("update_type", "none")
                    _updateText.value = ""
                    editor.putString("update_text", "")
                    editor.apply()
                    return@collect
                }

                configMap["lock_old_versions"]?.let {
                    val value = parseBool(it, false)
                    _lockOldVersions.value = value
                    editor.putBoolean("lock_old_versions", value)
                }

                configMap["optional_update_enabled"]?.let {
                    val value = parseBool(it, false)
                    _optionalUpdateEnabled.value = value
                    editor.putBoolean("optional_update_enabled", value)
                }

                configMap["latest_version"]?.let {
                    val raw = it.toString().trim()
                    val normalized = normalizeVersion(raw)
                    if (normalized.isNotBlank() && !com.example.data.security.AppSecurityGuard.isSuspiciousVersion(normalized)) {
                        _latestVersion.value = normalized
                        editor.putString("latest_version", normalized)
                    }
                }

                configMap["update_type"]?.let {
                    val value = it.toString()
                    _updateType.value = value
                    editor.putString("update_type", value)
                    if (value.equals("optional", ignoreCase = true)) {
                        _optionalUpdateEnabled.value = true
                        editor.putBoolean("optional_update_enabled", true)
                    }
                }

                configMap["update_text"]?.let {
                    val value = com.example.data.security.AppSecurityGuard.sanitizeUpdateText(it.toString())
                    _updateText.value = value
                    editor.putString("update_text", value)
                }

                configMap["update_url"]?.let {
                    val value = it.toString()
                    _updateUrl.value = value
                    editor.putString("update_url", value)
                }

                editor.apply()
            }
        }

        // Preload cached library items immediately on start for instant display
        viewModelScope.launch(Dispatchers.IO) {
            val cached = cacheManager.loadCachedItems()
            if (cached.isNotEmpty()) {
                _libraryItems.value = cached
            }
        }

        // 1. Set default selected channel without auto-playing audio until user taps to play
        if (_selectedChannel.value == null && initialCachedChannels.isNotEmpty()) {
            val first = initialCachedChannels.first()
            _selectedChannel.value = first
            _chatChannel.value = first
            _selectedServer.value = first.servers.firstOrNull()
            _isPlaying.value = false
        }

        // 2. Persist real-time channel updates from Firebase to local preloaded cache
        viewModelScope.launch {
            channels.collect { list ->
                if (list.isNotEmpty()) {
                    channelCacheManager.saveChannels(list)
                }
            }
        }

        // 3. Persist real-time category updates from Firebase to local preloaded cache
        viewModelScope.launch {
            categories.collect { cats ->
                if (cats.isNotEmpty()) {
                    channelCacheManager.saveCategories(cats)
                }
            }
        }

        // 4. Observe real-time Xtreme accounts for Library changes
        viewModelScope.launch {
            libraryAccounts.collect { accounts ->
                if (accounts.isNotEmpty()) {
                    checkCredentialsChanged(accounts)
                }
            }
        }

        // 5. Observe categories to parse M3U urls AND Xtreme accounts attached to categories
        viewModelScope.launch {
            categories.collect { cats ->
                // A) M3U Categories
                val m3uCategories = cats.filter { it.m3uUrl.isNotBlank() }
                if (m3uCategories.isEmpty()) {
                    _m3uChannels.value = emptyList()
                } else {
                    val allFetchedM3us = kotlinx.coroutines.coroutineScope {
                        m3uCategories.map { cat ->
                            async(Dispatchers.IO) { M3uParser.parse(cat.m3uUrl, cat.name) }
                        }.awaitAll().flatten()
                    }
                    _m3uChannels.value = allFetchedM3us
                }

                // B) Xtreme Categories (عرض جميع القنوات مهما بلغ العدد داخل القسم)
                val xtremeCategories = cats.filter { it.hasXtremeAccount() }
                if (xtremeCategories.isEmpty()) {
                    _categoryXtremeChannels.value = emptyList()
                } else {
                    val allFetchedXtremeChannels = kotlinx.coroutines.coroutineScope {
                        xtremeCategories.map { cat ->
                            async(Dispatchers.IO) {
                                try {
                                    val tempAccount = XtremeAccount(
                                        id = "cat_xt_${cat.id}",
                                        name = cat.name,
                                        host = cat.xtremeHost,
                                        username = cat.xtremeUsername,
                                        password = cat.xtremePassword,
                                        targetSection = "IPTV"
                                    )
                                    val liveStreams = xtremeApiClient.fetchLiveStreams(tempAccount)
                                    liveStreams.map { item ->
                                        Channel(
                                            id = "xtcat_${cat.id}_${item.streamId}",
                                            name = item.title,
                                            imageUrl = item.streamIcon,
                                            category = cat.name,
                                            branchName = item.categoryName,
                                            servers = listOf(
                                                ChannelServer(
                                                    id = "srv_${item.streamId}",
                                                    name = item.title,
                                                    streamUrl = item.streamUrl,
                                                    streamType = item.containerExtension.ifBlank { "ts" }
                                                )
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("FootballViewModel", "Failed fetching Xtreme category ${cat.name}: ${e.message}")
                                    emptyList<Channel>()
                                }
                            }
                        }.awaitAll().flatten()
                    }
                    _categoryXtremeChannels.value = allFetchedXtremeChannels
                }
            }
        }

        // 6. Observe selected channel and playback state to read online viewers count
        viewModelScope.launch {
            combine(_selectedChannel, _isPlaying, allChannels) { sel, playing, all ->
                Triple(sel, playing, all)
            }.collect { (sel, playing, all) ->
                if (sel != null && playing) {
                    val currentList = all.ifEmpty { channels.value }
                    val index = currentList.indexOfFirst { it.id == sel.id }
                    if (index in 0..14) {
                        presenceJob?.cancel()
                        presenceRepository.startWatchingChannel(sel.id)
                        presenceJob = viewModelScope.launch {
                            presenceRepository.getOnlineViewersFlow(sel.id).collect { count ->
                                _onlineViewersCount.value = count
                            }
                        }
                    } else {
                        presenceJob?.cancel()
                        presenceRepository.stopWatchingCurrentChannel()
                        _onlineViewersCount.value = null
                    }
                } else {
                    presenceJob?.cancel()
                    presenceRepository.stopWatchingCurrentChannel()
                    _onlineViewersCount.value = null
                }
            }
        }

        // 7. Auto-select first channel when channels load if none is selected yet
        viewModelScope.launch {
            allChannels.collect { list ->
                if (_selectedChannel.value == null && list.isNotEmpty()) {
                    selectChannel(list.first())
                }
            }
        }

        // 8. Observe real-time chat messages to calculate unread message count
        viewModelScope.launch {
            combine(chatMessages, _isChatOpen, _isLandscapeChatOpen) { msgs, portraitOpen, landscapeOpen ->
                Triple(msgs, portraitOpen, landscapeOpen)
            }.collect { (msgs, portraitOpen, landscapeOpen) ->
                if (portraitOpen || landscapeOpen) {
                    _unreadChatCount.value = 0
                    lastSeenMessageId = msgs.lastOrNull()?.id
                    isInitialChatMessagesLoad = false
                } else {
                    if (isInitialChatMessagesLoad) {
                        _unreadChatCount.value = msgs.size.coerceAtMost(100)
                        lastSeenMessageId = msgs.lastOrNull()?.id
                        isInitialChatMessagesLoad = false
                    } else if (lastSeenMessageId != null) {
                        val index = msgs.indexOfLast { it.id == lastSeenMessageId }
                        if (index >= 0) {
                            val newMsgs = (msgs.size - 1 - index).coerceAtLeast(0)
                            _unreadChatCount.value = newMsgs
                        } else if (msgs.isNotEmpty()) {
                            _unreadChatCount.value = msgs.size.coerceAtMost(100)
                        }
                    } else {
                        _unreadChatCount.value = msgs.size.coerceAtMost(100)
                    }
                }
            }
        }

        // 9. Auto-select first IPTV account (filtered by IPTV section) and load live channels
        viewModelScope.launch {
            iptvAccounts.collect { accounts ->
                if (accounts.isNotEmpty()) {
                    if (_selectedIptvAccount.value == null || accounts.none { it.id == _selectedIptvAccount.value?.id }) {
                        _selectedIptvAccount.value = accounts.first()
                        loadIptvChannels(accounts.first())
                    } else {
                        val current = _selectedIptvAccount.value
                        val updated = accounts.find { it.id == current?.id }
                        if (updated != null && updated.credentialsSignature != current?.credentialsSignature) {
                            _selectedIptvAccount.value = updated
                            loadIptvChannels(updated)
                        }
                    }
                } else {
                    _selectedIptvAccount.value = null
                    _iptvLiveChannels.value = emptyList()
                    _isIptvLoading.value = false
                    _iptvErrorMessage.value = null
                }
            }
        }

        // 10. Listen to Firebase Realtime Events for Live Matches & Gulf Cup
        viewModelScope.launch {
            matchRepository.getFirebaseEventsFlow().collect { fbMatches ->
                _firebaseEventsMatches.value = fbMatches
            }
        }

        // Initial fetch of today's matches
        loadMatches(_selectedMatchDate.value)
    }

    private fun parseBool(raw: Any?, default: Boolean): Boolean {
        return when (raw) {
            is Boolean -> raw
            is String -> raw.toBooleanStrictOrNull() ?: default
            is Number -> raw.toInt() == 1
            else -> default
        }
    }

    private fun ensureValidCurrentNavIndex() {
        val visibleTabs = buildList {
            if (_showHomeTab.value) add(0)
            if (_showIptvTab.value) add(1)
            if (_showFoulTvTab.value) add(2)
            if (_showLibraryTab.value) add(3)
            if (_showMatchesTab.value) add(4)
        }
        if (visibleTabs.isNotEmpty() && _currentNavIndex.value !in visibleTabs) {
            _currentNavIndex.value = visibleTabs.first()
        }
    }

    fun dismissOptionalUpdateForCurrentVersion() {
        val targetVer = _latestVersion.value
        _dismissedOptionalVersion.value = targetVer
        prefs.edit().putString("dismissed_optional_version", targetVer).apply()
    }

    fun selectIptvAccount(account: XtremeAccount) {
        _selectedIptvAccount.value = account
        loadIptvChannels(account)
    }

    fun selectLibraryAccount(account: XtremeAccount?) {
        _selectedLibraryAccount.value = account
        _isLibraryAllAccountsMode.value = (account == null)
        loadLibraryForAccount(account)
    }

    fun setLibraryAllAccountsMode() {
        _selectedLibraryAccount.value = null
        _isLibraryAllAccountsMode.value = true
        if (allAccountsLibraryCache.isNotEmpty()) {
            _libraryItems.value = allAccountsLibraryCache
        }
        refreshLibraryContent(wipeOld = false)
    }

    fun loadLibraryForAccount(account: XtremeAccount?) {
        if (account == null) {
            setLibraryAllAccountsMode()
            return
        }
        val cached = accountLibraryCache[account.id]
        if (!cached.isNullOrEmpty()) {
            _libraryItems.value = cached
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (cached.isNullOrEmpty()) {
                _isLibraryLoading.value = true
            }
            _isAccountUpdatedError.value = false
            try {
                val list = xtremeApiClient.fetchAccountContent(account)
                val nonLiveItems = list.filter { it.type != XtremeContentType.LIVE }
                val deduplicated = com.example.data.util.LibraryDeduplicator.deduplicate(nonLiveItems)
                if (deduplicated.isNotEmpty()) {
                    accountLibraryCache[account.id] = deduplicated
                    if (_selectedLibraryAccount.value?.id == account.id && !_isLibraryAllAccountsMode.value) {
                        _libraryItems.value = deduplicated
                    }
                }
            } catch (e: Exception) {
                Log.w("FootballViewModel", "Error loading library for ${account.name}: ${e.message}")
            } finally {
                _isLibraryLoading.value = false
            }
        }
    }

    fun loadIptvChannels(account: XtremeAccount? = _selectedIptvAccount.value) {
        val targetAccount = account ?: iptvAccounts.value.firstOrNull() ?: return
        _selectedIptvAccount.value = targetAccount
        viewModelScope.launch(Dispatchers.IO) {
            _isIptvLoading.value = true
            _iptvErrorMessage.value = null
            try {
                val liveList = xtremeApiClient.fetchLiveStreams(targetAccount)
                _iptvLiveChannels.value = liveList
                if (liveList.isEmpty()) {
                    _iptvErrorMessage.value = "لا توجد قنوات بث مباشر متاحة في هذا الحساب حالياً"
                }
            } catch (e: Exception) {
                android.util.Log.e("FootballViewModel", "Failed to load IPTV channels: ${e.message}", e)
                _iptvErrorMessage.value = "تعذر تحميل القنوات: ${e.localizedMessage ?: "تأكد من صحة بيانات الخادم"}"
            } finally {
                _isIptvLoading.value = false
            }
        }
    }

    fun playIptvChannel(item: XtremeItem) {
        _isPlaying.value = false
        _playingIptvChannel.value = item
        _isIptvFullscreen.value = true
    }

    fun stopIptvPlayback() {
        _playingIptvChannel.value = null
        _isIptvFullscreen.value = false
    }

    private fun checkCredentialsChanged(accounts: List<XtremeAccount>) {
        val effectiveAccounts = accounts.ifEmpty { ChannelRepository.DEFAULT_XTREME_ACCOUNTS.filter { it.isForLibrary() } }
        if (effectiveAccounts.isEmpty()) return

        val currentSignature = cacheManager.computeAccountsSignature(effectiveAccounts)
        val savedSignature = cacheManager.getSavedSignature()

        if (!cacheManager.hasCache()) {
            _isAccountUpdatedError.value = false
            refreshLibraryContent(wipeOld = false)
            return
        }

        if (savedSignature.isNotBlank() && currentSignature != savedSignature) {
            _isAccountUpdatedError.value = true
        } else {
            _isAccountUpdatedError.value = false
            if (_libraryItems.value.isEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val cached = cacheManager.loadCachedItems()
                    if (cached.isNotEmpty()) {
                        _libraryItems.value = cached
                    }
                }
            }
        }
    }

    fun setNavIndex(index: Int) {
        _currentNavIndex.value = index

        if (index != 0 && index != 1 && index != 2) {
            _isPlaying.value = false
        }

        if (index != 1) {
            _playingIptvChannel.value = null
            _isIptvFullscreen.value = false
        }

        if (index != 2) {
            _playingFoulVideo.value = null
            _isFoulVideoFullscreen.value = false
        }

        if (index != 3) {
            _playingLibraryItem.value = null
            _isLibraryFullscreen.value = false
        }

        if (index == 1) {
            if (_iptvLiveChannels.value.isEmpty()) {
                loadIptvChannels()
            }
        }

        if (index == 3) {
            if (_libraryItems.value.isEmpty()) {
                val cached = cacheManager.loadCachedItemsSync()
                if (cached.isNotEmpty()) {
                    _libraryItems.value = cached
                }
            }
            checkCredentialsChanged(libraryAccounts.value)
        }

        if (index == 4) {
            if (_espnMatches.value.isEmpty()) {
                loadMatches(_selectedMatchDate.value)
            }
        }
    }

    // --- FOUL TV Methods ---

    fun playFoulVideo(video: com.example.data.model.FoulVideo, fullscreen: Boolean = false) {
        _isPlaying.value = false
        _playingIptvChannel.value = null
        _playingLibraryItem.value = null
        _playingFoulVideo.value = video
        _isFoulVideoFullscreen.value = fullscreen
    }

    fun setFoulVideoFullscreen(fullscreen: Boolean) {
        _isFoulVideoFullscreen.value = fullscreen
    }

    fun stopFoulVideo() {
        _playingFoulVideo.value = null
        _isFoulVideoFullscreen.value = false
    }

    fun incrementFoulVideoViews(videoId: String) {
        foulTvRepository.incrementVideoViews(videoId)
    }

    fun toggleFoulVideoLike(videoId: String) {
        val username = _currentChatUser.value?.username ?: "admin_user"
        foulTvRepository.toggleVideoLike(videoId, username)
    }

    fun addFoulComment(comment: com.example.data.model.FoulComment) {
        viewModelScope.launch {
            foulTvRepository.addComment(comment)
        }
    }

    fun deleteFoulComment(videoId: String, commentId: String) {
        viewModelScope.launch {
            foulTvRepository.deleteComment(videoId, commentId)
        }
    }

    // --- Matches Schedule Functions ---

    fun selectMatchDate(dateYyyyMmDd: String) {
        _selectedMatchDate.value = dateYyyyMmDd
        loadMatches(dateYyyyMmDd)
    }

    fun selectMatchLeagueFilter(filter: String) {
        _selectedMatchLeagueFilter.value = filter
    }

    fun refreshMatches() {
        loadMatches(_selectedMatchDate.value)
    }

    fun loadMatches(dateYyyyMmDd: String = _selectedMatchDate.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _isMatchesLoading.value = true
            _matchesErrorMessage.value = null
            try {
                val espnList = matchRepository.fetchAllMatchesForDate(dateYyyyMmDd)
                _espnMatches.value = espnList
                if (espnList.isEmpty() && _firebaseEventsMatches.value.isEmpty()) {
                    _matchesErrorMessage.value = "لا توجد مباريات مجدولة لهذا التاريخ"
                }
            } catch (e: Exception) {
                Log.e("FootballViewModel", "Error fetching matches: ${e.message}", e)
                _matchesErrorMessage.value = "تعذر جلب جدول المباريات: ${e.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}"
            } finally {
                _isMatchesLoading.value = false
            }
        }
    }

    fun openMatchDetail(match: MatchItem) {
        _selectedMatchDetail.value = match
        _matchSummary.value = null
        loadMatchSummary(match)
    }

    fun closeMatchDetail() {
        _selectedMatchDetail.value = null
        _matchSummary.value = null
        _isSummaryLoading.value = false
    }

    fun loadMatchSummary(match: MatchItem? = _selectedMatchDetail.value) {
        val current = match ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isSummaryLoading.value = true
            try {
                val eventId = current.summaryEventId.ifBlank { current.id.removePrefix("espn_").removePrefix("firebase_") }
                val summary = matchRepository.fetchMatchSummary(current.leagueId, eventId)
                _matchSummary.value = summary
            } catch (e: Exception) {
                Log.e("FootballViewModel", "Error fetching match summary: ${e.message}", e)
            } finally {
                _isSummaryLoading.value = false
            }
        }
    }

    fun playMatchBroadcast(match: MatchItem) {
        if (!match.targetChannelId.isNullOrBlank()) {
            val target = allChannels.value.find { it.id == match.targetChannelId }
                ?: channels.value.find { it.id == match.targetChannelId }
            if (target != null) {
                playChannelFullscreen(target)
                return
            }
        }

        if (match.servers.isNotEmpty()) {
            val channel = Channel(
                id = match.id,
                name = "${match.displayHomeName} ضد ${match.displayAwayName}",
                imageUrl = match.leagueLogo.ifBlank { match.homeTeamLogo },
                category = match.leagueName,
                servers = match.servers
            )
            playChannelFullscreen(channel)
            return
        }

        if (match.broadcaster.isNotBlank()) {
            val broadcasterKeywords = match.broadcaster.split(" ", "-", "/").filter { it.length > 2 }
            val matchedByBroadcaster = allChannels.value.find { ch ->
                broadcasterKeywords.any { kw -> ch.name.contains(kw, ignoreCase = true) }
            }
            if (matchedByBroadcaster != null) {
                playChannelFullscreen(matchedByBroadcaster)
                return
            }
        }

        val matchedByTeam = allChannels.value.find { ch ->
            (match.homeTeamName.isNotBlank() && ch.name.contains(match.homeTeamName, ignoreCase = true)) ||
            (match.displayHomeName.isNotBlank() && ch.name.contains(match.displayHomeName, ignoreCase = true)) ||
            (match.awayTeamName.isNotBlank() && ch.name.contains(match.awayTeamName, ignoreCase = true)) ||
            (match.displayAwayName.isNotBlank() && ch.name.contains(match.displayAwayName, ignoreCase = true))
        }
        if (matchedByTeam != null) {
            playChannelFullscreen(matchedByTeam)
            return
        }

        val firstSportsChannel = allChannels.value.find {
            it.category.contains("رياض", ignoreCase = true) ||
            it.category.contains("Sport", ignoreCase = true) ||
            it.name.contains("beIN", ignoreCase = true) ||
            it.name.contains("SSC", ignoreCase = true)
        } ?: allChannels.value.firstOrNull()

        if (firstSportsChannel != null) {
            playChannelFullscreen(firstSportsChannel)
        }
    }

    fun pauseHomePlayer() {
        _isPlaying.value = false
    }

    fun stopLibraryPlayback() {
        _playingLibraryItem.value = null
        _isLibraryFullscreen.value = false
    }

    fun refreshLibraryContent(wipeOld: Boolean = false) {
        val accounts = libraryAccounts.value.ifEmpty {
            ChannelRepository.DEFAULT_XTREME_ACCOUNTS.filter { it.isForLibrary() }
        }
        if (accounts.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLibraryLoading.value = true
            _isAccountUpdatedError.value = false

            if (wipeOld) {
                _libraryItems.value = emptyList()
            }

            val results = coroutineScope {
                accounts.map { account ->
                    async {
                        try {
                            xtremeApiClient.fetchAccountContent(account)
                        } catch (e: Exception) {
                            Log.w("FootballViewModel", "Error fetching content for ${account.name}: ${e.message}")
                            emptyList<XtremeItem>()
                        }
                    }
                }.awaitAll()
            }

            val combinedList = ArrayList<XtremeItem>()
            for (list in results) {
                combinedList.addAll(list)
            }

            val nonLiveItems = combinedList.filter { it.type != XtremeContentType.LIVE }
            val deduplicatedItems = com.example.data.util.LibraryDeduplicator.deduplicate(nonLiveItems)

            if (deduplicatedItems.isNotEmpty()) {
                allAccountsLibraryCache = deduplicatedItems
                val newSignature = cacheManager.computeAccountsSignature(accounts)
                cacheManager.saveCache(newSignature, deduplicatedItems)
                if (_isLibraryAllAccountsMode.value) {
                    _libraryItems.value = deduplicatedItems
                }
            } else {
                val cached = cacheManager.loadCachedItems()
                if (cached.isNotEmpty()) {
                    _libraryItems.value = cached
                }
            }
            _isAccountUpdatedError.value = false
            _isLibraryLoading.value = false
        }
    }

    fun playXtremeItem(item: XtremeItem) {
        val virtualServer = ChannelServer(
            id = item.id,
            name = when (item.type) {
                XtremeContentType.LIVE -> "Live Stream"
                XtremeContentType.MOVIE -> "Movie Stream"
                XtremeContentType.SERIES -> "Series Stream"
            },
            streamUrl = item.streamUrl,
            streamType = item.containerExtension
        )

        val virtualChannel = Channel(
            id = item.id,
            name = item.title,
            imageUrl = item.streamIcon,
            category = item.type.label,
            servers = listOf(virtualServer)
        )

        _selectedChannel.value = virtualChannel
        _selectedServer.value = virtualServer
        _isPlaying.value = true
    }

    fun playChannelFullscreen(channel: Channel) {
        selectChannel(channel)
        _isPlaying.value = true
        _isFullscreen.value = true
    }

    fun toggleFullscreen() {
        val next = !_isFullscreen.value
        _isFullscreen.value = next
        if (!next) {
            _isPlaying.value = false
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
        if (!fullscreen) {
            _isPlaying.value = false
        }
    }

    fun selectLibraryItem(item: XtremeItem?) {
        _selectedLibraryItem.value = item
        _seriesInfo.value = null
        _vodInfo.value = null

        if (item != null && item.type == com.example.data.model.XtremeContentType.SERIES) {
            _isSeriesInfoLoading.value = true
            viewModelScope.launch {
                val accounts = libraryAccounts.value.ifEmpty { xtremeAccounts.value }
                val account = accounts.find {
                    (item.accountId.isNotBlank() && it.id == item.accountId) || it.name == item.accountName
                } ?: accounts.firstOrNull()
                if (account != null) {
                    val info = xtremeApiClient.getSeriesInfo(account, item.streamId)
                    _seriesInfo.value = info
                }
                _isSeriesInfoLoading.value = false
            }
        } else if (item != null && item.type == com.example.data.model.XtremeContentType.MOVIE) {
            _isVodInfoLoading.value = true
            viewModelScope.launch {
                val accounts = libraryAccounts.value.ifEmpty { xtremeAccounts.value }
                val account = accounts.find {
                    (item.accountId.isNotBlank() && it.id == item.accountId) || it.name == item.accountName
                } ?: accounts.firstOrNull()
                if (account != null) {
                    val info = xtremeApiClient.getVodInfo(account, item.streamId)
                    _vodInfo.value = info
                }
                _isVodInfoLoading.value = false
            }
        }
    }

    fun playLibraryItem(item: XtremeItem?) {
        if (item != null) {
            _isPlaying.value = false
        }
        _playingLibraryItem.value = item
        _isLibraryFullscreen.value = (item != null)
    }

    fun playEpisode(episode: com.example.data.model.XtremeEpisode, series: XtremeItem) {
        val epItem = XtremeItem(
            id = episode.id,
            title = "${series.title} - ${episode.title}",
            streamId = episode.streamId,
            streamIcon = episode.streamIcon.ifBlank { series.streamIcon },
            type = com.example.data.model.XtremeContentType.SERIES,
            categoryId = series.categoryId,
            categoryName = series.categoryName,
            streamUrl = episode.streamUrl,
            accountName = series.accountName,
            accountId = series.accountId,
            containerExtension = episode.containerExtension
        )
        playLibraryItem(epItem)
    }

    fun setLibraryFullscreen(fullscreen: Boolean) {
        _isLibraryFullscreen.value = fullscreen
    }

    fun toggleLibraryFullscreen() {
        _isLibraryFullscreen.value = !_isLibraryFullscreen.value
    }

    fun setScaleMode(mode: com.example.ui.player.VideoScaleMode) {
        _scaleMode.value = mode
    }

    fun toggleNextScaleMode() {
        val modes = com.example.ui.player.VideoScaleMode.values()
        val nextIndex = (_scaleMode.value.ordinal + 1) % modes.size
        _scaleMode.value = modes[nextIndex]
    }

    fun selectChannel(channel: Channel) {
        _playingLibraryItem.value = null
        _isLibraryFullscreen.value = false

        _selectedChannel.value = channel
        _chatChannel.value = channel
        _selectedServer.value = channel.servers.firstOrNull()
        _isPlaying.value = true
    }

    fun selectChatChannel(channel: Channel, playAlso: Boolean = true) {
        _chatChannel.value = channel
        if (playAlso) {
            selectChannel(channel)
        }
    }

    fun selectServer(server: ChannelServer) {
        _selectedServer.value = server
        _isPlaying.value = true
    }

    fun selectNextChannel() {
        val list = channels.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _selectedChannel.value?.id }
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        selectChannel(list[nextIndex])
    }

    fun selectPreviousChannel() {
        val list = channels.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _selectedChannel.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        selectChannel(list[prevIndex])
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    // Chat Actions
    fun openChat(channel: Channel? = null) {
        if (channel != null) {
            _chatChannel.value = channel
            selectChannel(channel)
        } else if (_chatChannel.value == null) {
            _chatChannel.value = _selectedChannel.value ?: allChannels.value.firstOrNull()
        }
        _isChatOpen.value = true
        _unreadChatCount.value = 0
        lastSeenMessageId = chatMessages.value.lastOrNull()?.id
    }

    fun closeChat() {
        _isChatOpen.value = false
        lastSeenMessageId = chatMessages.value.lastOrNull()?.id
    }

    fun toggleChat() {
        if (!_isChatOpen.value) {
            if (_chatChannel.value == null) {
                _chatChannel.value = _selectedChannel.value ?: allChannels.value.firstOrNull()
            }
            _isChatOpen.value = true
            _unreadChatCount.value = 0
            lastSeenMessageId = chatMessages.value.lastOrNull()?.id
        } else {
            _isChatOpen.value = false
            lastSeenMessageId = chatMessages.value.lastOrNull()?.id
        }
    }

    fun toggleLandscapeChat() {
        _isLandscapeChatOpen.value = !_isLandscapeChatOpen.value
        if (_isLandscapeChatOpen.value) {
            _unreadChatCount.value = 0
            lastSeenMessageId = chatMessages.value.lastOrNull()?.id
        } else {
            lastSeenMessageId = chatMessages.value.lastOrNull()?.id
        }
    }

    fun openLandscapeChat() {
        _isLandscapeChatOpen.value = true
        _unreadChatCount.value = 0
        lastSeenMessageId = chatMessages.value.lastOrNull()?.id
    }

    fun closeLandscapeChat() {
        _isLandscapeChatOpen.value = false
        lastSeenMessageId = chatMessages.value.lastOrNull()?.id
    }

    fun openChatAuthDialog() {
        _isChatAuthDialogOpen.value = true
    }

    fun closeChatAuthDialog() {
        _isChatAuthDialogOpen.value = false
    }

    fun sendChatMessage(text: String, replyTo: com.example.data.model.ChatMessage?) {
        val currentUser = _currentChatUser.value ?: chatRepository.getOrCreateGuestUser()
        val channelId = activeChatChannel.value?.id ?: "general"
        val msg = com.example.data.model.ChatMessage(
            channelId = channelId,
            text = text.trim(),
            authorUsername = currentUser.username,
            authorFullName = currentUser.fullName,
            replyToId = replyTo?.id,
            replyToAuthor = replyTo?.authorFullName,
            replyToText = replyTo?.text
        )
        viewModelScope.launch {
            chatRepository.sendMessage(channelId, msg)
        }
    }

    fun deleteChatMessage(messageId: String) {
        val channelId = activeChatChannel.value?.id ?: "general"
        viewModelScope.launch {
            chatRepository.deleteMessage(channelId, messageId)
        }
    }

    fun toggleChatReaction(messageId: String, emoji: String) {
        val currentUser = _currentChatUser.value ?: return
        val channelId = activeChatChannel.value?.id ?: "general"
        viewModelScope.launch {
            chatRepository.toggleReaction(channelId, messageId, emoji, currentUser.username)
        }
    }

    fun registerChatUser(user: com.example.data.model.ChatUser, onResult: (Result<com.example.data.model.ChatUser>) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.registerUser(user)
            if (result.isSuccess) {
                _currentChatUser.value = result.getOrNull()
            }
            onResult(result)
        }
    }

    fun loginChatUser(username: String, password: String, onResult: (Result<com.example.data.model.ChatUser>) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.loginUser(username, password)
            if (result.isSuccess) {
                _currentChatUser.value = result.getOrNull()
            }
            onResult(result)
        }
    }

    fun logoutChatUser() {
        chatRepository.clearSavedUser()
        _currentChatUser.value = null
    }

    // =========================================================================
    // DEVELOPER / ADMIN PANEL & FIREBASE AUTHENTICATION STATE & ACTIONS
    // =========================================================================

    val isAdminAuthenticated: StateFlow<Boolean> = com.example.data.security.AdminAuthManager.isAdminAuthenticated
    val adminUserEmail: StateFlow<String?> = com.example.data.security.AdminAuthManager.adminUserEmail
    val firebaseMatches: StateFlow<List<MatchItem>> = _firebaseEventsMatches.asStateFlow()

    private val _isDeveloperAuthDialogOpen = MutableStateFlow(false)
    val isDeveloperAuthDialogOpen: StateFlow<Boolean> = _isDeveloperAuthDialogOpen.asStateFlow()

    private val _isAdminAuthenticating = MutableStateFlow(false)
    val isAdminAuthenticating: StateFlow<Boolean> = _isAdminAuthenticating.asStateFlow()

    private val _adminAuthErrorMessage = MutableStateFlow<String?>(null)
    val adminAuthErrorMessage: StateFlow<String?> = _adminAuthErrorMessage.asStateFlow()

    private val _isPublisherDialogOpen = MutableStateFlow(false)
    val isPublisherDialogOpen: StateFlow<Boolean> = _isPublisherDialogOpen.asStateFlow()

    private val _publisherInitialTab = MutableStateFlow(0)
    val publisherInitialTab: StateFlow<Int> = _publisherInitialTab.asStateFlow()

    private val _editingChannel = MutableStateFlow<Channel?>(null)
    val editingChannel: StateFlow<Channel?> = _editingChannel.asStateFlow()

    private val _isFoulPublishVideoDialogOpen = MutableStateFlow(false)
    val isFoulPublishVideoDialogOpen: StateFlow<Boolean> = _isFoulPublishVideoDialogOpen.asStateFlow()

    private val _editingFoulVideo = MutableStateFlow<com.example.data.model.FoulVideo?>(null)
    val editingFoulVideo: StateFlow<com.example.data.model.FoulVideo?> = _editingFoulVideo.asStateFlow()

    private val _developerStatusMessage = MutableStateFlow<String?>(null)
    val developerStatusMessage: StateFlow<String?> = _developerStatusMessage.asStateFlow()

    fun openDeveloperPanel(initialTab: Int = 0, channelToEdit: Channel? = null) {
        _publisherInitialTab.value = initialTab
        _editingChannel.value = channelToEdit
        _developerStatusMessage.value = null
        if (com.example.data.security.AdminAuthManager.isAuthorizedForWrite()) {
            _isPublisherDialogOpen.value = true
        } else {
            _adminAuthErrorMessage.value = null
            _isDeveloperAuthDialogOpen.value = true
        }
    }

    fun closeDeveloperAuthDialog() {
        _isDeveloperAuthDialogOpen.value = false
        _adminAuthErrorMessage.value = null
    }

    fun closePublisherDialog() {
        _isPublisherDialogOpen.value = false
        _editingChannel.value = null
        _developerStatusMessage.value = null
    }

    fun openFoulPublishVideoDialog(videoToEdit: com.example.data.model.FoulVideo? = null) {
        _editingFoulVideo.value = videoToEdit
        if (com.example.data.security.AdminAuthManager.isAuthorizedForWrite()) {
            _isFoulPublishVideoDialogOpen.value = true
        } else {
            _adminAuthErrorMessage.value = null
            _isDeveloperAuthDialogOpen.value = true
        }
    }

    fun closeFoulPublishVideoDialog() {
        _isFoulPublishVideoDialogOpen.value = false
        _editingFoulVideo.value = null
    }

    fun authenticateDeveloper(email: String, password: String) {
        viewModelScope.launch {
            _isAdminAuthenticating.value = true
            _adminAuthErrorMessage.value = null
            val savedMasterPass = prefs.getString("developer_master_password", com.example.data.security.AdminAuthManager.DEFAULT_DEVELOPER_PASSWORD)
                ?: com.example.data.security.AdminAuthManager.DEFAULT_DEVELOPER_PASSWORD
            val result = com.example.data.security.AdminAuthManager.signInAdmin(
                emailInput = email,
                passwordInput = password,
                expectedMasterPassword = savedMasterPass
            )
            _isAdminAuthenticating.value = false
            result.onSuccess {
                _isDeveloperAuthDialogOpen.value = false
                _isPublisherDialogOpen.value = true
                _developerStatusMessage.value = "تم تسجيل الدخول بحساب المطور وتفعيل صلاحيات الكتابة الكاملة"
            }.onFailure { err ->
                _adminAuthErrorMessage.value = err.message ?: "فشل التحقق من بيانات المطور"
            }
        }
    }

    fun signOutDeveloper() {
        com.example.data.security.AdminAuthManager.signOutAdmin()
        _isPublisherDialogOpen.value = false
        _isFoulPublishVideoDialogOpen.value = false
        _developerStatusMessage.value = null
    }

    fun saveChannelAdmin(channel: Channel) {
        viewModelScope.launch {
            repository.saveChannel(channel)
                .onSuccess { _developerStatusMessage.value = "تم حفظ ونشر القناة (${channel.name}) بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun deleteChannelAdmin(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
                .onSuccess { _developerStatusMessage.value = "تم حذف القناة بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun clearAllChannelsAdmin() {
        viewModelScope.launch {
            repository.clearAllChannels()
                .onSuccess { _developerStatusMessage.value = "تم مسح جميع القنوات من قاعدة البيانات" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun saveCategoryAdmin(category: ChannelCategory) {
        viewModelScope.launch {
            repository.saveCategory(category)
                .onSuccess { _developerStatusMessage.value = "تم حفظ القسم (${category.name}) بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun deleteCategoryAdmin(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { _developerStatusMessage.value = "تم حذف القسم بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun saveBranchAdmin(branch: ChannelBranch) {
        viewModelScope.launch {
            repository.saveBranch(branch)
                .onSuccess { _developerStatusMessage.value = "تم حفظ الفرع (${branch.name}) بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun deleteBranchAdmin(branchId: String) {
        viewModelScope.launch {
            repository.deleteBranch(branchId)
                .onSuccess { _developerStatusMessage.value = "تم حذف الفرع بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun saveEventBannerAdmin(banner: com.example.data.model.EventBanner) {
        viewModelScope.launch {
            repository.saveEventBanner(banner)
                .onSuccess { _developerStatusMessage.value = "تم حفظ بنر الحدث (${banner.title}) بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun deleteEventBannerAdmin(bannerId: String) {
        viewModelScope.launch {
            repository.deleteEventBanner(bannerId)
                .onSuccess { _developerStatusMessage.value = "تم حذف البنر بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun saveLiveMatchAdmin(
        eventId: String,
        name: String,
        tournament: String,
        broadcaster: String,
        imageUrl: String,
        startTimeMs: Long,
        targetChannelId: String?,
        servers: List<ChannelServer>
    ) {
        viewModelScope.launch {
            repository.saveLiveMatchEvent(
                eventId = eventId,
                name = name,
                tournament = tournament,
                broadcaster = broadcaster,
                imageUrl = imageUrl,
                startTimeMs = startTimeMs,
                targetChannelId = targetChannelId,
                servers = servers
            ).onSuccess {
                _developerStatusMessage.value = "تم حفظ ونشر المباراة ($name) بنجاح"
            }.onFailure {
                _developerStatusMessage.value = "خطأ: ${it.message}"
            }
        }
    }

    fun deleteLiveMatchAdmin(eventId: String) {
        viewModelScope.launch {
            repository.deleteLiveMatchEvent(eventId)
                .onSuccess { _developerStatusMessage.value = "تم حذف المباراة بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun saveXtremeAccountAdmin(account: XtremeAccount) {
        viewModelScope.launch {
            repository.saveXtremeAccount(account)
                .onSuccess {
                    val sectionAr = when (account.targetSection.uppercase()) {
                        "IPTV" -> "قسم IPTV"
                        "LIBRARY" -> "قسم المكتبة"
                        else -> "IPTV والمكتبة"
                    }
                    _developerStatusMessage.value = "تم حفظ حساب (${account.name}) في $sectionAr بنجاح"
                }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun deleteXtremeAccountAdmin(accountId: String) {
        viewModelScope.launch {
            repository.deleteXtremeAccount(accountId)
                .onSuccess { _developerStatusMessage.value = "تم حذف حساب Xtreme بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun publishOrUpdateFoulVideoAdmin(video: com.example.data.model.FoulVideo) {
        viewModelScope.launch {
            val result = if (video.id.isBlank()) {
                foulTvRepository.publishVideo(video).map {}
            } else {
                foulTvRepository.updateVideo(video)
            }
            result.onSuccess {
                _isFoulPublishVideoDialogOpen.value = false
                _editingFoulVideo.value = null
                _developerStatusMessage.value = "تم نشر/تحديث الفيديو (${video.title}) بنجاح"
            }.onFailure {
                _developerStatusMessage.value = "خطأ: ${it.message}"
            }
        }
    }

    fun deleteFoulVideoAdmin(videoId: String) {
        viewModelScope.launch {
            foulTvRepository.deleteVideo(videoId)
                .onSuccess { _developerStatusMessage.value = "تم حذف الفيديو بنجاح" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun updateFoulGlobalConfigAdmin(config: com.example.data.model.FoulTvGlobalConfig) {
        viewModelScope.launch {
            foulTvRepository.updateGlobalConfig(config)
                .onSuccess { _developerStatusMessage.value = "تم تحديث إعدادات FOUL TV العامة" }
                .onFailure { _developerStatusMessage.value = "خطأ: ${it.message}" }
        }
    }

    fun saveBottomBarVisibilityAdmin(
        showHome: Boolean,
        showIptv: Boolean,
        showFoulTv: Boolean,
        showLibrary: Boolean,
        showMatches: Boolean,
        hideHomeAll: Boolean,
        hideHomeMode: String,
        hideHomeCategory: String
    ) {
        viewModelScope.launch {
            val updates = mapOf<String, Any>(
                "show_home_tab" to showHome,
                "hide_home_tab" to !showHome,
                "show_iptv_tab" to showIptv,
                "hide_iptv_tab" to !showIptv,
                "show_foul_tv_tab" to showFoulTv,
                "hide_foul_tv" to !showFoulTv,
                "show_library_tab" to showLibrary,
                "hide_library_tab" to !showLibrary,
                "show_matches_tab" to showMatches,
                "hide_matches_schedule" to !showMatches,
                "hide_home_all" to hideHomeAll,
                "hide_home_mode" to hideHomeMode,
                "hide_home_category" to hideHomeCategory
            )
            repository.updateAppConfig(updates)
                .onSuccess {
                    _developerStatusMessage.value = "تم تحديث شريط التنقل السفلي وإعدادات الأقسام Online بنجاح"
                }
                .onFailure {
                    _developerStatusMessage.value = "خطأ: ${it.message}"
                }
        }
    }

    fun saveAppConfigAdmin(
        latestVersion: String,
        forceUpdate: Boolean,
        optionalUpdate: Boolean,
        updateMsg: String,
        updateUrl: String,
        telegramUrl: String,
        newDeveloperPass: String?
    ) {
        viewModelScope.launch {
            val normalizedVer = normalizeVersion(latestVersion)
            val updateTypeStr = when {
                forceUpdate -> "forced"
                optionalUpdate -> "optional"
                else -> "none"
            }
            val updates = mutableMapOf<String, Any>(
                "latest_version" to normalizedVer,
                "lock_old_versions" to forceUpdate,
                "optional_update_enabled" to optionalUpdate,
                "update_type" to updateTypeStr,
                "update_text" to updateMsg,
                "update_url" to updateUrl,
                "telegram_url" to telegramUrl
            )
            if (!newDeveloperPass.isNullOrBlank()) {
                updates["developer_password"] = newDeveloperPass
                prefs.edit().putString("developer_master_password", newDeveloperPass).apply()
            }
            repository.updateAppConfig(updates)
                .onSuccess {
                    _developerStatusMessage.value = "تم حفظ إعدادات الإصدار والتحديث (${if (forceUpdate) "إجباري" else if (optionalUpdate) "اختياري" else "عادي"}) بنجاح"
                }
                .onFailure {
                    _developerStatusMessage.value = "خطأ: ${it.message}"
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        presenceJob?.cancel()
        presenceRepository.stopWatchingCurrentChannel()
    }

    companion object {
        const val CURRENT_APP_VERSION = "4.0.0"

        fun normalizeVersion(raw: String): String {
            val cleaned = raw.trim().removePrefix("v").removePrefix("V").trim()
            if (cleaned.isBlank()) return CURRENT_APP_VERSION
            return cleaned
        }

        fun isAcceptedVersion(version: String): Boolean {
            val normalized = normalizeVersion(version)
            return normalized == CURRENT_APP_VERSION
        }

        fun isVersionOlder(current: String, target: String): Boolean {
            try {
                val currParts = current.trim().removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
                val targParts = target.trim().removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
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
    }
}
