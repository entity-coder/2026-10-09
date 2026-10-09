package com.entitycoder.flashdecks.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.ui.MainActivity
import com.google.android.material.button.MaterialButton

/**
 * Shows the session score. Retry starts a fresh shuffled session; Done
 * returns to the deck detail screen.
 */
class QuizResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_quiz_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val deckId = args.getLong(ARG_DECK_ID)
        val correct = args.getInt(ARG_CORRECT)
        val total = args.getInt(ARG_TOTAL)
        val percent = args.getInt(ARG_PERCENT)
        val isNewBest = args.getBoolean(ARG_IS_NEW_BEST)

        view.findViewById<TextView>(R.id.result_score).text =
            getString(R.string.quiz_score_value, percent)
        view.findViewById<TextView>(R.id.result_detail).text =
            getString(R.string.quiz_score_detail, correct, total)
        view.findViewById<TextView>(R.id.result_best).visibility =
            if (isNewBest) View.VISIBLE else View.GONE

        view.findViewById<MaterialButton>(R.id.retry_button).setOnClickListener {
            (requireActivity() as MainActivity).showFragment(QuizFragment.newInstance(deckId))
        }
        view.findViewById<MaterialButton>(R.id.done_button).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).supportActionBar?.title = getString(R.string.title_result)
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        private const val ARG_CORRECT = "correct"
        private const val ARG_TOTAL = "total"
        private const val ARG_PERCENT = "percent"
        private const val ARG_IS_NEW_BEST = "is_new_best"

        fun newInstance(
            deckId: Long,
            correct: Int,
            total: Int,
            percent: Int,
            isNewBest: Boolean
        ): QuizResultFragment {
            return QuizResultFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DECK_ID, deckId)
                    putInt(ARG_CORRECT, correct)
                    putInt(ARG_TOTAL, total)
                    putInt(ARG_PERCENT, percent)
                    putBoolean(ARG_IS_NEW_BEST, isNewBest)
                }
            }
        }
    }
}
