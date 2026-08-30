package com.example.reviveapp

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.reviveapp.databinding.FragmentPersonalInfoBinding
import com.example.reviveapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.Double
import kotlin.concurrent.thread

class PersonalInfoFragment : Fragment() {

    private var _binding: FragmentPersonalInfoBinding? = null
    private val binding get() = _binding!! // safe access to binding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    private var name = ""
    private var age = 0
    private var height = 0
    private var weight = 0
    private var sex = ""
    private var alevels = ""

    private var calories = 0.0
    private var proteinpercent = 0.0
    private var fatspercent = 0.0
    private var carbspercent = 0.0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPersonalInfoBinding.inflate(inflater, container, false)
        val userid = FirebaseAuth.getInstance().currentUser?.uid

        if (userid != null) {
            databaseReference =
                FirebaseDatabase.getInstance()
                    .getReference("Information").child(userid)
            databaseReference.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    name = snapshot.child("personal information").child("name").getValue(String::class.java) ?: ""
                    age = snapshot.child("personal information").child("age").getValue(Int::class.java) ?:0
                    height = snapshot.child("personal information").child("height").getValue(Int::class.java) ?:0
                    sex = snapshot.child("personal information").child("sex").getValue(String::class.java) ?: ""
                    weight = snapshot.child("personal information").child("weight").getValue(Int::class.java) ?:0
                    alevels = snapshot.child("personal information").child("activityLevel").getValue(String::class.java) ?: ""

                    binding.readName.text = name.toString()
                    binding.readAge.text = age.toString()
                    binding.readHeight.text = height.toString()
                    binding.readSex.text = sex.toString()
                    binding.readWeight.text = weight.toString()
                    binding.readActivityLevels.text = alevels.toString()
                } else {
                    Toast.makeText(requireContext(), "No data found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }

        //calls for buttons pressed so that the same dialog popup can be used with different calls depending on what
        // is to be changed
        binding.btnEditAge.setOnClickListener{
            call_DetailChange("age")
        }

        binding.btnEditHeight.setOnClickListener{
            call_DetailChange("height")
        }
        binding.btnEditWeight.setOnClickListener{
            call_DetailChange("weight")
        }

        binding.btnEditSex.setOnClickListener{
            call_detailChange_dropdown("sex")
        }

        binding.btnEditActivityLevel.setOnClickListener(){
            call_detailChange_dropdown("activityLevel")
        }


        binding.btnRecalculateCalories.setOnClickListener{
            recalculatecalories()
        }
        //end the fragment
        return binding.root
    }

    private fun call_DetailChange(option: String) { // listens to see if forgot password text is clicked
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_changeinfo, null) // calls dialog_changeinfo xml
        val changedetail = view.findViewById<EditText>(R.id.editBoxDetails)
        builder.setView(view)
        val dialog = builder.create()

        fun validateInput(value: Int): Boolean {
            return when (option) {
                "age" -> {
                    if (value < 12) {
                        Toast.makeText(requireContext(), "Age must be 12 or above", Toast.LENGTH_SHORT).show()
                        false
                    } else true
                }
                "height" -> {
                    if (value > 250) {
                        Toast.makeText(requireContext(), "Height must be below 250", Toast.LENGTH_SHORT).show()
                        false
                    } else true
                }
                "weight" -> {
                    if (value <= 0) {
                        Toast.makeText(requireContext(), "Weight must be greater than 0", Toast.LENGTH_SHORT).show()
                        false
                    } else true
                }
                else -> true
            }
        }

        view.findViewById<Button>(R.id.btnChangeDetail).setOnClickListener {
            val userid = FirebaseAuth.getInstance().currentUser?.uid

            if (userid != null){
                var detailText = changedetail.text.toString()
                if (detailText.isNotEmpty()) {
                    try {
                        // Convert to integer
                        val detailValue = detailText.toInt()

                        // Update the database with the integer value
                        if (validateInput(detailValue)) {
                            // Update the database with the validated integer value
                            databaseReference.child("personal information").child(option)
                                .setValue(detailValue)
                                .addOnSuccessListener {
                                    updateUI(option, detailText)
                                    updatevariableints(option, detailValue)
                                    dialog.dismiss()
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to save",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } catch (e: NumberFormatException) {
                        // Handle invalid number input
                        Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Field cannot be empty", Toast.LENGTH_SHORT).show()
                }
            } else{
                Toast.makeText(requireContext(),"Failed",Toast.LENGTH_SHORT).show()
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

    private fun call_detailChange_dropdown(option: String){
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_changeinfo_dropdown, null) // calls dialog_forgot xml
        val changedetail = view.findViewById<AutoCompleteTextView>(R.id.Detail_input_dropdown)

        if (option == "sex"){
            val sexes = resources.getStringArray(R.array.Sexes)  // sets up drop down for sexes option
            val sexadapter = ArrayAdapter(requireContext(),R.layout.dropdown_item,sexes)
            changedetail.setAdapter(sexadapter)
        }else{
            val activitylevels = resources.getStringArray(R.array.Activity_Levels)  // sets up drop down for activity levels option
            val activityleveladapter = ArrayAdapter(requireContext(),R.layout.dropdown_item,activitylevels)
            changedetail.setAdapter(activityleveladapter)
        }

        builder.setView(view)


        val dialog = builder.create()
        view.findViewById<Button>(R.id.btnChangeDetailDropdown).setOnClickListener {
            val userid = FirebaseAuth.getInstance().currentUser?.uid

            if (userid != null){

                var detailText = changedetail.text.toString()
                if (detailText.isNotEmpty()) {
                    try {

                        // Update the database with the integer value
                        databaseReference.child("personal information").child(option).setValue(detailText)
                            .addOnSuccessListener {
                                updateUI(option,detailText)
                                updatevariablestrings(option, detailText)

                            }.addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: NumberFormatException) {
                        // Handle invalid number input
                        Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Field cannot be empty", Toast.LENGTH_SHORT).show()
                }

            }else {
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnCancelDetailDropdown).setOnClickListener {
            dialog.dismiss() // cancels operation
        }
        if (dialog.window != null){
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.show()
    }

    private fun recalculatecalories() {

        val userid = FirebaseAuth.getInstance().currentUser?.uid

        if (userid != null) {
            databaseReference =
                FirebaseDatabase.getInstance()
                    .getReference("Information").child(userid)
            databaseReference.get().addOnSuccessListener { snapshot ->
                proteinpercent = snapshot.child("personal information").child("proteinpercent")
                    .getValue(Double::class.java) ?: 0.0
                fatspercent = snapshot.child("personal information").child("fatspercent")
                    .getValue(Double::class.java) ?: 0.0
                carbspercent = snapshot.child("personal information").child("carbspercent")
                    .getValue(Double::class.java) ?: 0.0
                calories = snapshot.child("personal information").child("calories").getValue(Double::class.java) ?: 0.0
                val personalInfo = PersonalInfo(
                    age = age,
                    height = height,
                    weight = weight,
                    sex = sex,
                    activityLevel = alevels,
                    proteinpercent = proteinpercent, // this ensures that if the user has changed their %, then it wont be reset here
                    fatspercent = fatspercent,
                    carbspercent = carbspercent
                )

                personalInfo.calculatecalories()

                if (calories == personalInfo.calories){
                    Toast.makeText(requireContext(),"No need to recalculate", Toast.LENGTH_SHORT).show()
                } else{
                    personalInfo.calculatemacros()

                    val updates = mapOf(
                        "personal information/calories" to personalInfo.calories,
                        "personal information/deficitcalories" to personalInfo.deficitcalories,
                        "personal information/protein" to personalInfo.protein,
                        "personal information/fats" to personalInfo.fats,
                        "personal information/carbs" to personalInfo.carbs,
                    )
                    databaseReference.updateChildren(updates).addOnSuccessListener {
                        showConfirmationDialog(personalInfo.calories, personalInfo.deficitcalories)
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to ReCalculate", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load macronutrient percentages",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


    }
    // Moved dialog creation to separate function for clarity
    private fun showConfirmationDialog(calories: Double, deficitcalories: Double) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_confirm, null)
        builder.setView(view)

        val calorietext = view.findViewById<TextView>(R.id.caloriebox)
        val deficittext = view.findViewById<TextView>(R.id.deficitcaloriebox)
        calorietext.setText("Calories: $calories")
        deficittext.setText("Deficit Calories: $deficitcalories")

        val dialog = builder.create()
        view.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun updateUI(option: String, newValue: String) {
        when (option) {
            "name" -> binding.readName.text = newValue
            "age" -> binding.readAge.text = newValue
            "height" -> binding.readHeight.text = newValue
            "sex" -> binding.readSex.text = newValue
            "weight" -> binding.readWeight.text = newValue
            "activityLevel" -> binding.readActivityLevels.text = newValue
        }
    }

    private fun updatevariableints(option: String, newValue: Int) {
        when (option) {
            "age" -> age = newValue
            "height" -> height = newValue
            "weight" -> weight = newValue
        }
    }

    private fun updatevariablestrings(option: String, newValue: String) {
        when (option) {
            "sex" -> sex = newValue
            "activityLevel" -> alevels = newValue
        }
    }
}