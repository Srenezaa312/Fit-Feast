package com.example.fitfeast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitfeast.databinding.ItemMedicationBinding

class MedicationAdapter(
    private var medications: List<Medication>,
    private val onItemClicked: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = medications[position]
        holder.bind(medication, onItemClicked)
    }

    override fun getItemCount(): Int = medications.size

    fun updateMedications(medications: List<Medication>) {
        this.medications = medications
        notifyDataSetChanged()
    }

    class MedicationViewHolder(private val binding: ItemMedicationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(medication: Medication, onItemClicked: (Medication) -> Unit) {
            binding.medicationName.text = medication.name
            binding.medicationInstructions.text = medication.instructions
            binding.medicationPillQuantity.text = medication.pillQuantity.toString()
            binding.medicationRepeatDays.text = medication.repeatDays.joinToString(", ")
            binding.medicationStartDate.text = medication.startDate
            binding.medicationNotes.text = medication.notes

            // Handle card tap
            itemView.setOnClickListener { onItemClicked(medication) }
        }
    }

}
