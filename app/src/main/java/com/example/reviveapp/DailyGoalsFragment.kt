package com.example.reviveapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.reviveapp.databinding.FragmentDailygoalsBinding
import com.example.reviveapp.databinding.FragmentPersonalInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DailyGoalsFragment : Fragment() {

    private var _binding: FragmentDailygoalsBinding? = null
    private val binding get() = _binding!! // safe access to binding
    private lateinit var databaseReference: DatabaseReference

    private var calorieG = 0.0
    private var proteinG = 0.0
    private var fatG = 0.0
    private var carbG = 0.0

    private var proteinpercent = 0.0
    private var fatspercent = 0.0
    private var carbpercent = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDailygoalsBinding.inflate(inflater,container,false)

        setupFirebase()
        readData()

        binding.btnEditCalories.setOnClickListener{
            call_DetailChangeCalories()
        }
        binding.btnEditMacros.setOnClickListener{
            showMacrosAdjustmentDialog()
        }

        return binding.root
    }


    private fun setupFirebase() {
        val userid = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("Information").child(userid?: "").child("personal information")
    }

    private fun readData(){
        val userid = FirebaseAuth.getInstance().currentUser?.uid
        if (userid != null){
            databaseReference.get().addOnSuccessListener{ snapshot ->
                calorieG = snapshot.child("deficitcalories").getValue(Double::class.java) ?: 0.0
                proteinG = snapshot.child("protein").getValue(Double::class.java) ?: 0.0
                fatG = snapshot.child("fats").getValue(Double::class.java) ?: 0.0
                carbG = snapshot.child("carbs").getValue(Double::class.java) ?: 0.0

                proteinpercent = snapshot.child("proteinpercent").getValue(Double::class.java) ?: 0.0
                fatspercent = snapshot.child("fatspercent").getValue(Double::class.java) ?: 0.0
                carbpercent = snapshot.child("carbspercent").getValue(Double::class.java) ?: 0.0

                binding.readCalories.text = calorieG.toString()
                binding.readProtein.text = proteinG.toString()
                binding.readFats.text = fatG.toString()
                binding.readCarbs.text = carbG.toString()

                binding.readProteinPercent.text = "${(proteinpercent * 100).toInt()}%"
                binding.readFatsPercent.text = "${(fatspercent * 100).toInt()}%"
                binding.readCarbsPercent.text = "${(carbpercent * 100).toInt()}%"

            }.addOnFailureListener{
                Toast.makeText(requireContext(),"Something went wrong...", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun call_DetailChangeCalories() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_changeinfo, null) // calls dialog_changeinfo xml

        val changedetail = view.findViewById<EditText>(R.id.editBoxDetails)

        changedetail.setText(calorieG.toString())

        builder.setView(view)
        val dialog = builder.create()
        view.findViewById<Button>(R.id.btnChangeDetail).setOnClickListener{
            val userid = FirebaseAuth.getInstance().currentUser?.uid
            if (userid != null){
                val detailText = changedetail.text.toString()
                if (detailText.isNotEmpty()) {
                    val detailValue = detailText.toInt()

                    val newProtein = ((detailValue * proteinpercent) / 4).toInt()
                    val newFats = ((detailValue * fatspercent) / 9).toInt()
                    val newCarbs = ((detailValue * carbpercent) / 4).toInt()

                    val updates = hashMapOf<String, Any>(
                        "calories" to detailValue,
                        "deficitcalories" to detailValue,
                        "protein" to newProtein,
                        "fats" to newFats,
                        "carbs" to newCarbs
                    )

                    databaseReference.updateChildren(updates)
                        .addOnSuccessListener {
                            calorieG = detailValue.toDouble()
                            proteinG = newProtein.toDouble()
                            fatG = newFats.toDouble()
                            carbG = newCarbs.toDouble()

                            binding.readCalories.text = detailValue.toString()
                            binding.readProtein.text = newProtein.toString()
                            binding.readFats.text = newFats.toString()
                            binding.readCarbs.text = newCarbs.toString()

                            dialog.dismiss()
                            Toast.makeText(context, "Goals updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        view.findViewById<Button>(R.id.btnCancelDetails).setOnClickListener {
            dialog.dismiss() // cancels operation
        }
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.show()
    }


    private fun showMacrosAdjustmentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_macros_adjustment, null)

        // Initialize views
        val proteinSeekBar = view.findViewById<SeekBar>(R.id.proteinSeekBar)
        val fatsSeekBar = view.findViewById<SeekBar>(R.id.fatsSeekBar)
        val carbsSeekBar = view.findViewById<SeekBar>(R.id.carbsSeekBar)

        val proteinPercentageText = view.findViewById<TextView>(R.id.proteinPercentage)
        val fatsPercentageText = view.findViewById<TextView>(R.id.fatsPercentage)
        val carbsPercentageText = view.findViewById<TextView>(R.id.carbsPercentage)
        val totalPercentageText = view.findViewById<TextView>(R.id.totalPercentage)

        // Set initial values rounded to nearest 5%
        proteinSeekBar.progress = (((proteinpercent * 100) + 2.5) / 5).toInt() * 5
        fatsSeekBar.progress = (((fatspercent * 100) + 2.5) / 5).toInt() * 5
        carbsSeekBar.progress = (((carbpercent * 100) + 2.5) / 5).toInt() * 5

        // Update text views
        fun updatePercentageTexts() {
            proteinPercentageText.text = "${proteinSeekBar.progress}%"
            fatsPercentageText.text = "${fatsSeekBar.progress}%"
            carbsPercentageText.text = "${carbsSeekBar.progress}%"
            val total = proteinSeekBar.progress + fatsSeekBar.progress + carbsSeekBar.progress
            totalPercentageText.text = "Total: $total%"
            totalPercentageText.setTextColor(if (total == 100)
                resources.getColor(R.color.darkGreen)
            else resources.getColor(android.R.color.holo_red_dark))
        }

        // Set listeners for seek bars
        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Round to nearest 5%
                    val roundedProgress = ((progress + 2.5) / 5).toInt() * 5
                    seekBar?.progress = roundedProgress
                }
                updatePercentageTexts()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        proteinSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        fatsSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        carbsSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        updatePercentageTexts()

        builder.setView(view)
        val dialog = builder.create()

        // Handle save button click
        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val total = proteinSeekBar.progress + fatsSeekBar.progress + carbsSeekBar.progress
            if (total != 100) {
                Toast.makeText(context, "Total percentage must equal 100%", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert to decimals for storage
            val newProteinPercent = proteinSeekBar.progress / 100.0
            val newFatsPercent = fatsSeekBar.progress / 100.0
            val newCarbsPercent = carbsSeekBar.progress / 100.0

            // Save to Firebase and update local values
            val updates = hashMapOf<String, Any>(
                "proteinpercent" to newProteinPercent,
                "fatspercent" to newFatsPercent,
                "carbspercent" to newCarbsPercent
            )

            // Calculate new macro values based on current calories
            val newProtein = ((calorieG * newProteinPercent) / 4).toInt()
            val newFats = ((calorieG * newFatsPercent) / 9).toInt()
            val newCarbs = ((calorieG * newCarbsPercent) / 4).toInt()

            updates["protein"] = newProtein
            updates["fats"] = newFats
            updates["carbs"] = newCarbs

            databaseReference.updateChildren(updates)
                .addOnSuccessListener {
                    // Update local variables
                    proteinpercent = newProteinPercent
                    fatspercent = newFatsPercent
                    carbpercent = newCarbsPercent
                    proteinG = newProtein.toDouble()
                    fatG = newFats.toDouble()
                    carbG = newCarbs.toDouble()

                    // Update UI
                    binding.readProtein.text = newProtein.toString()
                    binding.readFats.text = newFats.toString()
                    binding.readCarbs.text = newCarbs.toString()
                    binding.readProteinPercent.text = "${(newProteinPercent * 100).toInt()}%"
                    binding.readFatsPercent.text = "${(newFatsPercent * 100).toInt()}%"
                    binding.readCarbsPercent.text = "${(newCarbsPercent * 100).toInt()}%"

                    Toast.makeText(context, "Macros updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update macros", Toast.LENGTH_SHORT).show()
                }
        }

        view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }


    private fun updateUI(option: String, newValue: String){
        when (option) {
            "calories" -> binding.readCalories.text = newValue
            "protein" -> binding.readProtein.text = newValue
            "fats" -> binding.readFats.text = newValue
            "carbs" -> binding.readCarbs.text = newValue

            "proteinpercent" -> binding.readProteinPercent.text = newValue
            "fatspercent" -> binding.readFatsPercent.text = newValue
            "carbspercent" -> binding.readCarbsPercent.text = newValue
        }
    }
}