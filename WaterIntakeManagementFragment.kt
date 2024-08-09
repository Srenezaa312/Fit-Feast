package com.example.fitfeast

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fitfeast.databinding.FragmentWaterIntakeManagementBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.math.max

class WaterIntakeManagementFragment : Fragment() {

    private var _binding: FragmentWaterIntakeManagementBinding? = null
    private val binding get() = _binding!!

    private var waterIntakeGoal: Double = 0.0
    private var remainingWaterIntake: Double = 0.0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is MainActivity) {
            throw RuntimeException("$context must be MainActivity")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWaterIntakeManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUserWaterIntakeGoal()
        setupIconClickListeners()

        // Update ActionBar title
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.water_intake_management)

        // Unlock the drawer
        (activity as MainActivity).setDrawerLocked(false)
    }

    private fun fetchUserWaterIntakeGoal() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: let {
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize progress bar to 0
        binding.waterLevelProgress.progress = 0

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("waterIntakeData").orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val latestRecord = documents.documents.first()
                    waterIntakeGoal = latestRecord.getDouble("waterIntake") ?: 8.0
                    remainingWaterIntake = latestRecord.getDouble("remainingWaterIntake") ?: waterIntakeGoal

                    binding.tvWaterIntakeGoal.text = getString(R.string.water_intake_goal, waterIntakeGoal)
                    binding.tvRemainingWater.text = getString(R.string.remaining_water_to_go, remainingWaterIntake)

                    // Now passing the remainingWaterIntake to updateProgressBar
                    updateProgressBar(remainingWaterIntake)
                } else {
                    Toast.makeText(context, "No water intake data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching water intake goal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun setupIconClickListeners() {
        binding.imgWaterDrop180ml.setOnClickListener { subtractWaterIntake(180.0) }
        binding.imgWaterDrop350ml.setOnClickListener { subtractWaterIntake(350.0) }
        binding.imgWaterDrop500ml.setOnClickListener { subtractWaterIntake(500.0) }
        binding.imgWaterDrop1000ml.setOnClickListener { subtractWaterIntake(1000.0) }
    }

    private fun subtractWaterIntake(amount: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: let {
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val documentReference = FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("waterIntakeData").document(currentDate)

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(documentReference)
            val currentRemaining = snapshot.getDouble("remainingWaterIntake") ?: 0.0
            val newRemaining = max(0.0, currentRemaining - (amount / 1000))

            transaction.update(documentReference, "remainingWaterIntake", newRemaining)

            // Returning new remaining for UI update
            newRemaining
        }.addOnSuccessListener { newRemaining ->
            updateUI(newRemaining)
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error updating water intake: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProgressBar(remainingWaterIntake: Double) {
        val consumedWater = waterIntakeGoal - remainingWaterIntake
        val progress = ((consumedWater / waterIntakeGoal) * 100).toInt()
        binding.waterLevelProgress.progress = progress
    }

    private fun updateUI(remainingWaterIntake: Double) {
        binding.tvRemainingWater.text = getString(R.string.remaining_water_to_go, remainingWaterIntake)
        if (remainingWaterIntake <= 0) {
            Toast.makeText(context, "Congratulations! You've reached your water intake goal for today!", Toast.LENGTH_LONG).show()
        }
        updateProgressBar(remainingWaterIntake)
    }


    private fun saveUpdatedWaterIntake() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: let {
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val waterIntakeData = hashMapOf(
            "timestamp" to com.google.firebase.Timestamp.now(),
            "remainingWaterIntake" to remainingWaterIntake
        )

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("waterIntakeData").document("latest")
            .set(waterIntakeData)
            .addOnSuccessListener {
                Toast.makeText(context, "Water intake updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating water intake: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
