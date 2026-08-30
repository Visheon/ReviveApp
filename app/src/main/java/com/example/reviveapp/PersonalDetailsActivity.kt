package com.example.reviveapp


import android.content.Intent
import android.os.Bundle
import android.service.autofill.UserData
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.example.reviveapp.databinding.ActivityPersonalDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class PersonalDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalDetailsBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalDetailsBinding.inflate(layoutInflater) // layout binding to be able to see buttons ,etc
        setContentView(binding.root)


        val sexes = resources.getStringArray(R.array.Sexes)  // sets up drop down for sexes option
        val sexadapter = ArrayAdapter(this,R.layout.dropdown_item,sexes)
        binding.sexInput.setAdapter(sexadapter)

        val activitylevels = resources.getStringArray(R.array.Activity_Levels)  // sets up drop down for activity levels option
        val activitylevelsadapter = ArrayAdapter(this,R.layout.dropdown_item,activitylevels)
        binding.fitnessInput.setAdapter(activitylevelsadapter)


        binding.personalinfoSaveButton.setOnClickListener{  // waits until "SAVE DETAILS" is pressed
            val name = binding.nameInput.text.toString() // assigns values to variables
            val age = binding.ageInput.text.toString().toIntOrNull() ?: 0
            val height = binding.heightInput.text.toString().toIntOrNull() ?: 0
            val weight = binding.weightInput.text.toString().toIntOrNull() ?: 0
            val sex = binding.sexInput.text.toString()
            val activitylevel = binding.fitnessInput.text.toString()

            if (name.isEmpty() || age == 0 || height == 0 || weight == 0 || sex.isEmpty() || activitylevel.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // makes sure fields arent empty
            }
            if(age < 12){
                Toast.makeText(this, "Must be at least 12 years old", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(height >= 250){
                Toast.makeText(this, "Height cannot be above 250cm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //get the current user id
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            // makes sure current user id is not null
            if (userId != null){
                databaseReference = FirebaseDatabase.getInstance().getReference("Information")

                val personalinformation = PersonalInfo(name,age,height,weight,sex,activitylevel,0.0,0.0, 0, 0, 0 )  // calls the personal information data class
                personalinformation.calculatecalories() // calculates the calories
                personalinformation.calculatemacros() // calculates macros
                databaseReference.child(userId).child("personal information").setValue(personalinformation).addOnSuccessListener{  // saves the data to firebase realtime database
                    binding.nameInput.text.clear()
                    binding.ageInput.text.clear() // clears values from variables as they are not needed anymore
                    binding.heightInput.text.clear()
                    binding.weightInput.text.clear()
                    binding.sexInput.text.clear()
                    binding.fitnessInput.text.clear()

                    Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show() // shows a success message
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener{
                    Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show() // in case of failiure shows a message
                }
            }else {
                // Handle the case if user is not authenticated (userId is null)
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }

    }
}