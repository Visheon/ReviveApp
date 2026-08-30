package com.example.reviveapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Importing the required libraries
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.regex.Pattern;

public class CalorieCalculatorFragment extends Fragment {

    private TextInputEditText age, height, weight;
    private RadioGroup gender;
    private MaterialRadioButton male, female;
    private TextView calories, required, textView1, textView2, textView3, textView4, textView5, textView6, text_dummy;
    private AppCompatButton calculate, reset;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_calorie_calculator, container, false);
        // Initializing the variables
        age = view.findViewById(R.id.age);
        height = view.findViewById(R.id.height);
        weight = view.findViewById(R.id.weight);
        gender = view.findViewById(R.id.gender);
        male = view.findViewById(R.id.male);
        female = view.findViewById(R.id.female);
        calories = view.findViewById(R.id.calories);
        textView1 = view.findViewById(R.id.textView1);
        textView2 = view.findViewById(R.id.textView2);
        textView3 = view.findViewById(R.id.textView3);
        textView4 = view.findViewById(R.id.textView4);
        textView5 = view.findViewById(R.id.textView5);
        textView6 = view.findViewById(R.id.textView6);
        text_dummy = view.findViewById(R.id.text_dummy);
        required = view.findViewById(R.id.required);
        calculate = view.findViewById(R.id.calculate);
        reset = view.findViewById(R.id.reset);
        // Creating the onClickListener for the reset button
        // This will reset all the values to the default values
        reset.setOnClickListener(v -> {
            age.setText("");
            height.setText("");
            weight.setText("");
            gender.clearCheck();
            calories.setText("Calories");
            textView1.setText("");
            textView2.setText("");
            textView3.setText("");
            textView4.setText("");
            textView5.setText("");
            textView6.setText("");
            text_dummy.setVisibility(View.GONE);
            required.setVisibility(View.GONE);
        });
        // Creating the onClickListener for the calculate button
        calculate.setOnClickListener(v -> {
            // Getting the values from the text fields
            String ageText = age.getText().toString();
            String heightText = height.getText().toString();
            String weightText = weight.getText().toString();
            // Creating the pattern for the regular expression
            // This will check if the value is a number or not
            Pattern pattern = Pattern.compile("[0-9]+");
            // Creating the variables for the checks and setting them to false
            // These will be used to check if the values are empty or not
            boolean ageCheck = false;
            boolean heightCheck = false;
            boolean weightCheck = false;
            // Checking if the age text field is empty or not
            // If it is empty, then it will show an error message
            if (ageText.isEmpty()) {
                age.setError("Please enter your age");
                age.requestFocus();
                ageCheck = false;
            } else if (!pattern.matcher(ageText).matches()) {
                age.setError("Please enter your age correctly");
                age.requestFocus();
                ageCheck = false;
            } else {
                age.setError(null);
                ageCheck = true;
            }
            // Checking if the height text field is empty or not
            // If it is empty, then it will show an error message
            if (heightText.isEmpty()) {
                height.setError("Please enter your height");
                height.requestFocus();
                heightCheck = false;
            } else if (!pattern.matcher(ageText).matches()) {
                age.setError("Please enter your age correctly");
                age.requestFocus();
                heightCheck = false;
            } else {
                height.setError(null);
                heightCheck = true;
            }
            // Checking if the weight text field is empty or not
            // If it is empty, then it will show an error message
            if (weightText.isEmpty()) {
                weight.setError("Please enter your weight");
                weight.requestFocus();
                weightCheck = false;
            } else if (!pattern.matcher(ageText).matches()) {
                age.setError("Please enter your age correctly");
                age.requestFocus();
                weightCheck = false;
            } else {
                weight.setError(null);
                weightCheck = true;
            }
            // Checking if the user has selected the gender or not
            if (gender.getCheckedRadioButtonId() == -1) {
                required.setText("Please Select Gender");
                required.setVisibility(View.VISIBLE);
            } else {
                required.setText("");
                required.setVisibility(View.GONE);

                // Checking if all the values are not empty
                if (ageCheck && heightCheck && weightCheck) {

                    // Calling the calculateBMR method
                    calculateCalorie();
                }
            }
        });

        return view;
    }
    // Creating the calculate method to calculate the calories required
    public void calculateCalorie(){

        // Getting the values from the text fields
        int ageValue = Integer.parseInt(age.getText().toString());
        int heightValue = Integer.parseInt(height.getText().toString());
        int weightValue = Integer.parseInt(weight.getText().toString());

        // Creating the variable for the total calories
        double totalCalories = 0;

        if(gender.getCheckedRadioButtonId()== male.getId()){
            // If user is "Male" then the following formula will be used to calculate the calories
            totalCalories = (66.47 + (13.75 * weightValue ) + (5.003 * heightValue )) - (6.755 * ageValue);
            calories.setText(String.format("%.2f", totalCalories)+"*");
            // Setting the text to the calories text view
            text_dummy.setVisibility(View.VISIBLE);
        } else {
            // If user is "Female" then the following formula will be used to calculate the calories
            totalCalories = (655.1 +  (9.563 * weightValue ) + (1.850 * heightValue)) - (4.676 * ageValue);
            calories.setText(String.format("%.2f", totalCalories)+"*");
            text_dummy.setVisibility(View.VISIBLE);
        }

        // Setting the text to the calories in the table layout and rounding it to 2 decimal places
        textView1.setText(String.format("%.2f", (totalCalories*1.2)-500));
        textView2.setText(String.format("%.2f", (totalCalories*1.375)-500));
        textView3.setText(String.format("%.2f", (totalCalories*1.55)-500));
        textView4.setText(String.format("%.2f", (totalCalories*1.725)-500));
        textView5.setText(String.format("%.2f", (totalCalories*1.8)-500));
        textView6.setText(String.format("%.2f", (totalCalories*1.89)-500));

        // Setting the text to the text view and making it visible
        required.setText("*"+"Calculation is based on the Harris-Benedict formula");
        required.setTextSize(12);
        required.setVisibility(View.VISIBLE);
    }
}