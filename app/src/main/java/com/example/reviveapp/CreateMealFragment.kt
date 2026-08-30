package com.example.reviveapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reviveapp.databinding.FragmentCreateMealBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.math.RoundingMode
import java.math.BigDecimal
import androidx.appcompat.widget.SearchView

class CreateMealFragment : Fragment() {
    private var _binding: FragmentCreateMealBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseReference: DatabaseReference
    private lateinit var selectedFoodsAdapter: SelectedFoodsAdapter
    private var currentMeal = Meal()
    private var isEditing = false
    private var editingMealName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments passed from MealFragment
        // Retrieve edit mode parameters if they exist
        arguments?.let {
            isEditing = it.getBoolean("isEditing", false)
            editingMealName = it.getString("mealName", "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Initialize the view and setup necessary components
        _binding = FragmentCreateMealBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupDatabase()


        binding.addFoodButton.setOnClickListener {
            // Show food selection dialog
            showFoodSelectionDialog()
        }
        binding.saveMealButton.setOnClickListener {
            saveMealToFirebase()
        }

        // Load existing meal data if in edit mode
        if (isEditing) {
            loadExistingMeal()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        // Initialize adapter for selected food items
        selectedFoodsAdapter = SelectedFoodsAdapter()
        // Setup click listener for removing items by tapping them
        selectedFoodsAdapter.setOnItemClickListener { foodItem ->
            // Remove the item from currentMeal
            currentMeal.foodItems.remove(foodItem.foodItem.name)
            currentMeal.updateTotals()
            // Update the display
            selectedFoodsAdapter.updateItems(currentMeal.foodItems.values.toList())
            updateMealDisplay()
            // Show confirmation toast
            Toast.makeText(context, "${foodItem.foodItem.name} removed", Toast.LENGTH_SHORT).show()
        }
        binding.selectedFoodsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = selectedFoodsAdapter
        }
    }

    private fun setupDatabase() {
        // Fetch food items from Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("Information")
            .child(userId ?: "")
            .child("Meals")
    }


    private fun showFoodSelectionDialog() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val foodsRef = FirebaseDatabase.getInstance()
            .getReference("Information")
            .child(userId ?: "")
            .child("Food items")

        foodsRef.get().addOnSuccessListener { snapshot ->
            val foods = mutableListOf<FoodItem>()
            snapshot.children.forEach { foodSnapshot ->
                val food = foodSnapshot.getValue(FoodItem::class.java)
                food?.let {
                    it.convertto100gram() // Convert to 100g for display
                    foods.add(it)
                }
            }

            if (foods.isNotEmpty()) {

                // Setup dialog for food selection with search functionality
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_selection, null)
                val builder = AlertDialog.Builder(requireContext())
                builder.setView(dialogView)

                val dialog = builder.create()

                val searchView = dialogView.findViewById<SearchView>(R.id.foodSearchView)
                val foodListView = dialogView.findViewById<ListView>(R.id.foodListView)
                val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

                val filteredFoods = mutableListOf<FoodItem>()
                filteredFoods.addAll(foods)

                // Simpler adapter implementation
                val adapter = object : BaseAdapter() {
                    override fun getCount(): Int = filteredFoods.size
                    override fun getItem(position: Int): FoodItem = filteredFoods[position]
                    override fun getItemId(position: Int): Long = position.toLong()

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
                        val nameTextView = itemView.findViewById<TextView>(R.id.recTitle)
                        val caloriesTextView = itemView.findViewById<TextView>(R.id.recPriority)

                        val foodItem = filteredFoods[position]
                        nameTextView.text = foodItem.name
                        caloriesTextView.text = "${foodItem.calories} kcal"

                        // Add margin between items
                        val params = itemView.layoutParams as? ViewGroup.MarginLayoutParams ?: ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(12, 8, 12, 8)
                        itemView.layoutParams = params

                        return itemView
                    }
                }

                foodListView.adapter = adapter

                // Setup search functionality
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false

                    override fun onQueryTextChange(newText: String?): Boolean {
                        filteredFoods.clear()
                        // Filter foods based on search text
                        if (newText.isNullOrEmpty()) {
                            filteredFoods.addAll(foods)
                        } else {
                            val filtered = foods.filter {
                                it.name.toLowerCase().contains(newText.toLowerCase())
                            }
                            filteredFoods.addAll(filtered)
                        }
                        adapter.notifyDataSetChanged()
                        return true
                    }
                })

                foodListView.setOnItemClickListener { _, _, position, _ ->
                    val selectedFood = filteredFoods[position].copy()
                    selectedFood.convertto1gram() // Convert back to 1g before showing quantity dialog
                    showFoodQuantityDialog(selectedFood)
                    dialog.dismiss()
                }

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                if (dialog.window != null) {
                    dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }

                dialog.show()
            } else {
                Toast.makeText(context, "No food items available. Please add some first.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to load food items", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFoodQuantityDialog(foodItem: FoodItem) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_quantity_input, null)

        val quantityInput = view.findViewById<EditText>(R.id.quantityInput)
        val confirmButton = view.findViewById<Button>(R.id.MealSaveBtn)
        val cancelButton = view.findViewById<Button>(R.id.MealCloseBtn)

        builder.setView(view)
        val dialog = builder.create()

        confirmButton.setOnClickListener {
            val quantity = quantityInput.text.toString().toDoubleOrNull()
            if (quantity != null && quantity > 0) {
                addFoodItemToMeal(foodItem, quantity)
                dialog.dismiss()
                updateMealDisplay()
            } else {
                Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        if (dialog.window != null){
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun addFoodItemToMeal(foodItem: FoodItem, grams: Double) {
        val foodItemWithQuantity = FoodItemWithQuantity(foodItem, grams)
        currentMeal.foodItems[foodItem.name] = foodItemWithQuantity
        currentMeal.updateTotals()
        selectedFoodsAdapter.updateItems(currentMeal.foodItems.values.toList())
    }

    private fun updateMealDisplay() {
        binding.mealTotalCalories.text = "Total Calories: ${currentMeal.totalCalories.roundToDecimal(1)}"
        binding.mealTotalNutrients.text =
            "Proteins: ${currentMeal.totalProteins.roundToDecimal(1)}g " +
                    "Fats: ${currentMeal.totalFats.roundToDecimal(1)}g " +
                    "Carbs: ${currentMeal.totalCarbs.roundToDecimal(1)}g"
    }


    private fun loadExistingMeal() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && editingMealName.isNotEmpty()) {
            // Set the meal name in the input field
            binding.mealNameInput.setText(editingMealName)

            val mealRef = FirebaseDatabase.getInstance()
                .getReference("Information")
                .child(userId)
                .child("Meals")
                .child(editingMealName)

            mealRef.get().addOnSuccessListener { snapshot ->
                snapshot.getValue(Meal::class.java)?.let { meal ->
                    currentMeal = meal
                    selectedFoodsAdapter.updateItems(meal.foodItems.values.toList())
                    updateMealDisplay()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to load meal data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveMealToFirebase() {
        val mealName = binding.mealNameInput.text.toString()
        if (mealName.isBlank()) {
            Toast.makeText(context, "Please enter a meal name", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditing && editingMealName != mealName) {
            databaseReference.child(editingMealName).removeValue()
        }

        if (currentMeal.foodItems.isEmpty()) {
            Toast.makeText(context, "Please add at least one food item", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("Information")
            .child(userId ?: "")
            .child("Meals")

        currentMeal.name = mealName
        currentMeal.updateTotals() // Make sure totals are up to date

        databaseReference.child(mealName).setValue(currentMeal)
            .addOnSuccessListener {
                Toast.makeText(context, "Meal saved successfully", Toast.LENGTH_SHORT).show()
                // Navigate back to MealFragment
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save meal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun Double.roundToDecimal(decimals: Int): Double {
        return BigDecimal(this)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}