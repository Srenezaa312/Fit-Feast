package com.example.fitfeast

import android.app.DatePickerDialog
import android.app.Dialog
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.fitfeast.databinding.DialogAddMedicationBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class AddMedicationDialogFragment : DialogFragment() {

    interface MedicationUpdateListener {
        fun onMedicationUpdated()
    }

    private var _binding: DialogAddMedicationBinding? = null
    private val binding get() = _binding!!
    private var medicationId: String? = null

    var updateListener: MedicationUpdateListener? = null

    companion object {
        const val MEDICATION_ID = "medication_id"

        fun newInstance(medicationId: String?): AddMedicationDialogFragment {
            val fragment = AddMedicationDialogFragment()
            val args = Bundle()
            args.putString(MEDICATION_ID, medicationId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            medicationId = it.getString(MEDICATION_ID)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddMedicationBinding.inflate(layoutInflater)

        setupInstructionsDropdown()

        binding.startDateEditText.setOnClickListener {
            showDatePickerDialog()
        }

        // Setup chip selection listener for each chip
        setupChipGroupListener()

        // Check if we're in edit mode and fetch existing medication data
        medicationId?.let {
            fetchAndPopulateMedicationData(it)
        }

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (medicationId == null) "Add Medication" else "Edit Medication")
            .setView(binding.root)
            .setPositiveButton(if (medicationId == null) "Add" else "Update", null)
            .setNegativeButton("Cancel", null)

        medicationId?.let {
            dialogBuilder.setNeutralButton("Delete") { _, _ ->
                showDeleteConfirmationDialog(it)
            }
        }

        val dialog = dialogBuilder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                attemptToSaveMedication(dialog)
            }
        }

        return dialog
    }


    private fun setupChipGroupListener() {
        val chipGroup = binding.daysChipGroup
        for (index in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(index) as? Chip
            chip?.setOnCheckedChangeListener { _, _ ->
                // Call updateChipGroupErrorVisibility every time any chip's checked state changes
                updateChipGroupErrorVisibility()
            }
        }
    }

    private fun updateChipGroupErrorVisibility() {
        val anyChipChecked = (0 until binding.daysChipGroup.childCount).any { index ->
            (binding.daysChipGroup.getChildAt(index) as? Chip)?.isChecked == true
        }
        binding.chipGroupError.visibility = if (anyChipChecked) View.GONE else View.VISIBLE
    }



    private fun showDeleteConfirmationDialog(medicationId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Medication")
            .setMessage("Are you sure you want to delete this medication?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMedication(medicationId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMedication(medicationId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("medications").document(medicationId).delete()
            .addOnSuccessListener {
                Log.d("DeleteMedication", "Showing toast for successful deletion.")
                // Use isAdded to check if Fragment is currently added to its activity
                if (isAdded) {
                    Toast.makeText(context, "Medication deleted successfully", Toast.LENGTH_SHORT).show()
                    updateListener?.onMedicationUpdated()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error deleting medication: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }



    private fun validateChipSelection(): Boolean {
        val chipGroup = binding.daysChipGroup
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                return true
            }
        }
        return false
    }
    private fun getSelectedDays(): List<String> {
        val selectedDays = mutableListOf<String>()
        val chipGroup = binding.daysChipGroup
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedDays.add(chip.text.toString())
            }
        }
        return selectedDays
    }

    private fun setupInstructionsDropdown() {
        val instructionsAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.instructions_options,
            android.R.layout.simple_dropdown_item_1line
        )
        binding.instructionsSpinner.apply {
            setAdapter(instructionsAdapter)
            onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
                // Clear error when an item is selected
                binding.instructionsLayout.error = null
            }
        }
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // Create a calendar instance with the selected date
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

            // Format the date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = dateFormat.format(selectedCalendar.time)

            // Set the formatted date to the startDateEditText
            binding.startDateEditText.setText(selectedDate)
        }, year, month, day).apply {
            show()
        }
    }



    private fun fetchAndPopulateMedicationData(medicationId: String) {
        // Fetch medication data from Firestore
        FirebaseFirestore.getInstance().collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            .collection("medications").document(medicationId).get()
            .addOnSuccessListener { documentSnapshot ->
                val medication = documentSnapshot.toObject(Medication::class.java)
                medication?.let {
                    // Populate form fields with fetched data
                    binding.medicationNameEditText.setText(it.name)
                    binding.startDateEditText.setText(it.startDate)
                    binding.instructionsSpinner.setText(it.instructions, false)
                    binding.pillQuantityEditText.setText(it.pillQuantity.toString())
                    binding.notesEditText.setText(it.notes)

                    // Highlight corresponding chips for the repeatDays
                    val chipGroup = binding.daysChipGroup
                    chipGroup.clearCheck()
                    it.repeatDays.forEach { day ->
                        for (i in 0 until chipGroup.childCount) {
                            val chip = chipGroup.getChildAt(i) as Chip
                            if (chip.tag.toString().equals(day, ignoreCase = true)) {
                                chip.isChecked = true
                                break
                            }
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("FetchMedication", "Error fetching medication details: ${e.message}", e)
            }
    }



    private fun attemptToSaveMedication(dialog: Dialog) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not identified", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear previous errors
        binding.medicationNameLayout.error = null
        binding.startDateLayout.error = null
        binding.instructionsLayout.error = null // Clear error for instructions
        binding.pillQuantityLayout.error = null

        // Extract values directly from the form
        val name = binding.medicationNameEditText.text.toString().trim()
        val startDate = binding.startDateEditText.text.toString().trim()
        val instructions = binding.instructionsSpinner.text.toString()
        val pillQuantityText = binding.pillQuantityEditText.text.toString().trim()
        val notes = binding.notesEditText.text.toString().trim()

        // Flag to indicate if there are any validation errors
        var hasError = false

        // Validate chip selection
        if (!validateChipSelection()) {
            binding.chipGroupError.visibility = View.VISIBLE // Show error
            hasError = true
        } else {
            binding.chipGroupError.visibility = View.GONE // Hide error if valid selection
        }

        // Validate instructions selection
        if (instructions.isEmpty()) {
            binding.instructionsLayout.error = "Instructions are required"
            hasError = true
        }

        // Basic validation with inline error messages
        if (name.isEmpty()) {
            binding.medicationNameLayout.error = "Medication name is required"
            hasError = true
        }

        if (startDate.isEmpty()) {
            binding.startDateLayout.error = "Start date is required"
            hasError = true
        }

        if (pillQuantityText.isEmpty()) {
            binding.pillQuantityLayout.error = "Pill quantity is required"
            hasError = true
        } else {
            val pillQuantity = pillQuantityText.toIntOrNull()
            if (pillQuantity == null || pillQuantity <= 0) {
                binding.pillQuantityLayout.error = "Pill quantity must be a positive number"
                hasError = true
            }
        }

        // Stop the method if there are validation errors
        if (hasError) return

        // Assuming pillQuantityText is not empty and is a valid integer at this point
        val pillQuantity = pillQuantityText.toInt()

        // Retrieve selected days using the getSelectedDays() method
        val selectedDays = getSelectedDays()

        // Create the medication object
        val medication = Medication(
            id = medicationId ?: "",
            name = name,
            startDate = startDate,
            instructions = instructions,
            pillQuantity = pillQuantity,
            repeatDays = selectedDays,
            notes = notes
        )

        // Determine whether to add a new medication or update an existing one
        if (medicationId == null) {
            addMedicationToFirestore(userId, medication, dialog)
        } else {
            updateMedicationInFirestore(userId, medicationId ?: "", medication, dialog)
        }
    }


    private fun addMedicationToFirestore(userId: String, medication: Medication, dialog: Dialog) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("medications").add(medication)
            .addOnSuccessListener {
                if (isAdded) { // Check if the fragment is currently added to its activity
                    Toast.makeText(context, "Medication added successfully.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Explicitly dismiss the dialog on success
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Failed to add medication: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun updateMedicationInFirestore(userId: String, medicationId: String, medication: Medication, dialog: Dialog) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("medications").document(medicationId)
            .set(medication)
            .addOnSuccessListener {
                updateListener?.onMedicationUpdated()
                if (isAdded) {
                    Toast.makeText(context, "Medication updated successfully.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Explicitly dismiss the dialog on success
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Failed to update medication: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}