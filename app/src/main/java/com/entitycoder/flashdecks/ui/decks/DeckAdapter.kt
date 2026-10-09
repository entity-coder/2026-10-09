package com.entitycoder.flashdecks.ui.decks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.data.DeckWithCardCount

/**
 * RecyclerView adapter for the deck list. Tap opens the deck; long-press
 * offers deletion. DiffUtil keeps list updates animated and cheap.
 */
class DeckAdapter(
    private val onClick: (DeckWithCardCount) -> Unit,
    private val onLongClick: (DeckWithCardCount) -> Unit
) : ListAdapter<DeckWithCardCount, DeckAdapter.DeckViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onLongClick)
    }

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.deck_name)
        private val countView: TextView = itemView.findViewById(R.id.deck_stats)
        private val bestView: TextView = itemView.findViewById(R.id.deck_best)

        fun bind(
            item: DeckWithCardCount,
            onClick: (DeckWithCardCount) -> Unit,
            onLongClick: (DeckWithCardCount) -> Unit
        ) {
            val resources = itemView.resources
            nameView.text = item.deck.name
            countView.text = resources.getQuantityString(
                R.plurals.cards_count, item.cardCount, item.cardCount
            )
            bestView.text = if (item.deck.bestScore >= 0) {
                resources.getString(R.string.best_score, item.deck.bestScore)
            } else {
                resources.getString(R.string.not_quizzed)
            }
            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeckWithCardCount>() {
            override fun areItemsTheSame(
                oldItem: DeckWithCardCount,
                newItem: DeckWithCardCount
            ): Boolean = oldItem.deck.id == newItem.deck.id

            override fun areContentsTheSame(
                oldItem: DeckWithCardCount,
                newItem: DeckWithCardCount
            ): Boolean = oldItem == newItem
        }
    }
}
