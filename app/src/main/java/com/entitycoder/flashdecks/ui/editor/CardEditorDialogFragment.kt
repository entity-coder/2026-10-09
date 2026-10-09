package com.entitycoder.flashdecks.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.data.Card
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Add/edit dialog for a single card. Communicates back through the Fragment
 * Result API on [REQUEST_KEY]: the parent fragment registers a listener and
 * reads [EXTRA_CARD_ID] (-1 for a new card), [EXTRA_FRONT] and [EXTRA_BACK].
 */
class CardEditorDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_card_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val cardId = args.getLong(ARG_CARD_ID, -1L)

        view.findViewById<TextView>(R.id.dialog_title).text = getString(
            if (cardId == -1L) R.string.dialog_add_card_title else R.string.dialog_edit_card_title
        )

        val frontInput = view.findViewById<TextInputEditText>(R.id.front_input)
        val backInput = view.findViewById<TextInputEditText>(R.id.back_input)
        frontInput.setText(args.getString(ARG_FRONT).orEmpty())
        backInput.setText(args.getString(ARG_BACK).orEmpty())

        view.findViewById<MaterialButton>(R.id.button_cancel)
            .setOnClickListener { dismiss() }

        view.findViewById<MaterialButton>(R.id.button_save).setOnClickListener {
            val front = frontInput.text.toString().trim()
            val back = backInput.text.toString().trim()
            if (front.isEmpty() || back.isEmpty()) {
                // Nudge the user instead of silently dropping the card.
                if (front.isEmpty()) frontInput.error = getString(R.string.error_empty_card)
                if (back.isEmpty()) backInput.error = getString(R.string.error_empty_card)
                return@setOnClickListener
            }
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    EXTRA_CARD_ID to cardId,
                    EXTRA_FRONT to front,
                    EXTRA_BACK to back
                )
            )
            dismiss()
        }
    }

    companion object {
        const val TAG = "card_editor"
        const val REQUEST_KEY = "card_editor_request"
        const val EXTRA_CARD_ID = "card_id"
        const val EXTRA_FRONT = "front"
        const val EXTRA_BACK = "back"

        private const val ARG_DECK_ID = "deck_id"
        private const val ARG_CARD_ID = "card_id"
        private const val ARG_FRONT = "front"
        private const val ARG_BACK = "back"

        /** Pass card = null to create a new card, or an existing card to edit it. */
        fun newInstance(deckId: Long, card: Card?): CardEditorDialogFragment {
            return CardEditorDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DECK_ID, deckId)
                    putLong(ARG_CARD_ID, card?.id ?: -1L)
                    putString(ARG_FRONT, card?.front.orEmpty())
                    putString(ARG_BACK, card?.back.orEmpty())
                }
            }
        }
    }
}
