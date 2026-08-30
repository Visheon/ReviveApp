package com.example.reviveapp

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt
import kotlin.math.roundToLong


data class FoodItem(
    var name: String = "",
    var calories: Double = 0.0,
    var proteins: Double = 0.0,
    var fats: Double = 0.0,
    var carbs: Double = 0.0
) {
    // Calculates 1g amounts from 100g

    fun convertto1gram() {
        calories = ((calories / 100.0) * 100).roundToInt() / 100.0
        proteins = ((proteins / 100.0) * 100).roundToInt() / 100.0
        fats = ((fats / 100.0) * 100).roundToInt() / 100.0
        carbs = ((carbs / 100.0) * 100).roundToInt() / 100.0
    }

    // Calculates 1g amount to 100g
    fun convertto100gram() {
        calories = ((calories * 100.0) * 100).roundToInt() / 100.0
        proteins = ((proteins * 100.0) * 100).roundToInt() / 100.0
        fats = ((fats * 100.0) * 100).roundToInt() / 100.0
        carbs = ((carbs * 100.0) * 100).roundToInt() / 100.0
    }
}

data class Meal(
    var name: String = "",
    var foodItems: MutableMap<String, FoodItemWithQuantity> = mutableMapOf(),
    var totalCalories: Double = 0.0,
    var totalProteins: Double = 0.0,
    var totalFats: Double = 0.0,
    var totalCarbs: Double = 0.0
) {
    fun updateTotals() {
        totalCalories = foodItems.values.sumOf { it.calories * it.grams }
        totalProteins = foodItems.values.sumOf { it.proteins * it.grams }
        totalFats = foodItems.values.sumOf { it.fats * it.grams }
        totalCarbs = foodItems.values.sumOf { it.carbs * it.grams }
    }
}

data class FoodItemWithQuantity(
    val foodItem: FoodItem = FoodItem(), // Add default value
    var grams: Double = 0.0
) {
    // Add no-argument constructor for Firebase
    constructor() : this(FoodItem(), 0.0)

    val calories: Double get() = foodItem.calories
    val proteins: Double get() = foodItem.proteins
    val fats: Double get() = foodItem.fats
    val carbs: Double get() = foodItem.carbs
}



