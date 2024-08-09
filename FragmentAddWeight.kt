package com.example.fitfeast

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.fitfeast.databinding.FragmentAddWeightBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.navigation.fragment.findNavController


class FragmentAddWeight : Fragment() {

    private var _binding: FragmentAddWeightBinding? = null
    private val binding get() = _binding!!
    private lateinit var userProfile: UserProfile

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.title = "Add Weight"

        setupDateTimePickers()
        fetchUserProfile {
            setupWeightInputListener()
            loadPreviousWeight()
        }
        binding.fabSaveWeight.setOnClickListener { saveNewWeight() }
        // Handle navigation back to WeightManagementFragment
        binding.fabBackToWeightManagement.setOnClickListener {
            findNavController().navigate(R.id.action_fragmentAddWeight_to_weightManagementFragment)
        }
    }

    private fun fetchUserProfile(onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                userProfile = documentSnapshot.toObject(UserProfile::class.java) ?: UserProfile()
                onComplete()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load user profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDateTimePickers() {
        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.textViewDate.text = dateFormat.format(currentDate.time)
        binding.textViewTime.text = timeFormat.format(currentDate.time)

        binding.imageViewCalendar.setOnClickListener {
            DatePickerDialog(requireContext(), DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                currentDate.set(year, month, dayOfMonth)
                binding.textViewDate.text = dateFormat.format(currentDate.time)
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.imageViewTime.setOnClickListener {
            TimePickerDialog(requireContext(), TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                currentDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                currentDate.set(Calendar.MINUTE, minute)
                binding.textViewTime.text = timeFormat.format(currentDate.time)
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show()
        }
    }

    private fun setupWeightInputListener() {
        binding.editTextNewWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val weightStr = s.toString()
                if (weightStr.isNotEmpty()) {
                    val weight = weightStr.toDouble()
                    calculateAndDisplayMetrics(weight)
                }
            }
        })
    }

    private fun calculateAndDisplayMetrics(currentWeight: Double) {
        userProfile.height?.let { height ->
            val bmi = calculateBMI(currentWeight, height)
            binding.textViewBMIValue.text = String.format("%.2f", bmi)

            val bodyFatPercentage = calculateBodyFat(bmi, userProfile.age, userProfile.gender)
            binding.textViewBodyFatValue.text = String.format("%.2f%%", bodyFatPercentage)

            // Assume userProfile.previousWeight holds the correct previous weight
            val percentageOfChange = if (userProfile.previousWeight != 0.0) {
                calculatePercentageOfChange(currentWeight, userProfile.previousWeight)
            } else {
                0.0 // If there's no previous weight, percentage change is 0
            }
            binding.textViewChangePercentageValue.text = String.format("%.2f%%", percentageOfChange)
        }
    }



    private fun calculateBMI(weight: Double, height: Double): Double {
        val heightInMeters = height / 100
        return weight / (heightInMeters * heightInMeters)
    }

    private fun calculateBodyFat(bmi: Double, age: Int, gender: String): Double {
        return if (gender == "Male") {
            (1.20 * bmi) + (0.23 * age) - 16.2
        } else {
            (1.20 * bmi) + (0.23 * age) - 5.4
        }
    }

    private fun calculatePercentageOfChange(currentWeight: Double, previousWeight: Double): Double {
        if (previousWeight == 0.0) return 0.0

        val percentageChange = ((currentWeight - previousWeight) / previousWeight) * 100

        Log.d("WeightChange", "Current Weight: $currentWeight, Previous Weight: $previousWeight, % of Change: $percentageChange")

        return percentageChange
    }


    private fun loadPreviousWeight() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("weights")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.documents.isNotEmpty()) {
                    val weights = documents.documents.mapNotNull { it.getDouble("weight") }
                    val currentWeight = weights.firstOrNull() ?: return@addOnSuccessListener

                    // The previous weight is either the second entry or the current weight if there is only one entry
                    val previousWeight = if (weights.size > 1) weights[1] else currentWeight

                    userProfile.weight = currentWeight
                    userProfile.previousWeight = previousWeight

                    // Update the previous weight display
                    binding.textViewPreviousWeight.text = "Previous Weight: $previousWeight kg"

                    // Calculate and display metrics with the current (latest) and previous weights
                    calculateAndDisplayMetrics(userProfile.weight)
                } else {
                    // No previous weight, can't calculate percentage of change
                    userProfile.weight = 0.0
                    userProfile.previousWeight = 0.0
                    binding.textViewPreviousWeight.text = "Previous Weight: N/A"
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load previous weight", Toast.LENGTH_SHORT).show()
            }
    }



    private fun saveNewWeight() {
        val newWeightStr = binding.editTextNewWeight.text.toString()
        if (newWeightStr.isBlank()) {
            Toast.makeText(context, "Please enter a weight", Toast.LENGTH_SHORT).show()
            return
        }

        val newWeight = newWeightStr.toDoubleOrNull()
        if (newWeight == null || newWeight == 0.0) {
            Toast.makeText(context, "Invalid weight", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate BMI and Body Fat %
        val bmi = calculateBMI(newWeight, userProfile.height)
        val bodyFatPercentage = calculateBodyFat(bmi, userProfile.age, userProfile.gender)

        // Create an instance of WeightRecord
        val weightRecord = WeightRecord(
            weight = newWeight,
            date = binding.textViewDate.text.toString(),
            time = binding.textViewTime.text.toString(),
            bmi = bmi,
            bodyFatPercentage = bodyFatPercentage,
            timestamp = FieldValue.serverTimestamp()
        )

        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("weights")
            .add(weightRecord.toMap()) // You'll need to create a toMap method in your WeightRecord class
            .addOnSuccessListener {
                Toast.makeText(context, "Weight saved successfully", Toast.LENGTH_SHORT).show()
                // Additional logic after success...
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save weight: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
