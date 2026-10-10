package com.entitycoder.flashdecks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    /** Cards of one deck in stable creation order. */
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY position ASC")
    fun getCards(deckId: Long): Flow<List<Card>>

    /** Highest position used in a deck; null when the deck has no cards. */
    @Query("SELECT MAX(position) FROM cards WHERE deckId = :deckId")
    suspend fun maxPosition(deckId: Long): Int?

    /** Random card across all decks — feeds the home-screen widget. */
    @Query("SELECT * FROM cards ORDER BY RANDOM() LIMIT 1")
    suspend fun randomCard(): Card?

    /** Single card lookup, used when the widget re-renders its stored card. */
    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getById(cardId: Long): Card?

    @Insert
    suspend fun insert(card: Card): Long

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)
}
