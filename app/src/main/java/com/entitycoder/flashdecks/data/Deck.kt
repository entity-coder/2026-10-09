package com.entitycoder.flashdecks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named collection of flashcards.
 * bestScore is a percentage (0-100); -1 means the deck has never been quizzed.
 */
@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val bestScore: Int = -1,
    val quizzesTaken: Int = 0
)
