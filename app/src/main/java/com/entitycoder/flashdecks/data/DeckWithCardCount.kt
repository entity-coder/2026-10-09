package com.entitycoder.flashdecks.data

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * A deck plus a live count of its cards, produced by DeckDao.getDecks().
 * Used by the deck list screen so it needs only one observable query.
 */
data class DeckWithCardCount(
    @Embedded val deck: Deck,
    @ColumnInfo(name = "cardCount") val cardCount: Int = 0
)
