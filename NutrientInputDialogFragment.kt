package com.example.fitfeast

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NutrientInputDialogFragment : DialogFragment() {

    // Define variables for input fields and text views.
    private var caloriesInput: EditText? = null
    private var fatPercentageInput: EditText? = null
    private var carbsPercentageInput: EditText? = null
    private var proteinPercentageInput: EditText? = null
    private var fatGramsTextView: TextView? = null
    private var carbsGramsTextView: TextView? = null
    private var proteinGramsTextView: TextView? = null

    private lateinit var errorMessageTextView: TextView

    // Define a listener interface for communication with the host fragment or activity.
    private var listener: NutrientInputListener? = null

    interface NutrientInputListener {
        fun onUpdateNutrientInput(
            calories: Double,
            fatGrams: Double,
            carbsGrams: Double,
            proteinGrams: Double
        )
    }

    fun setNutrientInputListener(listener: NutrientInputListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.nutrient_input_dialog, null)

        initializeViews(view)
        setupTextWatchers()

        // Fetch and display user data if available
        fetchAndDisplayUserData()

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Nutrient Intake")
            .setView(view)
            .setPositiveButton("Update") { dialog, _ ->
                // Extract the values from EditText fields
                val calories = caloriesInput?.text.toString().toDoubleOrNull() ?: 0.0
                val fatPercentage = fatPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
                val carbsPercentage = carbsPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
                val proteinPercentage = proteinPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0

                // Calculate the total percentage
                val totalPercentage = fatPercentage + carbsPercentage + proteinPercentage
                if (totalPercentage == 100.0) {
                    // If valid, calculate grams and notify listener
                    val fatGrams = calculateFatGrams(calories, fatPercentage)
                    val carbsGrams = calculateCarbsGrams(calories, carbsPercentage)
                    val proteinGrams = calculateProteinGrams(calories, proteinPercentage)

                    listener?.onUpdateNutrientInput(calories, fatGrams, carbsGrams, proteinGrams)
                } else {
                    // If invalid, show an error and prevent the dialog from closing
                    Toast.makeText(context, "Total percentage must equal 100%. Please adjust the values.", Toast.LENGTH_LONG).show()
                    dialog.cancel() // Keep the dialog open for corrections
                }
            }
            .setNegativeButton("Cancel", null)

        return dialogBuilder.create()
    }



    // Initializes the view components by finding them in the layout.
    private fun initializeViews(view: View) {
        caloriesInput = view.findViewById(R.id.caloriesInput)
        fatPercentageInput = view.findViewById(R.id.fatPercentageInput)
        carbsPercentageInput = view.findViewById(R.id.carbsPercentageInput)
        proteinPercentageInput = view.findViewById(R.id.proteinPercentageInput)
        fatGramsTextView = view.findViewById(R.id.fatGrams)
        carbsGramsTextView = view.findViewById(R.id.carbsGrams)
        proteinGramsTextView = view.findViewById(R.id.proteinGrams)
        errorMessageTextView = view.findViewById(R.id.errorMessageTextView)
    }

    // Sets up text watchers for the input fields to enable real-time calculation.
    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateInputFields()
                calculateAndDisplayMacronutrients()
            }
        }

        caloriesInput?.addTextChangedListener(textWatcher)
        fatPercentageInput?.addTextChangedListener(textWatcher)
        carbsPercentageInput?.addTextChangedListener(textWatcher)
        proteinPercentageInput?.addTextChangedListener(textWatcher)
    }

    // Method to validate and update the enable state of input fields
    private fun updateInputFields() {
        val fatPercentage = fatPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
        val carbsPercentage = carbsPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
        val proteinPercentage = proteinPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0

        val totalPercentage = fatPercentage + carbsPercentage + proteinPercentage

        // Initially, assume all fields can be edited
        fatPercentageInput?.isEnabled = true
        carbsPercentageInput?.isEnabled = true
        proteinPercentageInput?.isEnabled = true

        // Hide the error message by default
        errorMessageTextView.visibility = View.GONE

        // Logic to enable or disable fields based on total percentage
        if (totalPercentage == 100.0) {
            if (fatPercentage == 0.0) fatPercentageInput?.isEnabled = true
            if (carbsPercentage == 0.0) carbsPercentageInput?.isEnabled = true
            if (proteinPercentage == 0.0) proteinPercentageInput?.isEnabled = true
        } else if (totalPercentage > 100) {
            // Show error message if the total percentage exceeds 100%
            errorMessageTextView.visibility = View.VISIBLE
            errorMessageTextView.text = "Total percentage cannot exceed 100%. Please adjust the values."
        }

        // Disable other fields if one is set to 100%
        if (fatPercentage == 100.0) {
            carbsPercentageInput?.isEnabled = false
            proteinPercentageInput?.isEnabled = false
        } else if (carbsPercentage == 100.0) {
            fatPercentageInput?.isEnabled = false
            proteinPercentageInput?.isEnabled = false
        } else if (proteinPercentage == 100.0) {
            fatPercentageInput?.isEnabled = false
            carbsPercentageInput?.isEnabled = false
        }
    }


    // Validates the input values before processing.
    private fun validateInputs(): Boolean {
        val calories = caloriesInput?.text.toString().toDoubleOrNull()
        if (calories == null || calories <= 0.0) {
            Toast.makeText(context, "Please enter a valid calorie amount.", Toast.LENGTH_SHORT).show()
            return false
        }

        val fatPercentage = fatPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
        val carbsPercentage = carbsPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
        val proteinPercentage = proteinPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0

        val totalPercentage = fatPercentage + carbsPercentage + proteinPercentage
        if (totalPercentage != 100.0) {
            Toast.makeText(context, "The sum of macronutrient percentages must equal 100%.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }


    // Calculates and displays macronutrients based on input values.
    private fun calculateAndDisplayMacronutrients() {
        val calories = caloriesInput?.text.toString().toDoubleOrNull() ?: 0.0
        val fatPercentage = fatPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
        val carbsPercentage = carbsPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0
        val proteinPercentage = proteinPercentageInput?.text.toString().toDoubleOrNull() ?: 0.0

        val fatGrams = calculateFatGrams(calories, fatPercentage)
        val carbsGrams = calculateCarbsGrams(calories, carbsPercentage)
        val proteinGrams = calculateProteinGrams(calories, proteinPercentage)

        fatGramsTextView?.text = String.format("Fat (gm): %.2f", fatGrams)
        carbsGramsTextView?.text = String.format("Carbs (gm): %.2f", carbsGrams)
        proteinGramsTextView?.text = String.format("Protein (gm): %.2f", proteinGrams)
    }

    // Calculation functions for macronutrients
    private fun calculateCarbsGrams(calories: Double, carbsPercentage: Double) =
        (calories * carbsPercentage / 100) / 4

    private fun calculateProteinGrams(calories: Double, proteinPercentage: Double) =
        (calories * proteinPercentage / 100) / 4

    private fun calculateFatGrams(calories: Double, fatPercentage: Double) =
        (calories * fatPercentage / 100) / 9


    // Fetches and displays user data if available. This method should be called after initializing views.
    private fun fetchAndDisplayUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("nutritionData").orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("NutritionData", "No nutrition data found for user")
                    return@addOnSuccessListener
                }
                for (document in documents) {
                    val data = document.toObject(NutritionData::class.java)
                    activity?.runOnUiThread {
                        caloriesInput?.setText(data.calories.toString())
                        fatPercentageInput?.setText((data.fatGrams * 9 / data.calories * 100).toString())
                        carbsPercentageInput?.setText((data.carbsGrams * 4 / data.calories * 100).toString())
                        proteinPercentageInput?.setText((data.proteinGrams * 4 / data.calories * 100).toString())
                        calculateAndDisplayMacronutrients()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("NutritionData", "Error fetching user data", e)
                Toast.makeText(context, "Failed to fetch user data.", Toast.LENGTH_SHORT).show()
            }
    }



}

