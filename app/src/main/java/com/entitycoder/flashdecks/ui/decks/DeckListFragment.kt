package com.entitycoder.flashdecks.ui.decks

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.ui.MainActivity
import com.entitycoder.flashdecks.ui.detail.DeckDetailFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * Root screen: the list of decks. FAB creates a deck, tapping a row opens it,
 * long-pressing a row asks for delete confirmation.
 */
class DeckListFragment : Fragment() {

    private val viewModel: DeckListViewModel by viewModels()
    private lateinit var adapter: DeckAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_deck_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DeckAdapter(
            onClick = { item ->
                (requireActivity() as MainActivity).showFragment(
                    DeckDetailFragment.newInstance(item.deck.id)
                )
            },
            onLongClick = { item -> confirmDeleteDeck(item.deck.name, item.deck.id) }
        )

        view.findViewById<RecyclerView>(R.id.deck_recycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DeckListFragment.adapter
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_deck)
            .setOnClickListener { showAddDeckDialog() }

        val emptyView = view.findViewById<TextView>(R.id.empty_state)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.decks.collect { decks ->
                    adapter.submitList(decks)
                    emptyView.visibility = if (decks.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).supportActionBar?.title = getString(R.string.title_decks)
    }

    private fun showAddDeckDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.deck_name_hint)
        }
        // Dialogs need margin around the EditText; wrap it in a padded container.
        val container = FrameLayout(requireContext()).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_new_deck_title)
            .setView(container)
            .setPositiveButton(R.string.action_add) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addDeck(name)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDeleteDeck(deckName: String, deckId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_deck_title)
            .setMessage(getString(R.string.dialog_delete_deck_message, deckName))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // Resolve the Deck from the latest list snapshot for deletion.
                val target = viewModel.decks.value
                    .firstOrNull { it.deck.id == deckId }?.deck
                if (target != null) viewModel.deleteDeck(target)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
