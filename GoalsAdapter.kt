package com.example.fitfeast

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalsAdapter(private val goalsList: List<Goal>, private val listener: OnGoalClickListener) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    interface OnGoalClickListener {
        fun onGoalClick(goal: Goal)
        fun onWaterIntakeClick()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(itemView, listener, goalsList)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val currentGoal = goalsList[position]
        holder.bind(currentGoal)
    }

    override fun getItemCount() = goalsList.size

    class GoalViewHolder(itemView: View, private val listener: OnGoalClickListener, private val goalsList: List<Goal>) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.goalTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.goalDescription)
        private val waterIntakeValueTextView: TextView = itemView.findViewById(R.id.waterIntakeValue)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onGoalClick(goalsList[position])
                }
            }
        }

        fun bind(goal: Goal) {
            Log.d("GoalViewHolder", "Binding view for: ${goal.title}")

            titleTextView.text = goal.title
            when (goal.title) {
                "Water Intake" -> {
                    goal.waterIntake?.let { waterIntake ->
                        // Adjust the format specifier to handle the Double type
                        descriptionTextView.text = itemView.context.getString(R.string.water_intake_goal, waterIntake)
                        waterIntakeValueTextView.visibility = View.GONE
                    } ?: run {
                        descriptionTextView.text = itemView.context.getString(R.string.keep_track_of_hydration)
                        waterIntakeValueTextView.visibility = View.GONE
                    }
                }
                else -> {
                    descriptionTextView.text = goal.description
                    waterIntakeValueTextView.visibility = View.GONE
                }
            }
        }


    }
}
