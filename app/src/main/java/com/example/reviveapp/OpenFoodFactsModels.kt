package com.example.reviveapp

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single product as returned by the Open Food Facts search endpoint,
 * trimmed to just the fields we requested via `fields=`.
 *
 * The nutrient values are nullable on purpose: Open Food Facts is
 * crowdsourced, so a product can exist with a name but incomplete or
 * missing nutrition data. We keep that gap visible here rather than
 * quietly defaulting to 0 — a 0 would look like a real "0 calorie" food,
 * which is a lie we don't want to tell the user.
 */
data class OffProduct(
    val barcode: String,
    val name: String,
    val caloriesPer100g: Double?,
    val proteinsPer100g: Double?,
    val fatsPer100g: Double?,
    val carbsPer100g: Double?
) {
    companion object {
        // Builds one OffProduct from one entry in the "products" JSON array.
        fun fromJson(product: JSONObject): OffProduct {
            val nutriments = product.optJSONObject("nutriments")

            return OffProduct(
                barcode = product.optString("code", ""),
                name = product.optString("product_name", "").trim(),
                caloriesPer100g = nutriments?.optDoubleOrNull("energy-kcal_100g"),
                proteinsPer100g = nutriments?.optDoubleOrNull("proteins_100g"),
                fatsPer100g = nutriments?.optDoubleOrNull("fat_100g"),
                carbsPer100g = nutriments?.optDoubleOrNull("carbohydrates_100g")
            )
        }
    }
}

// Walks the full search response and returns every product it found.
// If the "products" key is missing entirely (shouldn't happen, but the
// server is someone else's code, not ours), we return an empty list
// instead of crashing.
fun parseOffSearchResponse(responseJson: JSONObject): List<OffProduct> {
    val productsArray = responseJson.optJSONArray("products") ?: JSONArray()
    return (0 until productsArray.length()).map { index ->
        OffProduct.fromJson(productsArray.getJSONObject(index))
    }
}

// org.json's built-in JSONObject.optDouble() returns Double.NaN when a key
// is missing, rather than null — awkward to check for every time. This
// wraps it into an honest, nullable Double so the rest of the code can
// just use ?. and ?: like everywhere else in your Kotlin.
private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val value = optDouble(key)
    return if (value.isNaN()) null else value
}

// Turns a possibly-incomplete Open Food Facts result into a full FoodItem,
// or null if it's missing a name or any of the four macros. FoodItem's
// fields are non-nullable, so this is the one place that enforces
// "no half-known foods make it into the app" — everything downstream of
// this function can assume a FoodItem is complete, the same guarantee
// you already get from foods entered by hand in ItemFragment.
fun OffProduct.toFoodItemOrNull(): FoodItem? {
    if (name.isBlank()) return null
    val calories = caloriesPer100g ?: return null
    val proteins = proteinsPer100g ?: return null
    val fats = fatsPer100g ?: return null
    val carbs = carbsPer100g ?: return null

    // These are still per-100g values here — the same representation your
    // Firebase-sourced FoodItems are in right after convertto100gram() is
    // called for display (see HomeFragment's showFoodSelectionDialog).
    // Converting down to per-1g only happens at selection time, in step 6,
    // exactly the way the existing Firebase food flow already does it via
    // selectedFood.convertto1gram(). Same shape in, same conversion point —
    // that's what lets us reuse the rest of the pipeline unmodified.
    return FoodItem(
        name = name,
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbs = carbs
    )
}