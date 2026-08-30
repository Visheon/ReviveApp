package com.example.reviveapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reviveapp.databinding.FragmentMealBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import java.math.BigDecimal
import java.math.RoundingMode

class MealFragment : Fragment(), MealAdapter.MealClickListener {
    private var _binding: FragmentMealBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mealAdapter: MealAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealBinding.inflate(inflater, container, false)

        // Initialize Firebase first
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("Information")
            .child(userId ?: "")
            .child("Meals")


        setupRecyclerView()
        setupSearch()
        loadMealsFromFirebase()

        binding.createMealButton.setOnClickListener {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, CreateMealFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return binding.root
    }

    override fun onMealClick(meal: Meal) {
        showMealDetailsDialog(meal)
    }

    override fun onMealLongClick(meal: Meal) {
        showEditDeleteDialog(meal)
    }

    private fun showMealDetailsDialog(meal: Meal) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_meal_details, null)

        val nameTextView = dialogView.findViewById<TextView>(R.id.mealNameText)
        val caloriesTextView = dialogView.findViewById<TextView>(R.id.totalCaloriesText)
        val nutrientsTextView = dialogView.findViewById<TextView>(R.id.nutrientsText)
        val foodItemsList = dialogView.findViewById<ListView>(R.id.foodItemsList)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        nameTextView.text = meal.name
        caloriesTextView.text = "Total Calories: ${meal.totalCalories.roundToDecimal(1)}"
        nutrientsTextView.text = "Proteins: ${meal.totalProteins.roundToDecimal(1)}g    " +
                "Fats: ${meal.totalFats.roundToDecimal(1)}g     " +
                "Carbs: ${meal.totalCarbs.roundToDecimal(1)}g     "

        // Create adapter for food items list
        val foodItems = meal.foodItems.values.toList()
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            foodItems.map { "${it.foodItem.name} (${it.grams}g) - ${(it.calories * it.grams).roundToDecimal(1)} kcal" }
        )
        foodItemsList.adapter = adapter

        builder.setView(dialogView)
        val dialog = builder.create()

        closeButton.setOnClickListener { dialog.dismiss() }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun showEditDeleteDialog(meal: Meal) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_meal_options, null)

        val editButton = view.findViewById<Button>(R.id.editButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        builder.setView(view)
        val dialog = builder.create()

        // Set click listeners
        editButton.setOnClickListener {
            dialog.dismiss()
            editMeal(meal)
        }

        deleteButton.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(meal)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Set transparent background
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(meal: Meal) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_meal_options, null)

        // Reuse the same layout but modify it for delete confirmation
        val titleText = view.findViewById<TextView>(R.id.dialogTitle)
        val confirmButton = view.findViewById<Button>(R.id.editButton)    // Reuse edit button for confirm
        val cancelButton = view.findViewById<Button>(R.id.deleteButton)   // Reuse delete button for cancel
        val bottomButton = view.findViewById<Button>(R.id.cancelButton)   // Hide the bottom button

        // Update UI elements
        titleText.text = "Delete Meal"
        confirmButton.text = "Delete"
        cancelButton.text = "Keep"
        confirmButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#400a06"))
        cancelButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#053411"))
        bottomButton.visibility = View.GONE

        builder.setView(view)
        val dialog = builder.create()

        // Set click listeners
        confirmButton.setOnClickListener {
            dialog.dismiss()
            deleteMeal(meal)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Set transparent background
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun editMeal(meal: Meal) {
        val transaction = parentFragmentManager.beginTransaction()
        val createMealFragment = CreateMealFragment().apply {
            arguments = Bundle().apply {
                putString("mealName", meal.name)
                putBoolean("isEditing", true)
            }
        }
        transaction.replace(R.id.fragment_container, createMealFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun confirmDelete(meal: Meal) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Meal")
            .setMessage("Are you sure you want to delete ${meal.name}?")
            .setPositiveButton("Delete") { _, _ -> deleteMeal(meal) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMeal(meal: Meal) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            databaseReference.child(meal.name).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, "Meal deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to delete meal", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun Double.roundToDecimal(decimals: Int): Double {
        return BigDecimal(this)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }


    private fun setupRecyclerView() {
        mealAdapter = MealAdapter()
        mealAdapter.setMealClickListener(this)
        binding.mealsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mealAdapter
        }
    }

    private fun setupSearch() {
        binding.mealSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                mealAdapter.searchMeals(newText ?: "")
                return true
            }
        })
    }

    private fun loadMealsFromFirebase() {
        try {
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val meals = mutableListOf<Meal>()
                    for (mealSnapshot in snapshot.children) {
                        val meal = mealSnapshot.getValue(Meal::class.java)
                        meal?.let { meals.add(it) }
                    }
                    mealAdapter.updateMeals(meals)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load meals", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading meals", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}