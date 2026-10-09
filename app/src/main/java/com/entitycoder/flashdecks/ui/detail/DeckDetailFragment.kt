package com.entitycoder.flashdecks.ui.detail

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.data.Card
import com.entitycoder.flashdecks.ui.MainActivity
import com.entitycoder.flashdecks.ui.editor.CardEditorDialogFragment
import com.entitycoder.flashdecks.ui.quiz.QuizFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * Shows one deck: its stats, its cards, and the entry point to a quiz.
 * The ViewModel is activity-scoped (shared with QuizFragment) so the quiz
 * can record its result on the same deck instance.
 */
class DeckDetailFragment : Fragment() {

    private val viewModel: DeckDetailViewModel by activityViewModels()
    private lateinit var adapter: CardAdapter

    private var deckId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckId = requireArguments().getLong(ARG_DECK_ID)
        viewModel.load(deckId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_deck_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CardAdapter(
            onClick = { card -> openCardEditor(card) },
            onLongClick = { card -> confirmDeleteCard(card) }
        )
        view.findViewById<RecyclerView>(R.id.card_recycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DeckDetailFragment.adapter
        }

        val titleView = view.findViewById<TextView>(R.id.deck_title)
        val metaView = view.findViewById<TextView>(R.id.deck_meta)
        val bestView = view.findViewById<TextView>(R.id.deck_best)
        val emptyView = view.findViewById<TextView>(R.id.empty_cards)
        val quizButton = view.findViewById<MaterialButton>(R.id.button_start_quiz)
        val quizHint = view.findViewById<TextView>(R.id.quiz_hint)

        // Tapping the title renames the deck, same as the overflow menu item.
        titleView.setOnClickListener { showRenameDialog() }

        view.findViewById<FloatingActionButton>(R.id.fab_add_card)
            .setOnClickListener { openCardEditor(null) }

        quizButton.setOnClickListener {
            (requireActivity() as MainActivity).showFragment(QuizFragment.newInstance(deckId))
        }

        // Overflow menu: rename / delete deck.
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.deck_detail_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_rename_deck -> {
                        showRenameDialog()
                        true
                    }
                    R.id.action_delete_deck -> {
                        confirmDeleteDeck()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Results from the add/edit card dialog.
        parentFragmentManager.setFragmentResultListener(
            CardEditorDialogFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val cardId = bundle.getLong(CardEditorDialogFragment.EXTRA_CARD_ID, -1L)
            val front = bundle.getString(CardEditorDialogFragment.EXTRA_FRONT).orEmpty()
            val back = bundle.getString(CardEditorDialogFragment.EXTRA_BACK).orEmpty()
            if (cardId == -1L) {
                viewModel.addCard(front, back)
            } else {
                viewModel.updateCard(cardId, front, back)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.deck.collect { deck ->
                        // Deck is null only if it was deleted elsewhere; back out.
                        if (deck == null) {
                            requireActivity().supportFragmentManager.popBackStack()
                            return@collect
                        }
                        titleView.text = deck.name
                        (requireActivity() as MainActivity).supportActionBar?.title = deck.name
                        metaView.text = resources.getQuantityString(
                            R.plurals.cards_count, viewModel.cardCount.value, viewModel.cardCount.value
                        ) + " · " + getString(R.string.quizzes_taken, deck.quizzesTaken)
                        bestView.text = if (deck.bestScore >= 0) {
                            getString(R.string.best_score, deck.bestScore)
                        } else {
                            getString(R.string.not_quizzed)
                        }
                    }
                }
                launch {
                    viewModel.cards.collect { cards ->
                        adapter.submitList(cards)
                        emptyView.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
                        val hasCards = cards.isNotEmpty()
                        quizButton.isEnabled = hasCards
                        quizHint.visibility = if (hasCards) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun openCardEditor(card: Card?) {
        CardEditorDialogFragment.newInstance(deckId, card)
            .show(parentFragmentManager, CardEditorDialogFragment.TAG)
    }

    private fun showRenameDialog() {
        val currentName = viewModel.deck.value?.name.orEmpty()
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(currentName)
            setSelection(currentName.length)
        }
        val container = FrameLayout(requireContext()).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_rename_deck_title)
            .setView(container)
            .setPositiveButton(R.string.action_rename) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.renameDeck(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDeleteCard(card: Card) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_card_title)
            .setMessage(R.string.dialog_delete_card_message)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteCard(card) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDeleteDeck() {
        val name = viewModel.deck.value?.name.orEmpty()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_deck_title)
            .setMessage(getString(R.string.dialog_delete_deck_message, name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteDeck()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"

        fun newInstance(deckId: Long): DeckDetailFragment {
            return DeckDetailFragment().apply {
                arguments = Bundle().apply { putLong(ARG_DECK_ID, deckId) }
            }
        }
    }
}
