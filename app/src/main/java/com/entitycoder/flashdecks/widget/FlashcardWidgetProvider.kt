package com.entitycoder.flashdecks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.data.AppDatabase
import com.entitycoder.flashdecks.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen widget for FlashDecks.
 *
 * Shows one random flashcard from the whole collection. Tapping the card
 * flips between question and answer; the "Next" button deals a fresh random
 * card. Tapping the header opens the app. Fully offline — it reads the same
 * Room database as the app.
 *
 * Widget state (current card id + which side is showing) lives in
 * SharedPreferences keyed by widget id, because several copies of the widget
 * can exist at once and each keeps its own card.
 */
class FlashcardWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // System-triggered refresh (widget added, resized, device rebooted):
        // keep the current card when we have one, otherwise deal a new card.
        for (widgetId in appWidgetIds) {
            val pickNew = !prefs(context).contains(keyCard(widgetId))
            updateWidget(context, appWidgetManager, widgetId, pickNew = pickNew)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        when (intent.action) {
            ACTION_FLIP -> updateWidget(context, manager, widgetId, flip = true)
            ACTION_NEXT -> updateWidget(context, manager, widgetId, pickNew = true)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Drop saved state for removed widgets so the prefs don't grow forever.
        val edit = prefs(context).edit()
        for (widgetId in appWidgetIds) {
            edit.remove(keyCard(widgetId)).remove(keyBack(widgetId))
        }
        edit.apply()
    }

    /**
     * Re-renders one widget. Database work runs off the main thread and
     * goAsync() keeps the broadcast alive until it finishes.
     */
    private fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        pickNew: Boolean = false,
        flip: Boolean = false
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val prefs = prefs(context)
                val dao = AppDatabase.getInstance(context).cardDao()

                var cardId = prefs.getLong(keyCard(widgetId), -1L)
                var showingBack = prefs.getBoolean(keyBack(widgetId), false)

                if (pickNew || cardId == -1L) {
                    val card = dao.randomCard()
                    if (card == null) {
                        renderEmpty(context, manager, widgetId)
                        return
                    }
                    cardId = card.id
                    showingBack = false
                } else if (flip) {
                    showingBack = !showingBack
                }

                // The stored card may have been deleted or edited since the
                // widget last rendered; fall back to a fresh random card.
                val card = dao.getById(cardId) ?: dao.randomCard()
                if (card == null) {
                    prefs.edit().remove(keyCard(widgetId)).remove(keyBack(widgetId)).apply()
                    renderEmpty(context, manager, widgetId)
                    return
                }

                prefs.edit()
                    .putLong(keyCard(widgetId), card.id)
                    .putBoolean(keyBack(widgetId), showingBack)
                    .apply()

                val views = RemoteViews(context.packageName, R.layout.widget_flashcard)
                views.setTextViewText(
                    R.id.widget_card_text,
                    if (showingBack) card.back else card.front
                )
                views.setTextViewText(
                    R.id.widget_hint,
                    if (showingBack) context.getString(R.string.widget_hint_answer)
                    else context.getString(R.string.widget_hint_question)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_card_text,
                    broadcast(context, ACTION_FLIP, widgetId)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_next_button,
                    broadcast(context, ACTION_NEXT, widgetId)
                )
                views.setOnClickPendingIntent(R.id.widget_header, openApp(context))
                manager.updateAppWidget(widgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Shown when the collection is empty: tapping opens the app to add cards. */
    private fun renderEmpty(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_flashcard)
        views.setTextViewText(R.id.widget_card_text, context.getString(R.string.widget_empty))
        views.setTextViewText(R.id.widget_hint, "")
        views.setOnClickPendingIntent(R.id.widget_header, openApp(context))
        views.setOnClickPendingIntent(R.id.widget_card_text, openApp(context))
        manager.updateAppWidget(widgetId, views)
    }

    private fun broadcast(context: Context, action: String, widgetId: Int): PendingIntent {
        val intent = Intent(context, FlashcardWidgetProvider::class.java)
            .setAction(action)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        // Extras don't distinguish PendingIntents, so the request code must
        // differ per widget and per action.
        val requestCode = widgetId * 2 + if (action == ACTION_NEXT) 1 else 0
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun keyCard(widgetId: Int) = "card_id_$widgetId"
    private fun keyBack(widgetId: Int) = "showing_back_$widgetId"

    companion object {
        private const val PREFS_NAME = "flashcard_widget"
        private const val ACTION_FLIP = "com.entitycoder.flashdecks.widget.action.FLIP"
        private const val ACTION_NEXT = "com.entitycoder.flashdecks.widget.action.NEXT"
    }
}
