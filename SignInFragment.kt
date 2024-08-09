package com.example.fitfeast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fitfeast.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.navigation.fragment.findNavController

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth

        binding.buttonSignIn.setOnClickListener {
            val email = binding.editTextEmailSignIn.text.toString().trim()
            val password = binding.editTextPasswordSignIn.text.toString().trim()
            signInUser(email, password)
        }
    }

    private fun signInUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Navigate to DashboardFragment
                    findNavController().navigate(R.id.action_signInFragment_to_dashboardFragment)
                } else {
                    // Handle errors
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidCredentialsException -> Toast.makeText(context, "Invalid credentials.", Toast.LENGTH_SHORT).show()
                        is FirebaseAuthInvalidUserException -> Toast.makeText(context, "User not found.", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(context, "Authentication failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
