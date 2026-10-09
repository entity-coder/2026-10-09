package com.entitycoder.flashdecks.ui.quiz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.ui.MainActivity
import com.entitycoder.flashdecks.ui.detail.DeckDetailViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * Runs one quiz session: the card front is shown, tapping the card flips it
 * with a 3D rotation to reveal the answer, then the user self-grades with
 * "I got it right" / "I got it wrong". When the last card is graded the
 * session score is recorded on the deck and the result screen opens.
 */
class QuizFragment : Fragment() {

    private val quizViewModel: QuizViewModel by viewModels()
    private val detailViewModel: DeckDetailViewModel by activityViewModels()

    private var deckId: Long = -1L
    private var navigatedToResult = false

    private lateinit var cardView: MaterialCardView
    private lateinit var cardText: TextView
    private lateinit var progressText: TextView
    private lateinit var hintText: TextView
    private lateinit var correctButton: MaterialButton
    private lateinit var wrongButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckId = requireArguments().getLong(ARG_DECK_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardView = view.findViewById(R.id.quiz_card)
        cardText = view.findViewById(R.id.quiz_card_text)
        progressText = view.findViewById(R.id.quiz_progress)
        hintText = view.findViewById(R.id.quiz_hint)
        correctButton = view.findViewById(R.id.correct_button)
        wrongButton = view.findViewById(R.id.wrong_button)

        // A larger camera distance keeps the flip from looking distorted.
        cardView.cameraDistance = resources.displayMetrics.density * 8000f

        val cards = detailViewModel.cards.value
        if (cards.isEmpty()) {
            // Shouldn't happen (the Start button is disabled), but bail out safely.
            requireActivity().supportFragmentManager.popBackStack()
            return
        }
        if (savedInstanceState == null) {
            quizViewModel.start(cards)
        }

        cardView.setOnClickListener { flipCard() }
        correctButton.setOnClickListener { quizViewModel.markCorrect() }
        wrongButton.setOnClickListener { quizViewModel.markWrong() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                quizViewModel.uiState.collect { state ->
                    if (state.finished) {
                        onQuizFinished()
                        return@collect
                    }
                    progressText.text = getString(
                        R.string.quiz_progress, state.index + 1, state.total
                    )
                    cardText.text = state.card?.front.orEmpty()
                    val gradingVisible = if (state.revealed) View.VISIBLE else View.GONE
                    correctButton.visibility = gradingVisible
                    wrongButton.visibility = gradingVisible
                    hintText.visibility = if (state.revealed) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).supportActionBar?.title = getString(R.string.title_quiz)
    }

    /**
     * Two-stage flip: rotate to 90° (edge-on, invisible), swap front/back
     * text, then rotate 90° -> 180° and snap back to 0° (visually identical,
     * keeps the next flip starting from a clean state).
     */
    private fun flipCard() {
        val state = quizViewModel.uiState.value
        val card = state.card
        if (state.revealed || state.finished || card == null) return

        val firstHalf = ObjectAnimator.ofFloat(cardView, "rotationY", 0f, 90f).apply {
            duration = 160
        }
        firstHalf.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                cardText.text = card.back
                val secondHalf = ObjectAnimator.ofFloat(cardView, "rotationY", 90f, 180f).apply {
                    duration = 160
                }
                secondHalf.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cardView.rotationY = 0f
                        quizViewModel.reveal()
                    }
                })
                secondHalf.start()
            }
        })
        firstHalf.start()
    }

    private fun onQuizFinished() {
        if (navigatedToResult) return
        navigatedToResult = true
        // Record the score on the deck, then show the result screen.
        // The result replaces this fragment WITHOUT a back-stack entry so
        // that Back/Done from the result returns to the deck detail screen.
        viewLifecycleOwner.lifecycleScope.launch {
            val isNewBest = detailViewModel.recordQuizResult(quizViewModel.scorePercent)
            (requireActivity() as MainActivity).showFragment(
                QuizResultFragment.newInstance(
                    deckId = deckId,
                    correct = quizViewModel.correct,
                    total = quizViewModel.total,
                    percent = quizViewModel.scorePercent,
                    isNewBest = isNewBest
                ),
                addToBackStack = false
            )
        }
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"

        fun newInstance(deckId: Long): QuizFragment {
            return QuizFragment().apply {
                arguments = Bundle().apply { putLong(ARG_DECK_ID, deckId) }
            }
        }
    }
}
