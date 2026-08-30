package com.example.reviveapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.math.BigDecimal
import java.math.RoundingMode

class HomeMealAdapter(private val onItemClick: (FoodItemWithQuantity) -> Unit) :
    RecyclerView.Adapter<HomeMealAdapter.FoodViewHolder>() {

    private var foods = mutableListOf<FoodItemWithQuantity>()

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.recTitle)
        val detailsTextView: TextView = itemView.findViewById(R.id.recPriority)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(foods[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        holder.nameTextView.text = "${food.foodItem.name} (${food.grams.roundToDecimal(1)}g)"
        holder.detailsTextView.text = "${(food.calories * food.grams).roundToDecimal(1)} kcal"

    }

    override fun getItemCount() = foods.size

    fun updateFoods(newFoods: List<FoodItemWithQuantity>) {
        foods.clear()
        foods.addAll(newFoods)
        notifyDataSetChanged()
    }

    private fun Double.roundToDecimal(decimals: Int): Double {
        return BigDecimal(this)
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}