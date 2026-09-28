package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.AudioExtractorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainDownloadScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DownTikTheme

enum class Screen {
    HOME,
    HISTORY,
    SETTINGS
}

enum class BottomDestination {
    VIDEO,
    AUDIO
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isVideoPreviewOpen by viewModel.isVideoPreviewOpen.collectAsStateWithLifecycle()

            DownTikTheme(themeMode = themeMode) {
                var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
                var selectedDestination by rememberSaveable { mutableStateOf(BottomDestination.VIDEO) }
                val snackbarHostState = remember { SnackbarHostState() }

                // Handle back button navigation
                BackHandler(enabled = !isVideoPreviewOpen && (currentScreen != Screen.HOME || selectedDestination != BottomDestination.VIDEO)) {
                    if (currentScreen != Screen.HOME) {
                        currentScreen = Screen.HOME
                    } else if (selectedDestination != BottomDestination.VIDEO) {
                        selectedDestination = BottomDestination.VIDEO
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        if (!isVideoPreviewOpen) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentScreen) {
                                            Screen.HOME -> "DownTik"
                                            Screen.HISTORY -> "Riwayat Download"
                                            Screen.SETTINGS -> "Pengaturan"
                                        },
                                        style = if (currentScreen == Screen.HOME) {
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.0.sp
                                            )
                                        } else {
                                            MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                navigationIcon = {
                                    if (currentScreen != Screen.HOME) {
                                        IconButton(
                                            onClick = { currentScreen = Screen.HOME },
                                            modifier = Modifier.testTag("nav_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Kembali ke Beranda"
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    if (currentScreen == Screen.HOME) {
                                        IconButton(
                                            onClick = { currentScreen = Screen.HISTORY },
                                            modifier = Modifier.testTag("history_nav_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "Riwayat Unduhan"
                                            )
                                        }

                                        IconButton(
                                            onClick = { currentScreen = Screen.SETTINGS },
                                            modifier = Modifier.testTag("settings_nav_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Pengaturan"
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen == Screen.HOME && !isVideoPreviewOpen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                NavigationBarItem(
                                    selected = selectedDestination == BottomDestination.VIDEO,
                                    onClick = { selectedDestination = BottomDestination.VIDEO },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedDestination == BottomDestination.VIDEO) {
                                                Icons.Default.Download
                                            } else {
                                                Icons.Outlined.Download
                                            },
                                            contentDescription = "Video Downloader"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Video",
                                            fontWeight = if (selectedDestination == BottomDestination.VIDEO) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_video")
                                )

                                NavigationBarItem(
                                    selected = selectedDestination == BottomDestination.AUDIO,
                                    onClick = { selectedDestination = BottomDestination.AUDIO },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedDestination == BottomDestination.AUDIO) {
                                                Icons.Default.MusicNote
                                            } else {
                                                Icons.Outlined.MusicNote
                                            },
                                            contentDescription = "Audio Extractor"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Audio",
                                            fontWeight = if (selectedDestination == BottomDestination.AUDIO) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_audio")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            Screen.HOME -> {
                                // Keep both screens active or smoothly switched with state preserved
                                Box(modifier = Modifier.fillMaxSize()) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = selectedDestination == BottomDestination.VIDEO,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        MainDownloadScreen(viewModel = viewModel)
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = selectedDestination == BottomDestination.AUDIO,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        AudioExtractorScreen(viewModel = viewModel)
                                    }
                                }
                            }
                            Screen.HISTORY -> {
                                HistoryScreen(viewModel = viewModel)
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
