package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.db.MixtunDatabase
import com.example.data.model.FileCategory
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.media.PlaybackManager
import com.example.ui.components.MixtunGlassBottomBar
import com.example.ui.components.MixtunMiniPlayer
import com.example.ui.components.MixtunTab
import com.example.ui.screens.*
import com.example.ui.theme.MixtunTheme
import com.example.ui.theme.PastelTheme
import com.example.ui.theme.getBackgroundBrush
import com.example.ui.viewers.UniversalViewerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: FileRepository
    private lateinit var playbackManager: PlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = MixtunDatabase.getInstance(applicationContext)
        repository = FileRepository(applicationContext, db.mixtunDao())
        playbackManager = PlaybackManager(applicationContext, repository)

        // Realtime data only: purge any old demo mock records, and scan real device media
        lifecycleScope.launch {
            repository.purgeDemoData()
            repository.scanDeviceMedia()
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var currentPastelTheme by remember { mutableStateOf(PastelTheme.CYAN_ICE) }
            var currentTab by remember { mutableStateOf(MixtunTab.HOME) }
            var activeViewerFile by remember { mutableStateOf<FileRecord?>(null) }
            var isSearching by remember { mutableStateOf(false) }
            var browseCategory by remember { mutableStateOf(FileCategory.ALL) }

            // Handle incoming Intent (Open With / View / Send)
            LaunchedEffect(intent) {
                handleIncomingIntent(intent) { file ->
                    activeViewerFile = file
                }
            }

            // Observe mini-player
            val miniPlayerVisible by playbackManager.isMiniPlayerVisible.collectAsState()
            val currentMedia by playbackManager.currentFile.collectAsState()
            val isPlaying by playbackManager.isPlaying.collectAsState()
            val currentPos by playbackManager.currentPosition.collectAsState()
            val duration by playbackManager.duration.collectAsState()

            MixtunTheme(darkTheme = isDarkTheme, pastelTheme = currentPastelTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(getBackgroundBrush(isDarkTheme, currentPastelTheme))
                ) {
                    if (activeViewerFile != null) {
                        UniversalViewerScreen(
                            file = activeViewerFile!!,
                            repository = repository,
                            playbackManager = playbackManager,
                            onBack = {
                                activeViewerFile = null
                            }
                        )
                    } else if (isSearching) {
                        BackHandler { isSearching = false }
                        SearchScreen(
                            repository = repository,
                            onOpenFile = { file ->
                                isSearching = false
                                activeViewerFile = file
                            },
                            onBack = { isSearching = false }
                        )
                    } else {
                        // Main Tabs Host
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (currentTab) {
                                MixtunTab.HOME -> {
                                    HomeScreen(
                                        repository = repository,
                                        onOpenFile = { file -> activeViewerFile = file },
                                        onNavigateToCategory = { cat ->
                                            browseCategory = cat
                                            currentTab = MixtunTab.BROWSE
                                        },
                                        onSearchClick = { isSearching = true }
                                    )
                                }
                                MixtunTab.BROWSE -> {
                                    BrowseScreen(
                                        repository = repository,
                                        initialCategory = browseCategory,
                                        onOpenFile = { file -> activeViewerFile = file },
                                        onSearchClick = { isSearching = true }
                                    )
                                }
                                MixtunTab.RECENT -> {
                                    RecentScreen(
                                        repository = repository,
                                        onOpenFile = { file -> activeViewerFile = file }
                                    )
                                }
                                MixtunTab.FAVORITES -> {
                                    FavoritesScreen(
                                        repository = repository,
                                        onOpenFile = { file -> activeViewerFile = file }
                                    )
                                }
                                MixtunTab.SETTINGS -> {
                                    SettingsScreen(
                                        isDarkTheme = isDarkTheme,
                                        onToggleDarkTheme = { isDarkTheme = it },
                                        currentPastelTheme = currentPastelTheme,
                                        onSelectPastelTheme = { currentPastelTheme = it },
                                        repository = repository
                                    )
                                }
                            }

                            // Bottom Navigation and Mini Player Stack
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                // Floating Mini Player
                                if (miniPlayerVisible && currentMedia != null) {
                                    MixtunMiniPlayer(
                                        file = currentMedia!!,
                                        isPlaying = isPlaying,
                                        currentPositionMs = currentPos,
                                        durationMs = duration,
                                        onExpand = {
                                            activeViewerFile = currentMedia
                                        },
                                        onTogglePlayPause = {
                                            playbackManager.togglePlayPause()
                                        },
                                        onDismiss = {
                                            playbackManager.dismissMiniPlayer()
                                        }
                                    )
                                }

                                // Floating Bottom Bar
                                MixtunGlassBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = { tab ->
                                        currentTab = tab
                                        if (tab == MixtunTab.BROWSE) {
                                            browseCategory = FileCategory.ALL
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?, onFileReady: (FileRecord) -> Unit) {
        if (intent == null) return
        val action = intent.action
        val uri: Uri? = when (action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        if (uri != null) {
            lifecycleScope.launch {
                val record = repository.importUri(uri)
                if (record != null) {
                    onFileReady(record)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.release()
    }
}
