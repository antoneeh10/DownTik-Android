package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.downloader.DownTikStorage
import com.example.ui.MainViewModel
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Appearance / Theme
            Text(
                text = "Tampilan",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    ThemeOptionRow(
                        title = "Ikuti Sistem",
                        icon = Icons.Default.BrightnessAuto,
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        testTag = "theme_system_radio"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                    ThemeOptionRow(
                        title = "Mode Terang",
                        icon = Icons.Default.LightMode,
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        testTag = "theme_light_radio"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                    ThemeOptionRow(
                        title = "Mode Gelap",
                        icon = Icons.Default.DarkMode,
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        testTag = "theme_dark_radio"
                    )
                }
            }

            // Section: Storage
            Text(
                text = "Lokasi Penyimpanan",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Video folder row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Video",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DownTikStorage.VIDEO_FOLDER,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio folder row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Audio",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DownTikStorage.AUDIO_FOLDER,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Clear history button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearConfirmDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hapus Riwayat Unduhan",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hapus daftar riwayat tanpa menghapus file di galeri/penyimpanan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Section: About
            Text(
                text = "Tentang",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "DownTik",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Versi 1.1.0 • Video Downloader & Audio Extractor",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DownTik didesain dengan Material Design 3 untuk pengalaman download video dan ekstraksi audio yang cepat, bersih, tanpa watermark, dan aman.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Clear History Confirmation Dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Hapus Semua Riwayat?") },
                text = { Text("Daftar riwayat unduhan akan dikosongkan. File video & audio yang sudah tersimpan di penyimpanan tidak akan terhapus.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearConfirmDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Riwayat unduhan telah dibersihkan")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.testTag(testTag)
        )
    }
}
