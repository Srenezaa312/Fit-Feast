package com.example.fitfeast

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitfeast.databinding.FragmentUserProfileCreationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

class UserProfileCreationFragment : Fragment() {

    private var _binding: FragmentUserProfileCreationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserProfileCreationBinding.inflate(inflater, container, false)
        setupGenderSpinner()
        setupActivityLevelSpinner()
        setupDOBPicker()
        setupSubmitButton()
        setupTextChangeListeners()
        checkFormValidity()
        return binding.root
    }

    private fun setupTextChangeListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkFormValidity()
            }
        }

        binding.nameEditText.addTextChangedListener(textWatcher)
        binding.dobEditText.addTextChangedListener(textWatcher)
        binding.weightEditText.addTextChangedListener(textWatcher)
        binding.heightEditText.addTextChangedListener(textWatcher)
        (binding.genderSpinner as? AutoCompleteTextView)?.addTextChangedListener(textWatcher)
        (binding.activityLevelSpinner as? AutoCompleteTextView)?.addTextChangedListener(textWatcher)
    }

    private fun checkFormValidity() {
        val isFormValid = binding.nameEditText.text?.isNotBlank() == true &&
                binding.dobEditText.text?.isNotBlank() == true &&
                (binding.genderSpinner as? AutoCompleteTextView)?.text?.isNotBlank() == true &&
                binding.weightEditText.text?.isNotBlank() == true &&
                binding.heightEditText.text?.isNotBlank() == true &&
                (binding.activityLevelSpinner as? AutoCompleteTextView)?.text?.isNotBlank() == true

        binding.submitProfileButton.isEnabled = isFormValid
    }


    private fun setupGenderSpinner() {
        val genderAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gender_options,
            android.R.layout.simple_dropdown_item_1line
        )
        (binding.genderSpinner as? AutoCompleteTextView)?.setAdapter(genderAdapter)
    }

    private fun setupActivityLevelSpinner() {
        val activityLevelAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.activity_level_options,
            android.R.layout.simple_dropdown_item_1line
        )
        (binding.activityLevelSpinner as? AutoCompleteTextView)?.setAdapter(activityLevelAdapter)
    }

    private fun setupDOBPicker() {
        binding.dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%d/%d/%d", month + 1, dayOfMonth, year) // Adjusting format for consistency
                binding.dobEditText.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSubmitButton() {
        binding.submitProfileButton.isEnabled = false

        // Set up text change listeners for form validation
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                checkFormValidity()
            }

            override fun afterTextChanged(s: Editable) {
            }
        }

        // Apply the text change listener to all EditText fields
        binding.nameEditText.addTextChangedListener(afterTextChangedListener)
        binding.dobEditText.addTextChangedListener(afterTextChangedListener)
        binding.weightEditText.addTextChangedListener(afterTextChangedListener)
        binding.heightEditText.addTextChangedListener(afterTextChangedListener)

        (binding.genderSpinner as? AutoCompleteTextView)?.addTextChangedListener(afterTextChangedListener)
        (binding.activityLevelSpinner as? AutoCompleteTextView)?.addTextChangedListener(afterTextChangedListener)

        // Set up the button's click listener
        binding.submitProfileButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val dob = binding.dobEditText.text.toString()
            val gender = (binding.genderSpinner as? AutoCompleteTextView)?.text.toString()
            val weight = binding.weightEditText.text.toString().toDoubleOrNull() ?: 0.0
            val height = binding.heightEditText.text.toString().toDoubleOrNull() ?: 0.0
            val activityLevel = (binding.activityLevelSpinner as? AutoCompleteTextView)?.text.toString()

            val userProfile = UserProfile(name, dob, gender, calculateAge(dob), weight, "kg", height, "cm", activityLevel)
            addUserProfileToFirestore(userProfile)
        }
    }

    private fun addUserProfileToFirestore(userProfile: UserProfile) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).set(userProfile)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate to the Dashboard Fragment
                    findNavController().navigate(R.id.action_userProfileCreationFragment_to_dashboardFragment)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error creating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun calculateAge(dob: String): Int {
        val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        val birthDate = LocalDate.parse(dob, formatter)
        val today = LocalDate.now()
        return Period.between(birthDate, today).years
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
