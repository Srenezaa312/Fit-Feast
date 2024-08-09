package com.example.fitfeast

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fitfeast.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlin.math.max


class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context as? MainActivity)?.setDrawerLocked(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchMedications()
        setupMedicationCardClickListener()
        fetchLatestWeight()
        setupWeightCardClickListener()
        fetchWaterIntakeData()
        fetchCalorieIntakeData()
        fetchNews()

        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.dashboard)

        binding.cardViewWaterIntake.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_waterIntakeManagementFragment)
        }
        binding.cardViewCaloriesIntake.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_caloriesManagementFragment)
        }
    }


    private fun fetchNews() {
        lifecycleScope.launch {
            try {
                val newsResponse = RetrofitInstance.api.getEverything(apiKey = "a275ac9dc51b4c989ab5a7e77ee714ea", query = "health, fitness, exercise")
                if (newsResponse.status == "ok" && newsResponse.articles.isNotEmpty()) {
                    Log.d("DashboardFragment", "Articles fetched: ${newsResponse.articles.size}")
                    activity?.runOnUiThread {
                        setupNewsViewPager(newsResponse.articles)
                    }
                } else {
                    Log.e("DashboardFragment", "No articles received or status not OK.")
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error fetching news", e)
            }
        }
    }

    private fun updateMedicationCount(medicationsCount: Int) {
        val medicationText = "$medicationsCount"
        binding.medicationsCountTextView.text = medicationText
    }

    private fun fetchMedications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("medications")
            .get()
            .addOnSuccessListener { documents ->
                val medicationsCount = documents.size()
                updateMedicationCount(medicationsCount)
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching medications", e)
            }
    }
    private fun setupMedicationCardClickListener() {
        binding.cardViewMedication.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_medicationFragment)
        }
    }



    private fun setupNewsViewPager(articles: List<Article>) {
        val adapter = NewsArticleAdapter(articles)
        binding.newsViewPager.adapter = adapter
        // Link ViewPager2 with DotsIndicator
        binding.dotsIndicator.setViewPager2(binding.newsViewPager)
    }

    private fun fetchLatestWeight() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("weights")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.documents.isNotEmpty()) {
                    val weight = documents.documents.first().getDouble("weight") ?: return@addOnSuccessListener
                    updateWeightText(weight)
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching latest weight", e)
            }
    }

    private fun updateWeightText(weight: Double) {
        val weightText = " ${String.format("%.2f", weight)} kg"
        binding.textViewCurrentWeight.text = weightText
    }

    private fun setupWeightCardClickListener() {
        binding.cardViewWeight.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_weightManagementFragment)
        }
    }
    private fun fetchWaterIntakeData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("waterIntakeData")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("DashboardFragment", "No water intake data found")
                    // Handle the case where there is no data
                } else {
                    val latestRecord = documents.documents.first()
                    val currentIntake = latestRecord.getDouble("waterIntake") ?: 0.0
                    val remainingIntake = latestRecord.getDouble("remainingWaterIntake") ?: 0.0
                    updateWaterIntakeUI(currentIntake, remainingIntake)
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching water intake data", e)
            }
    }

    private fun updateWaterIntakeUI(waterIntakeGoal: Double, remainingIntake: Double) {
        val consumedWater = waterIntakeGoal - remainingIntake
        val progress = if (waterIntakeGoal > 0) {
            ((consumedWater / waterIntakeGoal) * 100).toInt()
        } else {
            0
        }

        // Ensure consumed water is not negative
        val displayedConsumedWater = max(consumedWater, 0.0)

        binding.textViewCurrentIntake.text = "${String.format("%.2f", displayedConsumedWater)}"
        binding.textViewTotalGoal.text = "${String.format("%.2f", waterIntakeGoal)}"
        binding.progressBarWaterIntake.progress = progress
    }


    private fun fetchCalorieIntakeData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("nutritionData")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("DashboardFragment", "No calorie intake data found")
                    // Handle the case where there is no data
                } else {
                    val latestRecord = documents.documents.first()
                    val caloriesGoal = latestRecord.getDouble("calories") ?: 0.0
                    val remainingCalories = latestRecord.getDouble("remainingCalories") ?: caloriesGoal
                    val consumedCalories = caloriesGoal - remainingCalories
                    updateCalorieIntakeUI(caloriesGoal, consumedCalories)
                }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching calorie intake data", e)
            }
    }

    private fun updateCalorieIntakeUI(caloriesGoal: Double, consumedCalories: Double) {
        val progress = if (caloriesGoal > 0) {
            ((consumedCalories / caloriesGoal) * 100).toInt()
        } else {
            0
        }
        activity?.runOnUiThread {
            binding.textViewCurrentCaloriesValue.text = String.format("%.2f", consumedCalories)
            binding.textViewCaloriesGoal.text = String.format("%.2f", caloriesGoal)
            binding.progressBarCaloriesIntake.progress = progress
        }
    }


    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.dashboard)
    }

    override fun onDetach() {
        super.onDetach()
        // Directly lock the drawer when fragment is detached
        (activity as? MainActivity)?.setDrawerLocked(true)
    }
}
