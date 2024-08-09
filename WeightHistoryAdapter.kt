package com.example.fitfeast

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WeightHistoryAdapter : RecyclerView.Adapter<WeightHistoryAdapter.WeightHistoryViewHolder>() {

    private var historyItems: List<WeightHistoryItem> = emptyList()

    fun submitList(items: List<WeightHistoryItem>) {
        this.historyItems = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weight_history, parent, false)
        return WeightHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeightHistoryViewHolder, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount(): Int = historyItems.size

    class WeightHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewBMI: TextView = itemView.findViewById(R.id.textViewBMI)
        private val textViewBodyFatPercentage: TextView = itemView.findViewById(R.id.textViewBodyFatPercentage)
        private val textViewWeight: TextView = itemView.findViewById(R.id.textViewWeight)

        fun bind(item: WeightHistoryItem) {
            textViewDate.text = item.date
            textViewTime.text = item.time
            textViewBMI.text = String.format("%.2f", item.bmi)
            textViewBodyFatPercentage.text = String.format("%.2f%%", item.bodyFatPercentage)
            textViewWeight.text = String.format("%.2f kg", item.weight)
        }
    }
}
