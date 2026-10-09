package com.entitycoder.flashdecks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    /**
     * All decks, newest first, each carrying its card count computed with a
     * correlated subquery so the UI needs only one observable query.
     */
    @Query(
        "SELECT *, (SELECT COUNT(*) FROM cards WHERE cards.deckId = decks.id) " +
            "AS cardCount FROM decks ORDER BY createdAt DESC"
    )
    fun getDecks(): Flow<List<DeckWithCardCount>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun getDeck(deckId: Long): Flow<Deck?>

    @Insert
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck)

    @Delete
    suspend fun delete(deck: Deck)
}
