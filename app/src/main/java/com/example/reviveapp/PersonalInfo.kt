package com.example.reviveapp

data class PersonalInfo(
    var name: String = "",
    var age: Int = 0,
    var height: Int = 0,
    var weight: Int = 0,
    var sex: String = "",
    var activityLevel: String = "",
    var calories: Double = 0.0,
    var deficitcalories: Double = 0.0,
    var protein: Int = 0,
    var fats: Int = 0,
    var carbs: Int = 0,
    var proteinpercent: Double = 0.3,
    var fatspercent: Double = 0.3,
    var carbspercent: Double = 0.4
){
    fun calculatecalories(){
        if (sex == "Male"){
            calories = 66.5 + (13.75 * weight) + (5.003 * height) - (6.775 * age)
                if (calories < 1200){
                    calories = 1200.0
                }
        }else {
            calories = 655.1 + (9.563* weight) + (1.85 * height) - (4.676 * age)
            if (calories < 1200){
                calories = 1200.0
            }
        }

        if (activityLevel == "Little"){
            calories = 1 * calories
        } else if (activityLevel == "Light"){
            calories = 1.2 * calories
        } else if (activityLevel == "Moderate"){
            calories = 1.35 * calories
        } else if (activityLevel == "Active"){
            calories = 1.5 * calories
        } else{
            calories = 1.7 * calories
        }

        // rounds
        calories = kotlin.math.round(calories)
        deficitcalories = kotlin.math.round(calories - 500)
    }

    fun calculatemacros(){
        if (calories != 0.0){
            protein = ((deficitcalories * proteinpercent) / 4).toInt()
            carbs = ((deficitcalories * carbspercent) / 4).toInt()
            fats = ((deficitcalories * fatspercent) / 9).toInt()
        }
    }

}
