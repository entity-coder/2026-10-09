package com.entitycoder.flashdecks.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Persists the user's light/dark/system theme choice in SharedPreferences and
 * applies it through AppCompatDelegate. Called once from FlashDecksApp so the
 * saved mode is active before any Activity is created.
 */
object ThemeManager {

    const val MODE_SYSTEM = "system"
    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"

    private const val PREFS_NAME = "flashdecks_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
        apply(mode)
    }

    /** Applies a mode string without persisting it. */
    fun applySaved(context: Context) {
        apply(getThemeMode(context))
    }

    private fun apply(mode: String) {
        val nightMode = when (mode) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
