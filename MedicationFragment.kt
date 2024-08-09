package com.example.fitfeast

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitfeast.databinding.FragmentMedicationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MedicationFragment : Fragment(), AddMedicationDialogFragment.MedicationUpdateListener {

    private var _binding: FragmentMedicationBinding? = null
    private val binding get() = _binding!!
    private lateinit var medicationAdapter: MedicationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchMedications()

        binding.addMedicationButton.setOnClickListener {
            showAddMedicationDialog()
        }

        // Handle the manual refresh button
        //binding.refreshMedicationButton.setOnClickListener {
           // fetchMedications()
            //Toast.makeText(context, "Refreshed", Toast.LENGTH_SHORT).show()
       // }
    }

    private fun showAddMedicationDialog() {
        val dialog = AddMedicationDialogFragment().apply {
            updateListener = this@MedicationFragment
        }
        dialog.show(parentFragmentManager, "addMedication")
    }

    override fun onMedicationUpdated() {
        fetchMedications()
    }

    private fun setupRecyclerView() {
        medicationAdapter = MedicationAdapter(emptyList(), ::openEditDialog)
        binding.medicationsRecyclerView.adapter = medicationAdapter
    }

    private fun openEditDialog(medication: Medication) {
        val editDialogFragment = AddMedicationDialogFragment.newInstance(medication.id)
        editDialogFragment.show(parentFragmentManager, "editMedication")
    }


    private fun fetchMedications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("medications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("MedicationFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val medications = snapshots?.map { doc ->
                    Medication(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        instructions = doc.getString("instructions") ?: "",
                        pillQuantity = doc.getLong("pillQuantity")?.toInt() ?: 0,
                        repeatDays = doc.get("repeatDays") as? List<String> ?: emptyList(),
                        notes = doc.getString("notes") ?: ""
                    )
                } ?: emptyList()

                medicationAdapter.updateMedications(medications)
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}