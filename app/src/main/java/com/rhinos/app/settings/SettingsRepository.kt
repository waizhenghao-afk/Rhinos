package com.rhinos.app.settings

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val expiryTipsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = "RM",
    val currencySymbol: String = "RM",
    val onboarded: Boolean = false
)

/**
 * Lightweight settings on SharedPreferences. Currency is stored as code+symbol so
 * a future settings list can offer more currencies without a migration.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rhinos_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun current(): AppSettings = _settings.value

    private fun load(): AppSettings = AppSettings(
        expiryTipsEnabled = prefs.getBoolean(KEY_TIPS, true),
        themeMode = ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.SYSTEM },
        currencyCode = prefs.getString(KEY_CURRENCY_CODE, "RM") ?: "RM",
        currencySymbol = prefs.getString(KEY_CURRENCY_SYMBOL, "RM") ?: "RM",
        onboarded = prefs.getBoolean(KEY_ONBOARDED, false)
    )

    fun setExpiryTipsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIPS, enabled).apply()
        _settings.value = _settings.value.copy(expiryTipsEnabled = enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME, mode.ordinal).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    /** Currencies supported in V1. Structure allows adding more later. */
    fun setCurrency(code: String, symbol: String) {
        prefs.edit()
            .putString(KEY_CURRENCY_CODE, code)
            .putString(KEY_CURRENCY_SYMBOL, symbol)
            .apply()
        _settings.value = _settings.value.copy(currencyCode = code, currencySymbol = symbol)
    }

    fun setOnboarded() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
        _settings.value = _settings.value.copy(onboarded = true)
    }

    private companion object {
        const val KEY_TIPS = "expiry_tips_enabled"
        const val KEY_THEME = "theme_mode"
        const val KEY_CURRENCY_CODE = "currency_code"
        const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_ONBOARDED = "onboarded"
    }
}
