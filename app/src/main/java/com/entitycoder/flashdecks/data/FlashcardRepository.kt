package com.entitycoder.flashdecks.data

import kotlinx.coroutines.flow.Flow

/**
 * Single entry point to the data layer. The repository exposes the DAOs'
 * Flows and suspend functions unchanged; ViewModels talk only to this class,
 * never to a DAO directly, so the data source can change without touching UI.
 */
class FlashcardRepository(private val db: AppDatabase) {

    fun getDecks(): Flow<List<DeckWithCardCount>> = db.deckDao().getDecks()

    fun getDeck(deckId: Long): Flow<Deck?> = db.deckDao().getDeck(deckId)

    fun getCards(deckId: Long): Flow<List<Card>> = db.cardDao().getCards(deckId)

    suspend fun insertDeck(deck: Deck): Long = db.deckDao().insert(deck)

    suspend fun updateDeck(deck: Deck) = db.deckDao().update(deck)

    suspend fun deleteDeck(deck: Deck) = db.deckDao().delete(deck)

    suspend fun insertCard(card: Card): Long = db.cardDao().insert(card)

    suspend fun updateCard(card: Card) = db.cardDao().update(card)

    suspend fun deleteCard(card: Card) = db.cardDao().delete(card)

    suspend fun maxCardPosition(deckId: Long): Int? = db.cardDao().maxPosition(deckId)
}
