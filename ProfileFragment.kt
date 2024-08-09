package com.example.fitfeast

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fitfeast.databinding.ProfileFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class ProfileFragment : Fragment() {

    private var _binding: ProfileFragmentBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private var originalUserProfile: UserProfile? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImageToFirebaseStorage(uri)
            }
        }
    }


    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storage.reference.child("profileImages/$userId.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfileImageUrl(uri.toString()) {
                        // Set the image using Glide
                        activity?.runOnUiThread {
                            Glide.with(this@ProfileFragment)
                                .load(uri)
                                .apply(RequestOptions().centerCrop())
                                .into(binding.profileImageView)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileImageUrl(imageUrl: String, onSuccess: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "Profile image URL updated successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error updating profile image URL", e)
            }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfile()
        setupProfilePictureUpload()
        setupEditIcons()

        binding.editDobIcon.setOnClickListener {
            showDobEditDialog()
        }
        binding.editGenderIcon.setOnClickListener {
            showGenderEditDialog()
        }
        binding.editWeightIcon.setOnClickListener {
            showWeightEditDialog()
        }
        binding.editHeightIcon.setOnClickListener {
            showHeightEditDialog()
        }
        binding.editActivityLevelIcon.setOnClickListener {
            showActivityLevelEditDialog()
        }

        binding.updateProfileButton.setOnClickListener {
            updateProfileInFirestore()
        }
    }

    private fun updateProfileInFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("ProfileFragment", "User ID is null")
            Toast.makeText(context, "Error: User not signed in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if originalUserProfile is null before proceeding
        val userProfile = originalUserProfile
        if (userProfile == null) {
            Log.e("ProfileFragment", "UserProfile is null")
            Toast.makeText(context, "Error: UserProfile is null", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = hashMapOf<String, Any>().apply {
            userProfile.name.takeIf { it.isNotBlank() }?.let { this["name"] = it }
            userProfile.dateOfBirth.takeIf { it.isNotBlank() }?.let { this["dateOfBirth"] = it }
            userProfile.gender.takeIf { it.isNotBlank() }?.let { this["gender"] = it }
            this["weight"] = userProfile.weight
            this["height"] = userProfile.height
            userProfile.activityLevel.takeIf { it.isNotBlank() }?.let { this["activityLevel"] = it }
        }

        if (userUpdates.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(userId).update(userUpdates)
                .addOnSuccessListener {
                    Log.d("ProfileFragment", "Profile updated successfully")
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Error updating profile", e)
                    Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "No changes to update.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userProfile = documentSnapshot.toObject(UserProfile::class.java)
                        userProfile?.let {
                            // Update the UI accordingly
                            binding.nameTextView.text = it.name
                            binding.emailTextView.text = auth.currentUser?.email
                            binding.dobTextView.text = it.dateOfBirth
                            binding.genderTextView.text = it.gender
                            binding.weightTextView.text = "${it.weight} ${it.weightUnit}"
                            binding.heightTextView.text = "${it.height} ${it.heightUnit}"
                            binding.activityLevelTextView.text = it.activityLevel

                            // Save the fetched userProfile to originalUserProfile
                            originalUserProfile = it

                            // Enable the "Update Profile" button as the profile is successfully loaded
                            binding.updateProfileButton.isEnabled = true

                            // Load the profile image
                            Glide.with(this@ProfileFragment)
                                .load(it.profileImageUrl)
                                .apply(RequestOptions().placeholder(R.drawable.ic_add_a_photo).centerCrop())
                                .into(binding.profileImageView)

                            Log.d("ProfileFragment", "User profile loaded successfully")
                        } ?: run {
                            Log.d("ProfileFragment", "User profile is null")
                            Toast.makeText(context, "Error: User profile is null", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "User profile not found.", Toast.LENGTH_SHORT).show()
                        Log.d("ProfileFragment", "Document snapshot does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d("ProfileFragment", "Error loading profile: ${e.message}", e)
                }
        } else {
            Toast.makeText(context, "User not signed in.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun setupProfilePictureUpload() {
        binding.profileImageView.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(pickImageIntent)
        }
    }



    private fun setupEditIcons() {
        binding.editDobIcon.setOnClickListener {
            showDobEditDialog()
        }
        binding.editGenderIcon.setOnClickListener {
            showGenderEditDialog()
        }
        binding.editWeightIcon.setOnClickListener {
            showWeightEditDialog()
        }
        binding.editHeightIcon.setOnClickListener {
            showHeightEditDialog()
        }
        binding.editActivityLevelIcon.setOnClickListener {
            showActivityLevelEditDialog()
        }
    }

    private fun showDobEditDialog() {
        val currentDob = originalUserProfile?.dateOfBirth
        val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
        val calendar = Calendar.getInstance()
        currentDob?.let {
            try {
                val dob = LocalDate.parse(it, formatter)
                calendar.set(dob.year, dob.monthValue - 1, dob.dayOfMonth)
            } catch (e: Exception) {
                Toast.makeText(context, "Error parsing date: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }
        }

        DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->
            val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
            val formattedDate = selectedDate.format(formatter)
            binding.dobTextView.text = formattedDate

            // Update the originalUserProfile to reflect the new date of birth locally
            originalUserProfile?.dateOfBirth = formattedDate
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }





    private fun showGenderEditDialog() {
        val genderOptions = arrayOf("Male", "Female", "Other")
        val currentGenderIndex = genderOptions.indexOf(originalUserProfile?.gender ?: "Male")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Gender")
            .setSingleChoiceItems(genderOptions, currentGenderIndex) { dialog, which ->
                // Update the genderTextView with the selected gender
                val selectedGender = genderOptions[which]
                binding.genderTextView.text = selectedGender

                // Update the originalUserProfile to reflect the new gender locally
                originalUserProfile?.gender = selectedGender

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }



    private fun showWeightEditDialog() {
        // Create an EditText for the dialog to input the new weight
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(originalUserProfile?.weight.toString())
            selectAll()
        }

        // Create and show the dialog
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Edit Weight")
            setView(input)
            setPositiveButton("Save") { dialog, _ ->
                val newWeight = input.text.toString()
                if (newWeight.isNotEmpty()) {
                    val weightDouble = newWeight.toDoubleOrNull() ?: originalUserProfile?.weight ?: 0.0
                    binding.weightTextView.text = "$newWeight kg" // Update UI

                    // Update the originalUserProfile to reflect the new weight locally
                    originalUserProfile?.weight = weightDouble
                }
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        }.create().show()
    }



    private fun showHeightEditDialog() {
        // Inflate the custom layout for the dialog
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.dialog_edit_height, null)
        val editTextHeight = view.findViewById<EditText>(R.id.editTextHeight)

        // Pre-populate the current height if available
        editTextHeight.setText(originalUserProfile?.height.toString())

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Edit Height")
            setView(view)
            setPositiveButton("Save") { dialog, _ ->
                val newHeight = editTextHeight.text.toString().toDoubleOrNull()
                if (newHeight != null) {
                    // Update UI
                    binding.heightTextView.text = "$newHeight cm"

                    // Update the originalUserProfile to reflect the new height locally
                    originalUserProfile?.height = newHeight
                } else {
                    Toast.makeText(context, "Please enter a valid height.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        }.create().show()
    }




    private fun showActivityLevelEditDialog() {
        val activityLevels = resources.getStringArray(R.array.activity_level_options)
        val currentActivityLevelIndex = activityLevels.indexOf(originalUserProfile?.activityLevel ?: "")

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Select Activity Level")
            // Display the dialog with single-choice items radio buttons
            setSingleChoiceItems(activityLevels, currentActivityLevelIndex) { dialog, which ->
                val selectedActivityLevel = activityLevels[which]

                // Update the activityLevelTextView with the selected activity level
                binding.activityLevelTextView.text = selectedActivityLevel

                // Update the originalUserProfile model to reflect the new activity level locally
                originalUserProfile?.activityLevel = selectedActivityLevel

                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }



    private fun updateProfileData(field: String, value: Any) {
        val userId = auth.currentUser?.uid ?: return

        // Update Firestore
        firestore.collection("users").document(userId)
            .update(mapOf(field to value))
            .addOnSuccessListener {
                Toast.makeText(context, "$field updated successfully.", Toast.LENGTH_SHORT).show()

                // Update local model to reflect change
                when (field) {
                    "dateOfBirth" -> originalUserProfile?.dateOfBirth = value as String
                    "gender" -> originalUserProfile?.gender = value as String
                    "weight" -> originalUserProfile?.weight = (value as Number).toDouble()
                    "height" -> originalUserProfile?.height = (value as Number).toDouble()
                    "activityLevel" -> originalUserProfile?.activityLevel = value as String
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Failed to update $field: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
