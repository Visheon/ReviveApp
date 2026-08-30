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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.reviveapp.databinding.ActivityMainBinding
import com.example.reviveapp.databinding.FragmentProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!! // safe access to binding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        readData()


        binding.logoutButton.setOnClickListener{
            Firebase.auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)

            Toast.makeText(requireContext(),"Logout Successful", Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    private fun readData(){
        val user = FirebaseAuth.getInstance().currentUser
        val userid = FirebaseAuth.getInstance().currentUser?.uid
        if (userid != null){
            databaseReference = FirebaseDatabase.getInstance().getReference("Information").child(userid)
            databaseReference.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("personal information").child("name").value
                    val email = user?.email
                    binding.readName.text = name.toString()
                    binding.readEmail.text = email.toString()

                } else {
                    Toast.makeText(requireContext(),"No data found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener{
                Toast.makeText(requireContext(),"Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by clearing binding
        _binding = null
    }
}