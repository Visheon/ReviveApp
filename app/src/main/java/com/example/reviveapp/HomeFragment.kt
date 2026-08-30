package com.example.reviveapp

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reviveapp.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.math.BigDecimal
import java.math.RoundingMode

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Adapters for each meal section
    private lateinit var breakfastAdapter: HomeMealAdapter
    private lateinit var snacksAdapter: HomeMealAdapter
    private lateinit var dinnerAdapter: HomeMealAdapter

    // Current daily meals data
    private var currentDailyMeals = DailyMeals()

    // Initial Nutrient Goals
    private var calorieGoal = 2000.0
    private var proteinGoal = 150.0
    private var fatGoal = 65.0
    private var carbGoal = 275.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserGoals()
        setupFirebase()
        setupRecyclerViews()
        setupClickListeners()
    }

    private fun loadUserGoals() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance()
                .getReference("Information")
                .child(userId)
                .child("personal information")
                .get()
                .addOnSuccessListener { snapshot ->
                    calorieGoal = snapshot.child("deficitcalories").getValue(Double::class.java) ?: 0.0
                    proteinGoal = snapshot.child("protein").getValue(Double::class.java) ?: 0.0
                    fatGoal = snapshot.child("fats").getValue(Double::class.java) ?: 0.0
                    carbGoal = snapshot.child("carbs").getValue(Double::class.java) ?: 0.0
                    updateUI()
                    // Once we have the goals, load the meals
                    loadTodaysMeals()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load calorie goals", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun setupFirebase() {
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("Information")
            .child(userId ?: "")
            .child("DailyMeals")
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun setupRecyclerViews() {
        // Initialize adapters with click handlers
        breakfastAdapter = HomeMealAdapter { foodItem ->
            showFoodItemDialog(foodItem, "Breakfast")
        }
        snacksAdapter = HomeMealAdapter { foodItem ->
            showFoodItemDialog(foodItem, "Snacks")
        }
        dinnerAdapter = HomeMealAdapter { foodItem ->
            showFoodItemDialog(foodItem, "Dinner")
        }

        // Setup RecyclerViews with adapters and layouts
        binding.breakfastRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = breakfastAdapter
        }

        binding.snacksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = snacksAdapter
        }

        binding.dinnerRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dinnerAdapter
        }
    }

    private fun setupClickListeners() {
        // Add food buttons click listeners
        binding.addBreakfastButton.setOnClickListener {
            showFoodSelectionDialog("Breakfast")
        }

        binding.addSnacksButton.setOnClickListener {
            showFoodSelectionDialog("Snacks")
        }

        binding.addDinnerButton.setOnClickListener {
            showFoodSelectionDialog("Dinner")
        }
    }

    private fun loadTodaysMeals() {
        val currentDate = getCurrentDate()

        databaseReference.child(currentDate)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Check if this is a new day
                    val savedDate = snapshot.child("date").getValue(String::class.java)
                    if (savedDate != currentDate) {
                        // It's a new day, reset the meals
                        currentDailyMeals = DailyMeals(date = currentDate)
                        // Save the empty day to Firebase
                        databaseReference.child(currentDate).setValue(currentDailyMeals)
                    } else {
                        // Load existing meals for today
                        currentDailyMeals = DailyMeals()
                        currentDailyMeals.date = currentDate

                        // Load meals from snapshot
                        listOf("breakfast", "snacks", "dinner").forEach { mealType ->
                            snapshot.child(mealType).children.forEach { foodSnapshot ->
                                val food = foodSnapshot.getValue(FoodItemWithQuantity::class.java)
                                food?.let {
                                    when (mealType) {
                                        "breakfast" -> currentDailyMeals.breakfast[it.foodItem.name] = it
                                        "snacks" -> currentDailyMeals.snacks[it.foodItem.name] = it
                                        "dinner" -> currentDailyMeals.dinner[it.foodItem.name] = it
                                    }
                                }
                            }
                        }

                        currentDailyMeals.updateTotals()
                        updateUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load meals", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI() {

        // Update overall calorie circle and text
        binding.calorieCount.text = currentDailyMeals.totalCalories.roundToDecimal(1).toString()
        binding.calorieProgress.progress = ((currentDailyMeals.totalCalories / calorieGoal) * 100).toInt()

        // Update the "of X kcal" text to show actual goal
        binding.calorieGoalText.text = "of ${calorieGoal} kcal"


        // Update macro progress bars and texts
        binding.proteinProgress.progress =
            ((currentDailyMeals.totalProteins / proteinGoal) * 100).toInt()
        binding.proteinText.text =
            "${currentDailyMeals.totalProteins.roundToDecimal(1)} of $proteinGoal grams"

        binding.fatsProgress.progress =
            ((currentDailyMeals.totalFats / fatGoal) * 100).toInt()
        binding.fatsText.text =
            "${currentDailyMeals.totalFats.roundToDecimal(1)} of $fatGoal grams"

        binding.carbsProgress.progress =
            ((currentDailyMeals.totalCarbs / carbGoal) * 100).toInt()
        binding.carbsText.text =
            "${currentDailyMeals.totalCarbs.roundToDecimal(1)} of $carbGoal grams"

        // Update meal calorie displays
        binding.breakfastCalories.text =
            "${currentDailyMeals.getMealCalories(currentDailyMeals.breakfast).roundToDecimal(1)} kcal"
        binding.snacksCalories.text =
            "${currentDailyMeals.getMealCalories(currentDailyMeals.snacks).roundToDecimal(1)} kcal"
        binding.dinnerCalories.text =
            "${currentDailyMeals.getMealCalories(currentDailyMeals.dinner).roundToDecimal(1)} kcal"

        // Update RecyclerViews
        breakfastAdapter.updateFoods(currentDailyMeals.breakfast.values.toList())
        snacksAdapter.updateFoods(currentDailyMeals.snacks.values.toList())
        dinnerAdapter.updateFoods(currentDailyMeals.dinner.values.toList())
    }

    private fun showFoodSelectionDialog(mealType: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val foodsRef = FirebaseDatabase.getInstance()
                .getReference("Information")
                .child(userId ?: "")

            // Create dialog
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_food_selection, null)
            val builder = AlertDialog.Builder(requireContext())
            builder.setView(dialogView)
            val dialog = builder.create()

            // Setup dialog views
            val searchView = dialogView.findViewById<SearchView>(R.id.foodSearchView)
            val foodListView = dialogView.findViewById<ListView>(R.id.foodListView)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
            val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)

            titleText.text = "Select Food or Meal"

            // Lists to hold our items
            val allItems = mutableListOf<SelectionItem>()
            val filteredItems = mutableListOf<SelectionItem>()

            // Custom adapter for our combined list
            val adapter = object : BaseAdapter() {
                override fun getCount(): Int = filteredItems.size
                override fun getItem(position: Int): SelectionItem = filteredItems[position]
                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: LayoutInflater.from(context)
                        .inflate(R.layout.recycler_item, parent, false)

                    val nameTextView = itemView.findViewById<TextView>(R.id.recTitle)
                    val detailsTextView = itemView.findViewById<TextView>(R.id.recPriority)

                    val item = filteredItems[position]
                    nameTextView.text = item.name
                    when (item) {
                        is SelectionItem.FoodItem -> {
                            detailsTextView.text = "${item.food.calories} kcal/100g"
                        }
                        is SelectionItem.MealItem -> {
                            detailsTextView.text = "${item.meal.totalCalories.roundToDecimal(1)} kcal"
                        }
                    }


                    return itemView
                }
            }

            // Load foods first
            foodsRef.child("Food items").get().addOnSuccessListener { foodSnapshot ->
                Log.d("HomeFragment", "Food items snapshot: ${foodSnapshot.childrenCount}")
                foodSnapshot.children.forEach { foodSnap ->
                    val food = foodSnap.getValue(FoodItem::class.java)
                    Log.d("HomeFragment", "Found food: ${food?.name}")
                    food?.let {
                        it.convertto100gram() // Convert to 100g for display
                        allItems.add(SelectionItem.FoodItem(it))
                    }
                }

                // Then load meals
                foodsRef.child("Meals").get().addOnSuccessListener { mealSnapshot ->
                    mealSnapshot.children.forEach { mealSnap ->
                        val meal = mealSnap.getValue(Meal::class.java)
                        meal?.let {
                            allItems.add(SelectionItem.MealItem(it))
                        }
                    }

                    if (allItems.isEmpty()) {
                        Toast.makeText(context, "Please add food items first", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        // Initial population of filtered list
                        filteredItems.addAll(allItems)
                        adapter.notifyDataSetChanged()
                    }
                }.addOnFailureListener { error ->
                    Toast.makeText(context, "Failed to load meals: ${error.message}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(context, "Failed to load foods: ${error.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            foodListView.adapter = adapter

            // Setup search functionality
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    filteredItems.clear()
                    if (newText.isNullOrEmpty()) {
                        filteredItems.addAll(allItems)
                    } else {
                        val searchText = newText.toLowerCase()
                        filteredItems.addAll(allItems.filter { item ->
                            when (item) {
                                is SelectionItem.FoodItem ->
                                    item.food.name.toLowerCase().contains(searchText)
                                is SelectionItem.MealItem ->
                                    item.meal.name.toLowerCase().contains(searchText)
                            }
                        })
                    }
                    adapter.notifyDataSetChanged()
                    return true
                }
            })

            // Handle item selection
            foodListView.setOnItemClickListener { _, _, position, _ ->
                val selectedItem = filteredItems[position]
                when (selectedItem) {
                    is SelectionItem.FoodItem -> {
                        val selectedFood = selectedItem.food.copy()
                        selectedFood.convertto1gram() // Convert back to 1g before quantity input
                        showFoodQuantityDialog(selectedFood, mealType)
                    }
                    is SelectionItem.MealItem -> {
                        addMealToMeal(selectedItem.meal, mealType)
                    }
                }
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
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    sealed class SelectionItem {
        abstract val name: String

        data class FoodItem(val food: com.example.reviveapp.FoodItem) : SelectionItem() {
            override val name: String get() = food.name
        }

        data class MealItem(val meal: Meal) : SelectionItem() {
            override val name: String get() = meal.name
        }
    }

    // Function to add a meal's items to current meal
    private fun addMealToMeal(mealToAdd: Meal, mealType: String) {
        val currentDate = getCurrentDate()

        // Add all food items from the selected meal
        mealToAdd.foodItems.values.forEach { foodItem ->
            when (mealType) {
                "Breakfast" -> currentDailyMeals.breakfast[foodItem.foodItem.name] = foodItem
                "Snacks" -> currentDailyMeals.snacks[foodItem.foodItem.name] = foodItem
                "Dinner" -> currentDailyMeals.dinner[foodItem.foodItem.name] = foodItem
            }
        }

        // Update Firebase
        databaseReference.child(currentDate)
            .setValue(currentDailyMeals)
            .addOnSuccessListener {
                currentDailyMeals.updateTotals()
                updateUI()
                Toast.makeText(context, "Meal added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add meal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFoodQuantityDialog(foodItem: FoodItem, mealType: String) {
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
                addFoodToMeal(foodItem, quantity, mealType)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun addFoodToMeal(foodItem: FoodItem, grams: Double, mealType: String) {
        val foodItemWithQuantity = FoodItemWithQuantity(foodItem, grams)
        val currentDate = getCurrentDate()

        // Add to local data structure
        when (mealType) {
            "Breakfast" -> currentDailyMeals.breakfast[foodItem.name] = foodItemWithQuantity
            "Snacks" -> currentDailyMeals.snacks[foodItem.name] = foodItemWithQuantity
            "Dinner" -> currentDailyMeals.dinner[foodItem.name] = foodItemWithQuantity
        }
        currentDailyMeals.updateTotals()

        // Update Firebase
        databaseReference.child(currentDate)
            .setValue(currentDailyMeals)
            .addOnSuccessListener {
                currentDailyMeals.updateTotals()
                updateUI()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save food item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFoodItemDialog(foodItem: FoodItemWithQuantity, mealType: String) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_meal_options, null)

        // Set up dialog views
        val titleText = view.findViewById<TextView>(R.id.dialogTitle)
        val editButton = view.findViewById<Button>(R.id.editButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        // Customize dialog for meal items
        titleText.text = "${foodItem.foodItem.name} Details"
        editButton.text = "Edit Amount"

        builder.setView(view)
        val dialog = builder.create()

        // Handle button clicks
        editButton.setOnClickListener {
            dialog.dismiss()
            showQuantityEditDialog(foodItem, mealType)
        }

        deleteButton.setOnClickListener {
            dialog.dismiss()
            removeFoodFromMeal(foodItem, mealType)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun showQuantityEditDialog(foodItem: FoodItemWithQuantity, mealType: String) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_quantity_input, null)

        val quantityInput = view.findViewById<EditText>(R.id.quantityInput)
        val confirmButton = view.findViewById<Button>(R.id.MealSaveBtn)
        val cancelButton = view.findViewById<Button>(R.id.MealCloseBtn)

        // Show current quantity
        quantityInput.setText(foodItem.grams.toString())

        builder.setView(view)
        val dialog = builder.create()

        confirmButton.setOnClickListener {
            val newQuantity = quantityInput.text.toString().toDoubleOrNull()
            if (newQuantity != null && newQuantity > 0) {
                updateFoodQuantity(foodItem, newQuantity, mealType)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun updateFoodQuantity(foodItem: FoodItemWithQuantity, newQuantity: Double, mealType: String) {
        val currentDate = getCurrentDate()
        val updatedFoodItem = foodItem.copy(grams = newQuantity)

        // Update local data
        when (mealType) {
            "Breakfast" -> currentDailyMeals.breakfast[foodItem.foodItem.name] = updatedFoodItem
            "Snacks" -> currentDailyMeals.snacks[foodItem.foodItem.name] = updatedFoodItem
            "Dinner" -> currentDailyMeals.dinner[foodItem.foodItem.name] = updatedFoodItem
        }

        // Update Firebase
        databaseReference.child(currentDate)
            .setValue(currentDailyMeals)
            .addOnSuccessListener {
                currentDailyMeals.updateTotals()
                updateUI()
                Toast.makeText(context, "Quantity updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFoodFromMeal(foodItem: FoodItemWithQuantity, mealType: String) {
        val currentDate = getCurrentDate()

        // Remove from local data
        when (mealType) {
            "Breakfast" -> currentDailyMeals.breakfast.remove(foodItem.foodItem.name)
            "Snacks" -> currentDailyMeals.snacks.remove(foodItem.foodItem.name)
            "Dinner" -> currentDailyMeals.dinner.remove(foodItem.foodItem.name)
        }

        // Update Firebase
        databaseReference.child(currentDate)
            .setValue(currentDailyMeals)
            .addOnSuccessListener {
                currentDailyMeals.updateTotals()
                updateUI()
                Toast.makeText(context, "Food item removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to remove food item", Toast.LENGTH_SHORT).show()
            }
    }


    private fun Double.roundToDecimal(decimals: Int): Double {
        return BigDecimal(this)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}