package com.answufeng.store.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

sealed class DemoListRow {
    data class Header(val section: DemoSection) : DemoListRow()
    data class Action(val item: DemoItem) : DemoListRow()
}

class DemoListAdapter(
    private val onItemClick: (DemoItem) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<DemoListRow>()

    fun submitSections(sections: List<DemoSection>) {
        rows.clear()
        sections.forEach { section ->
            rows.add(DemoListRow.Header(section))
            section.items.forEach { rows.add(DemoListRow.Action(it)) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is DemoListRow.Header -> VIEW_HEADER
        is DemoListRow.Action -> VIEW_ACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_HEADER -> HeaderHolder(inflater.inflate(R.layout.item_demo_section, parent, false))
            else -> ActionHolder(inflater.inflate(R.layout.item_demo_action, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is DemoListRow.Header -> {
                (holder as HeaderHolder).title.setText(row.section.titleRes)
            }
            is DemoListRow.Action -> {
                val h = holder as ActionHolder
                val ctx = h.itemView.context
                h.title.setText(row.item.titleRes)
                h.subtitle.setText(row.item.subtitleRes)
                h.card.strokeColor = ctx.getColor(
                    if (row.item.destructive) R.color.error else R.color.card_border,
                )
                h.card.setOnClickListener { onItemClick(row.item) }
            }
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSectionTitle)
    }

    private class ActionHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardAction)
        val title: TextView = view.findViewById(R.id.tvActionTitle)
        val subtitle: TextView = view.findViewById(R.id.tvActionSubtitle)
    }

    companion object {
        private const val VIEW_HEADER = 0
        private const val VIEW_ACTION = 1
    }
}
