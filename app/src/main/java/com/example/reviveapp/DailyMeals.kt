package com.example.reviveapp

data class DailyMeals(
    var date: String = "",
    var breakfast: MutableMap<String, FoodItemWithQuantity> = mutableMapOf(),
    var snacks: MutableMap<String, FoodItemWithQuantity> = mutableMapOf(),
    var dinner: MutableMap<String, FoodItemWithQuantity> = mutableMapOf(),
    var totalCalories: Double = 0.0,
    var totalProteins: Double = 0.0,
    var totalFats: Double = 0.0,
    var totalCarbs: Double = 0.0
) {
    fun updateTotals() {
        totalCalories = 0.0
        totalProteins = 0.0
        totalFats = 0.0
        totalCarbs = 0.0

        // Calculate totals from all meals
        listOf(breakfast, snacks, dinner).forEach { meal ->
            meal.values.forEach { foodItem ->
                totalCalories += foodItem.calories * foodItem.grams
                totalProteins += foodItem.proteins * foodItem.grams
                totalFats += foodItem.fats * foodItem.grams
                totalCarbs += foodItem.carbs * foodItem.grams
            }
        }
    }

    fun getMealCalories(meal: MutableMap<String, FoodItemWithQuantity>): Double {
        return meal.values.sumOf { it.calories * it.grams }
    }
}
