package com.example.reviveapp


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reviveapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.log

class SignupActivity : AppCompatActivity() {

    // Declaring binding for accessing views and FirebaseAuth for user authentication
    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge UI support
        binding = ActivitySignupBinding.inflate(layoutInflater) // Initializing binding
        setContentView(binding.root) // Setting the content view to the binding's root layout

        firebaseAuth = FirebaseAuth.getInstance() // Initializing FirebaseAuth instance

        // Adjust padding for system bars (like status and navigation bars) dynamically
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Listener for the "Sign Up" button
        binding.signupButton.setOnClickListener {
            val email = binding.signupEmail.text.toString() // Get email from input
            val password = binding.signupPassword.text.toString() // Get password from input
            val confirmPassword = binding.signupConfirm.text.toString() // Get confirm password from input

            // Check if all fields are filled
            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                // Check if password and confirm password match
                if (password == confirmPassword) {
                    // Attempt to create a new user in Firebase
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            // Redirect to login activity on successful sign-up
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            // Show error message if sign-up fails
                            Toast.makeText(this, "Incorrect Input", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Show error if passwords do not match
                    Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show error if any field is empty
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener for "Log In" text to redirect users to the login screen
        binding.loginRedirectText.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}