package com.entitycoder.flashdecks.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entitycoder.flashdecks.data.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Immutable snapshot of everything the quiz screen renders. */
data class QuizUiState(
    val index: Int = 0,
    val total: Int = 0,
    val revealed: Boolean = false,
    val finished: Boolean = false,
    val card: Card? = null
)

/**
 * Owns one quiz session. Plain ViewModel (fragment-scoped): a session is
 * transient UI state, so it is deliberately NOT persisted to the database.
 * The final percentage is handed to DeckDetailViewModel.recordQuizResult()
 * by the fragment when the session ends.
 */
class QuizViewModel : ViewModel() {

    private val cards = MutableStateFlow<List<Card>>(emptyList())
    private val currentIndex = MutableStateFlow(0)
    private val revealed = MutableStateFlow(false)
    private val correctCount = MutableStateFlow(0)
    private val wrongCount = MutableStateFlow(0)
    private val isFinished = MutableStateFlow(false)

    val uiState: StateFlow<QuizUiState> = combine(
        cards, currentIndex, revealed, isFinished
    ) { cardList, index, isRevealed, finished ->
        QuizUiState(
            index = index,
            total = cardList.size,
            revealed = isRevealed,
            finished = finished,
            card = cardList.getOrNull(index)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizUiState())

    val correct: Int get() = correctCount.value
    val wrong: Int get() = wrongCount.value
    val total: Int get() = cards.value.size

    /** Whole-number percentage of cards marked correct. */
    val scorePercent: Int
        get() = if (total == 0) 0 else correctCount.value * 100 / total

    /** Starts a new session with the deck's cards in random order. */
    fun start(deckCards: List<Card>) {
        cards.value = deckCards.shuffled()
        currentIndex.value = 0
        revealed.value = false
        correctCount.value = 0
        wrongCount.value = 0
        isFinished.value = false
    }

    /** Called after the flip animation completes; shows the grading buttons. */
    fun reveal() {
        revealed.value = true
    }

    fun markCorrect() {
        if (isFinished.value) return
        correctCount.value++
        advance()
    }

    fun markWrong() {
        if (isFinished.value) return
        wrongCount.value++
        advance()
    }

    private fun advance() {
        revealed.value = false
        if (currentIndex.value + 1 >= cards.value.size) {
            isFinished.value = true
        } else {
            currentIndex.value++
        }
    }
}
