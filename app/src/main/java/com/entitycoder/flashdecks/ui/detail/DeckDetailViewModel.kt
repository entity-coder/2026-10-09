package com.entitycoder.flashdecks.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.entitycoder.flashdecks.FlashDecksApp
import com.entitycoder.flashdecks.data.Card
import com.entitycoder.flashdecks.data.Deck
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the deck detail screen AND is shared (activity-scoped) with the quiz
 * screens, so the quiz can record its result on the same deck instance the
 * detail screen is observing. Call [load] once with the deck id.
 */
class DeckDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FlashDecksApp).repository

    private val deckId = MutableStateFlow<Long?>(null)

    fun load(id: Long) {
        if (deckId.value != id) deckId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val deck: StateFlow<Deck?> = deckId.filterNotNull()
        .flatMapLatest { repository.getDeck(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val cards: StateFlow<List<Card>> = deckId.filterNotNull()
        .flatMapLatest { repository.getCards(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Convenience flow for "0 cards" / "N cards" UI states. */
    val cardCount: StateFlow<Int> = cards
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addCard(front: String, back: String) {
        val id = deckId.value ?: return
        viewModelScope.launch {
            // New cards go at the end of the stable ordering.
            val position = (repository.maxCardPosition(id) ?: -1) + 1
            repository.insertCard(Card(deckId = id, front = front, back = back, position = position))
        }
    }

    fun updateCard(cardId: Long, front: String, back: String) {
        viewModelScope.launch {
            val existing = cards.value.firstOrNull { it.id == cardId } ?: return@launch
            repository.updateCard(existing.copy(front = front, back = back))
        }
    }

    fun deleteCard(card: Card) {
        viewModelScope.launch { repository.deleteCard(card) }
    }

    fun renameDeck(name: String) {
        viewModelScope.launch {
            val current = deck.value ?: return@launch
            repository.updateDeck(current.copy(name = name))
        }
    }

    fun deleteDeck() {
        viewModelScope.launch {
            val current = deck.value ?: return@launch
            repository.deleteDeck(current)
        }
    }

    /**
     * Records a finished quiz: bumps quizzesTaken and keeps the higher of the
     * old and new percentage as bestScore. Returns true when the score is a
     * new best (or the deck's first ever score) so the result screen can
     * celebrate it.
     */
    suspend fun recordQuizResult(percent: Int): Boolean {
        val current = deck.value ?: return false
        val isNewBest = current.bestScore < 0 || percent > current.bestScore
        repository.updateDeck(
            current.copy(
                bestScore = if (isNewBest) percent else current.bestScore,
                quizzesTaken = current.quizzesTaken + 1
            )
        )
        return isNewBest
    }
}
