package com.example.reviveapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reviveapp.databinding.FragmentItemBinding
import com.example.reviveapp.databinding.FragmentPersonalInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ItemFragment : Fragment() {

    private var _binding: FragmentItemBinding? = null
    private val binding get() = _binding!! // safe access to binding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var searchView: SearchView

    private lateinit var adapter: FoodItemAdapter
    private var foodList = mutableListOf<FoodItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentItemBinding.inflate(inflater, container, false)
        searchView = binding.search


        val recyclerView = binding.recyclerItemView // Assuming you have a RecyclerView in your Fragment layout
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = FoodItemAdapter(requireContext(), emptyList()) // Initialize with an empty list to start now
        recyclerView.adapter = adapter

        databaseReference = FirebaseDatabase.getInstance().getReference("Information")
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let {
            databaseReference.child(it).child("Food items").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    foodList.clear()
                    for (foodSnapshot in snapshot.children) {
                        val foodItem = foodSnapshot.getValue(FoodItem::class.java)
                        if (foodItem != null) {
                            foodItem.convertto100gram() //converts back to 100g for viewing ease
                            foodList.add(foodItem) // adds each item to a list
                        }
                    }
                    // Update adapter with fetched data
                    adapter.updateData(foodList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            })
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchList(newText ?: "")
                return true
            }
        })
        binding.itemUploadButton.setOnClickListener{
            val builder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.dialog_uploaditem, null) // calls dialog_uploaditem xml

            val name = view.findViewById<EditText>(R.id.uploadItemName)
            val calories = view.findViewById<EditText>(R.id.uploadCalories)
            val protein = view.findViewById<EditText>(R.id.uploadProtein)
            val fats = view.findViewById<EditText>(R.id.uploadFats)
            val carbs = view.findViewById<EditText>(R.id.uploadCarbs)

            builder.setView(view)
            val dialog = builder.create()

            view.findViewById<Button>(R.id.itemsaveButton).setOnClickListener {

                val itemname = name.text.toString()
                val intcalories = calories.text.toString().toDoubleOrNull() ?: 0.0
                val intprotein = protein.text.toString().toDoubleOrNull() ?: 0.0
                val intfats = fats.text.toString().toDoubleOrNull() ?: 0.0
                val intcarbs = carbs.text.toString().toDoubleOrNull() ?: 0.0

                if (itemname.isEmpty() || intcalories == 0.0 || intprotein == 0.0 || intfats == 0.0 || intcarbs == 0.0) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                } else {

                    val itemupload = FoodItem(name = itemname, calories = intcalories, proteins = intprotein, fats = intfats, carbs = intcarbs)
                    itemupload.convertto1gram()

                    val userid = FirebaseAuth.getInstance().currentUser?.uid
                    if (userid != null){
                        databaseReference = FirebaseDatabase.getInstance().getReference("Information")
                        databaseReference.child(userid).child("Food items").child(itemname).setValue(itemupload).addOnSuccessListener{

                            name.text.clear()
                            calories.text.clear()
                            protein.text.clear()
                            fats.text.clear()
                            carbs.text.clear()
                            Toast.makeText(requireContext(),"Saved",Toast.LENGTH_SHORT).show() // shows a success message
                        }

                    } else{
                        Toast.makeText(requireContext(),"Failed",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }

                    dialog.dismiss()
                }
            }
            view.findViewById<Button>(R.id.itemCancelButton).setOnClickListener {
                dialog.dismiss() // cancels operation
            }
            if (dialog.window != null){
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()

        }
        return binding.root
    }


    private fun searchList(query: String) {
        if (foodList.isNotEmpty()){
            val filteredList = foodList.filter {
                it.name.toLowerCase().contains(query.toLowerCase()) // Filter food items by name
            }
            adapter.updateData(filteredList) // Update the adapter with filtered data
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding reference
    }
}