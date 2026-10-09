package com.entitycoder.flashdecks.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.entitycoder.flashdecks.R
import com.entitycoder.flashdecks.data.Card

/**
 * RecyclerView adapter for a deck's cards. Each row previews front -> back.
 * Tap edits the card; long-press offers deletion.
 */
class CardAdapter(
    private val onClick: (Card) -> Unit,
    private val onLongClick: (Card) -> Unit
) : ListAdapter<Card, CardAdapter.CardViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onLongClick)
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val frontView: TextView = itemView.findViewById(R.id.card_front)
        private val backView: TextView = itemView.findViewById(R.id.card_back)

        fun bind(card: Card, onClick: (Card) -> Unit, onLongClick: (Card) -> Unit) {
            frontView.text = card.front
            backView.text = card.back
            itemView.setOnClickListener { onClick(card) }
            itemView.setOnLongClickListener {
                onLongClick(card)
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Card>() {
            override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean =
                oldItem == newItem
        }
    }
}
