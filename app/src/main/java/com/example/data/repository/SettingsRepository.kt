package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.ThemeMode
import com.example.update.ReleaseChannel
import com.example.update.UpdateConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("downtik_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _updateChannel = MutableStateFlow(loadUpdateChannel())
    val updateChannel: StateFlow<ReleaseChannel> = _updateChannel.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadUpdateChannel(): ReleaseChannel {
        val saved = prefs.getString(KEY_UPDATE_CHANNEL, UpdateConfig.DEFAULT_CHANNEL.id)
        return ReleaseChannel.fromId(saved)
    }

    fun setUpdateChannel(channel: ReleaseChannel) {
        prefs.edit().putString(KEY_UPDATE_CHANNEL, channel.id).apply()
        _updateChannel.value = channel
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_UPDATE_CHANNEL = "key_update_channel"
    }
}
