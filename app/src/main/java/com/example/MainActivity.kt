package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RecordingItem
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.RecordingsListScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranscriptDetailScreen
import com.example.ui.theme.ClassVoiceTheme
import com.example.viewmodel.MainViewModel

enum class NavTab {
    RECORD,
    RECORDINGS,
    SCHEDULE,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClassVoiceTheme {
                // Persian RTL support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ClassVoiceApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ClassVoiceApp(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(NavTab.RECORD) }
    val selectedRecording by viewModel.selectedRecording.collectAsStateWithLifecycle()

    // If viewing a recording detail, show TranscriptDetailScreen
    if (selectedRecording != null) {
        BackHandler {
            viewModel.setSelectedRecording(null)
        }
        TranscriptDetailScreen(
            recording = selectedRecording!!,
            viewModel = viewModel,
            onBack = { viewModel.setSelectedRecording(null) }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.RECORD,
                    onClick = { currentTab = NavTab.RECORD },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.RECORD) Icons.Filled.Mic else Icons.Outlined.Mic,
                            contentDescription = "ضبط کلاس"
                        )
                    },
                    label = { Text("ضبط کلاس") },
                    modifier = Modifier.testTag("tab_record")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.RECORDINGS,
                    onClick = { currentTab = NavTab.RECORDINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.RECORDINGS) Icons.Filled.Folder else Icons.Outlined.Folder,
                            contentDescription = "آرشیو و PDF"
                        )
                    },
                    label = { Text("آرشیو و PDF") },
                    modifier = Modifier.testTag("tab_recordings")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SCHEDULE,
                    onClick = { currentTab = NavTab.SCHEDULE },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.SCHEDULE) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "برنامه هفتگی"
                        )
                    },
                    label = { Text("برنامه هفتگی") },
                    modifier = Modifier.testTag("tab_schedule")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SETTINGS,
                    onClick = { currentTab = NavTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "تنظیمات"
                        )
                    },
                    label = { Text("تنظیمات") },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.RECORD -> {
                    RecordScreen(
                        viewModel = viewModel,
                        onOpenTranscript = { item ->
                            viewModel.setSelectedRecording(item)
                        }
                    )
                }
                NavTab.RECORDINGS -> {
                    RecordingsListScreen(
                        viewModel = viewModel,
                        onSelectRecording = { item ->
                            viewModel.setSelectedRecording(item)
                        },
                        onGoToRecord = {
                            currentTab = NavTab.RECORD
                        }
                    )
                }
                NavTab.SCHEDULE -> {
                    ScheduleScreen(viewModel = viewModel)
                }
                NavTab.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
