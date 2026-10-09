package com.entitycoder.flashdecks

import android.app.Application
import com.entitycoder.flashdecks.data.AppDatabase
import com.entitycoder.flashdecks.data.FlashcardRepository
import com.entitycoder.flashdecks.ui.theme.ThemeManager

/**
 * Application class that owns the single Room database instance and its
 * repository, so every ViewModel can reach them via
 * `(application as FlashDecksApp).repository`.
 *
 * It also applies the user's saved theme (light/dark/system) before any
 * Activity is created, avoiding a theme flash on cold start.
 */
class FlashDecksApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: FlashcardRepository by lazy { FlashcardRepository(database) }

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applySaved(this)
    }
}
