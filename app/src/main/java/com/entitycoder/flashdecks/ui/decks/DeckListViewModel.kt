package com.entitycoder.flashdecks.ui.decks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.entitycoder.flashdecks.FlashDecksApp
import com.entitycoder.flashdecks.data.Deck
import com.entitycoder.flashdecks.data.DeckWithCardCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the deck list screen. AndroidViewModel is used (instead of a plain
 * ViewModel with a factory) so the repository can be reached through the
 * Application without writing a custom ViewModelProvider.Factory.
 */
class DeckListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FlashDecksApp).repository

    /** All decks with live card counts, newest first. */
    val decks: StateFlow<List<DeckWithCardCount>> = repository.getDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDeck(name: String) {
        viewModelScope.launch {
            repository.insertDeck(Deck(name = name))
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            // Card rows are removed by the ON DELETE CASCADE foreign key.
            repository.deleteDeck(deck)
        }
    }
}
