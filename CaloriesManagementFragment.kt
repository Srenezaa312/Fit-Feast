package com.example.fitfeast

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fitfeast.databinding.FragmentCaloriesManagementBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlin.math.max

class CaloriesManagementFragment : Fragment() {

    private var _binding: FragmentCaloriesManagementBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is MainActivity) {
            throw RuntimeException("$context must be MainActivity")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCaloriesManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUserCaloriesGoal()
        setupIconClickListeners()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.calories_intake_management)

        // Unlock the drawer
        (activity as MainActivity).setDrawerLocked(false)
    }

    private fun fetchUserCaloriesGoal() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val query = FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("nutritionData")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)

        query.get()
            .addOnSuccessListener { documents ->
                if (documents.documents.isNotEmpty()) {
                    val latestDocument = documents.documents.first()
                    val nutritionData = latestDocument.toObject(NutritionData::class.java)
                    val caloriesGoal = nutritionData?.calories ?: 0.0
                    var remainingCalories = latestDocument.getDouble("remainingCalories") ?: caloriesGoal

                    if (latestDocument.getDouble("remainingCalories") == null) {
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .collection("nutritionData").document(latestDocument.id)
                            .update("remainingCalories", caloriesGoal)
                        remainingCalories = caloriesGoal
                    }

                    binding.tvCaloriesIntakeGoal.text = getString(R.string.calories_intake_goal, caloriesGoal)
                    binding.tvRemainingCalories.text = getString(R.string.remaining_calories_to_go, remainingCalories)
                    updateProgressBar(caloriesGoal, remainingCalories)
                } else {
                    Toast.makeText(context, "No nutrition data available. Set your goals.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching nutrition data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun updateProgressBar(caloriesGoal: Double, remainingCalories: Double) {
        val consumedCalories = caloriesGoal - remainingCalories
        val progress = if (caloriesGoal > 0) {
            ((consumedCalories / caloriesGoal) * 100).toInt()
        } else {
            0
        }
        binding.caloriesLevelProgress.progress = progress
    }


    private fun setupIconClickListeners() {
        binding.imgFoodIcon180kcal.setOnClickListener { subtractCalories(180.0) }
        binding.imgFoodIcon350kcal.setOnClickListener { subtractCalories(350.0) }
        binding.imgFoodIcon500kcal.setOnClickListener { subtractCalories(500.0) }
        binding.imgFoodIcon1000kcal.setOnClickListener { subtractCalories(1000.0) }
    }


    private fun subtractCalories(amount: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val query = FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("nutritionData").orderBy("timestamp", Query.Direction.DESCENDING).limit(1)

        query.get().addOnSuccessListener { documents ->
            if (documents.documents.isNotEmpty()) {
                val documentRef = documents.documents.first().reference

                FirebaseFirestore.getInstance().runTransaction { transaction ->
                    val snapshot = transaction.get(documentRef)
                    val currentRemaining = snapshot.getDouble("remainingCalories") ?: snapshot.getDouble("calories") ?: 0.0
                    val newRemaining = max(0.0, currentRemaining - amount)
                    transaction.update(documentRef, "remainingCalories", newRemaining)
                    newRemaining
                }.addOnSuccessListener { newRemaining ->
                    updateUI(newRemaining as Double, documentRef.id)
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Error updating calories: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No recent nutrition data found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to fetch nutrition data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(newRemaining: Double, documentId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val documentReference = FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("nutritionData").document(documentId)

        documentReference.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val caloriesGoal = document.getDouble("calories") ?: 0.0
                binding.tvRemainingCalories.text = getString(R.string.remaining_calories_to_go, newRemaining)
                updateProgressBar(caloriesGoal, newRemaining)

                if (newRemaining == 0.0) {
                    Toast.makeText(context, "Congratulations! You've reached your calorie intake goal for today!", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error fetching updated calories data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDetach() {
        super.onDetach()
        // Directly use MainActivity's method to lock the drawer
        (activity as MainActivity).setDrawerLocked(true)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
