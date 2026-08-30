package com.example.reviveapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.math.BigDecimal
import java.math.RoundingMode

class MealAdapter : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {
    interface MealClickListener {
        fun onMealClick(meal: Meal)
        fun onMealLongClick(meal: Meal)
    }

    private var meals = mutableListOf<Meal>()
    private var filteredMeals = mutableListOf<Meal>()

    init {
        filteredMeals = meals
    }



    fun setMealClickListener(listener: MealClickListener) {
        clickListener = listener
    }

    private var clickListener: MealClickListener? = null

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.recTitle)
        val detailsTextView: TextView = itemView.findViewById(R.id.recPriority)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    clickListener?.onMealClick(filteredMeals[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    clickListener?.onMealLongClick(filteredMeals[position])
                }
                true
            }
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = filteredMeals[position]
        holder.nameTextView.text = meal.name
        holder.detailsTextView.text = "Calories: ${meal.totalCalories.roundToDecimal(1)}"
    }

    override fun getItemCount() = filteredMeals.size

    fun updateMeals(newMeals: List<Meal>) {
        meals.clear()
        meals.addAll(newMeals)
        filteredMeals = meals.toMutableList()
        notifyDataSetChanged()
    }

    fun searchMeals(query: String) {
        filteredMeals = if (query.isEmpty()) {
            meals.toMutableList()
        } else {
            meals.filter {
                it.name.toLowerCase().contains(query.toLowerCase())
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    private fun Double.roundToDecimal(decimals: Int): Double {
        return BigDecimal(this)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}



class SelectedFoodsAdapter : RecyclerView.Adapter<SelectedFoodsAdapter.SelectedFoodViewHolder>() {
    private var selectedFoods = mutableListOf<FoodItemWithQuantity>()
    private var onItemClickListener: ((FoodItemWithQuantity) -> Unit)? = null

    fun setOnItemClickListener(listener: (FoodItemWithQuantity) -> Unit) {
        onItemClickListener = listener
    }

    inner class SelectedFoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.recTitle)
        val detailsTextView: TextView = itemView.findViewById(R.id.recPriority)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(selectedFoods[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedFoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return SelectedFoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedFoodViewHolder, position: Int) {
        val item = selectedFoods[position]
        holder.nameTextView.text = "${item.foodItem.name} (${item.grams}g)"
        holder.detailsTextView.text = "Cal: ${(item.calories * item.grams).roundToDecimal(1)}"
    }

    override fun getItemCount() = selectedFoods.size

    fun updateItems(items: List<FoodItemWithQuantity>) {
        selectedFoods.clear()
        selectedFoods.addAll(items)
        notifyDataSetChanged()
    }

    private fun Double.roundToDecimal(decimals: Int): Double {
        return BigDecimal(this)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}