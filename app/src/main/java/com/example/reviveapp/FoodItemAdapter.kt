package com.example.reviveapp
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FoodItemAdapter(private val context: Context, private var foodList: List<FoodItem>)
    : RecyclerView.Adapter<FoodItemAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.recTitle)
        val priorityTextView: TextView = itemView.findViewById(R.id.recPriority)

        init {
            itemView.setOnClickListener {
                val foodItem = foodList[adapterPosition]
                showFoodItemDialog(foodItem)
            }

            itemView.setOnLongClickListener {
                val foodItem = foodList[adapterPosition]
                showDeleteOptionsDialog(foodItem)
                true
            }
        }

        private fun showDeleteOptionsDialog(foodItem: FoodItem) {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_meal_options, null)

            val editButton = dialogView.findViewById<Button>(R.id.editButton)
            val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

            builder.setView(dialogView)
            val dialog = builder.create()
            editButton.visibility = View.GONE

            // Set click listeners

            deleteButton.setOnClickListener {
                dialog.dismiss()
                showDeleteConfirmationDialog(foodItem)
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

        private fun showEditFieldsDialog(foodItem: FoodItem) {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_options, null)

            // Find all buttons
            val editNameBtn = dialogView.findViewById<Button>(R.id.editNameButton)
            val editCaloriesBtn = dialogView.findViewById<Button>(R.id.editCaloriesButton)
            val editProteinsBtn = dialogView.findViewById<Button>(R.id.editProteinsButton)
            val editFatsBtn = dialogView.findViewById<Button>(R.id.editFatsButton)
            val editCarbsBtn = dialogView.findViewById<Button>(R.id.editCarbsButton)
            val cancelBtn = dialogView.findViewById<Button>(R.id.cancelButton)

            builder.setView(dialogView)
            val dialog = builder.create()

            editNameBtn.setOnClickListener {
                dialog.dismiss()
                showPropertyEditDialog(foodItem, "name", "Name", foodItem.name, false)
            }

            editCaloriesBtn.setOnClickListener {
                dialog.dismiss()
                showPropertyEditDialog(foodItem, "calories", "Calories", foodItem.calories.toString(), true)
            }

            editProteinsBtn.setOnClickListener {
                dialog.dismiss()
                showPropertyEditDialog(foodItem, "proteins", "Proteins", foodItem.proteins.toString(), true)
            }

            editFatsBtn.setOnClickListener {
                dialog.dismiss()
                showPropertyEditDialog(foodItem, "fats", "Fats", foodItem.fats.toString(), true)
            }

            editCarbsBtn.setOnClickListener {
                dialog.dismiss()
                showPropertyEditDialog(foodItem, "carbs", "Carbs", foodItem.carbs.toString(), true)
            }

            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }

            if (dialog.window != null) {
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()
        }

        private fun showDeleteConfirmationDialog(foodItem: FoodItem) {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_meal_options, null)

            // Get UI elements
            val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
            val confirmButton = dialogView.findViewById<Button>(R.id.editButton)    // Reuse edit button for confirm
            val cancelButton = dialogView.findViewById<Button>(R.id.deleteButton)   // Reuse delete button for cancel
            val bottomButton = dialogView.findViewById<Button>(R.id.cancelButton)   // Hide the bottom button

            // Update UI elements
            titleText.text = "Delete Food Item"
            confirmButton.text = "Delete"
            cancelButton.text = "Keep"
            confirmButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#400a06"))
            cancelButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#053411"))
            bottomButton.visibility = View.GONE

            builder.setView(dialogView)
            val dialog = builder.create()

            // Set click listeners
            confirmButton.setOnClickListener {
                dialog.dismiss()
                deleteFoodItem(foodItem)
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

        private fun showPropertyEditDialog(
            foodItem: FoodItem,
            property: String,
            displayName: String,
            currentValue: String,
            isNumber: Boolean
        ) {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_changeinfo, null)

            val titleTextView = dialogView.findViewById<TextView>(R.id.ChangeTitle)
            val descTextView = dialogView.findViewById<TextView>(R.id.desc)
            val editText = dialogView.findViewById<EditText>(R.id.editBoxDetails)
            val cancelButton = dialogView.findViewById<Button>(R.id.btnCancelDetails)
            val saveButton = dialogView.findViewById<Button>(R.id.btnChangeDetail)

            titleTextView.text = "Edit $displayName"
            descTextView.text = "Enter new $displayName"
            editText.setText(currentValue)

            // Set input type based on field
            if (!isNumber) {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }

            val dialog = builder.setView(dialogView).create()

            cancelButton.setOnClickListener { dialog.dismiss() }

            saveButton.setOnClickListener {
                val newValue = editText.text.toString()
                if (newValue.isNotEmpty()) {
                    when (property) {
                        "name" -> foodItem.name = newValue
                        "calories" -> foodItem.calories = newValue.toDoubleOrNull() ?: foodItem.calories
                        "proteins" -> foodItem.proteins = newValue.toDoubleOrNull() ?: foodItem.proteins
                        "fats" -> foodItem.fats = newValue.toDoubleOrNull() ?: foodItem.fats
                        "carbs" -> foodItem.carbs = newValue.toDoubleOrNull() ?: foodItem.carbs
                    }
                    updateFoodItem(foodItem)
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Please enter a valid value", Toast.LENGTH_SHORT).show()
                }
            }

            if (dialog.window != null) {
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()
        }

        private fun confirmDelete(foodItem: FoodItem) {
            val options = arrayOf("Delete")
            AlertDialog.Builder(context)
                .setTitle("Delete ${foodItem.name}?")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> {
                            // Show final confirmation dialog
                            AlertDialog.Builder(context)
                                .setTitle("Delete Food Item")
                                .setMessage("Are you sure you want to delete ${foodItem.name}?")
                                .setPositiveButton("Delete") { _, _ -> deleteFoodItem(foodItem) }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }

        private fun updateFoodItem(foodItem: FoodItem) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val databaseRef = FirebaseDatabase.getInstance()
                    .getReference("Information")
                    .child(userId)
                    .child("Food items")
                    .child(foodItem.name)

                foodItem.convertto1gram() // Convert to 1g before saving
                databaseRef.setValue(foodItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Food item updated", Toast.LENGTH_SHORT).show()
                        notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update food item", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun deleteFoodItem(foodItem: FoodItem) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val databaseRef = FirebaseDatabase.getInstance()
                    .getReference("Information")
                    .child(userId)
                    .child("Food items")
                    .child(foodItem.name)

                databaseRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Food item deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete food item", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun showFoodItemDialog(foodItem: FoodItem) {
            try {
                val builder = AlertDialog.Builder(context)
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_infoitem, null)
                val editNameBtn = dialogView.findViewById<Button>(R.id.editNameButton)
                val editCaloriesBtn = dialogView.findViewById<Button>(R.id.editCaloriesButton)
                val editProteinsBtn = dialogView.findViewById<Button>(R.id.editProteinsButton)
                val editFatsBtn = dialogView.findViewById<Button>(R.id.editFatsButton)
                val editCarbsBtn = dialogView.findViewById<Button>(R.id.editCarbsButton)

                dialogView.findViewById<TextView>(R.id.fooditemname)?.text = foodItem.name
                dialogView.findViewById<TextView>(R.id.readitemCalories)?.text = "${foodItem.calories} kcal"
                dialogView.findViewById<TextView>(R.id.readitemProteins)?.text = "${foodItem.proteins} g"
                dialogView.findViewById<TextView>(R.id.readitemFats)?.text = "${foodItem.fats} g"
                dialogView.findViewById<TextView>(R.id.readitemCarbs)?.text = "${foodItem.carbs} g"

                builder.setView(dialogView)
                val dialog = builder.create()

                dialogView.findViewById<Button>(R.id.itemClose)?.setOnClickListener {
                    dialog.dismiss()
                }

                editCaloriesBtn.setOnClickListener {
                    dialog.dismiss()
                    showPropertyEditDialog(foodItem, "calories", "Calories", foodItem.calories.toString(), true)
                }

                editProteinsBtn.setOnClickListener {
                    dialog.dismiss()
                    showPropertyEditDialog(foodItem, "proteins", "Proteins", foodItem.proteins.toString(), true)
                }

                editFatsBtn.setOnClickListener {
                    dialog.dismiss()
                    showPropertyEditDialog(foodItem, "fats", "Fats", foodItem.fats.toString(), true)
                }

                editCarbsBtn.setOnClickListener {
                    dialog.dismiss()
                    showPropertyEditDialog(foodItem, "carbs", "Carbs", foodItem.carbs.toString(), true)
                }

                dialog.window?.setBackgroundDrawable(ColorDrawable(0))
                dialog.show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error showing food item details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val foodItem = foodList[position]
        holder.nameTextView.text = foodItem.name
        holder.priorityTextView.text = "Calories: ${foodItem.calories}"
    }

    override fun getItemCount(): Int = foodList.size

    fun updateData(newList: List<FoodItem>) {
        foodList = newList
        notifyDataSetChanged()
    }

    fun searchDataList(searchList: ArrayList<FoodItem>) {
        foodList = searchList
        notifyDataSetChanged()
    }
}