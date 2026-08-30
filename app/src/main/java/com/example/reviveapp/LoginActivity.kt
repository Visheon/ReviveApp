package com.example.reviveapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reviveapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(3000)  // waits with the splashscreen for 2 seconds
        installSplashScreen()  // sets the splashscreen
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()   // sets variable for the firebase imported procedure

        if (firebaseAuth.getCurrentUser() != null) {
            val userid = FirebaseAuth.getInstance().currentUser?.uid
            if (userid != null){
                databaseReference = FirebaseDatabase.getInstance().getReference("Information").child(userid)
                databaseReference.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // If data exists, skip the form and go to the next activity
                        // Navigate to the next activity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                    } else {
                        val intent = Intent(this, PersonalDetailsActivity::class.java)
                        startActivity(intent)  // moves to the next screen if password and email are correct

                    }
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.loginButton.setOnClickListener{   // waits for the login button to be clicked
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {  // makes sure the two email and password fields are not left empty
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
                    if (it.isSuccessful){
                        Log.d("LoginActivity", "Authentication successful")
                        val userid = FirebaseAuth.getInstance().currentUser?.uid
                        if (userid != null){
                            databaseReference = FirebaseDatabase.getInstance().getReference("Information").child(userid)
                            databaseReference.get().addOnSuccessListener { snapshot ->
                                Log.d("LoginActivity", "Database snapshot exists: ${snapshot.exists()}")
                                if (snapshot.child("personal information").exists()) {
                                    // If data exists, skip the form and go to the next activity
                                    // Navigate to the next activity

                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    val intent = Intent(this, PersonalDetailsActivity::class.java)
                                    startActivity(intent)  // moves to the next screen if password and email are correct
                                }
                            }
                        }else{
                            Toast.makeText(this, "Authentication is Failed", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()  // returns an error if wrong
                    }
                }
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show() // returns an error if fields are empty
            }
        }
        binding.forgotPassword.setOnClickListener{  // listens to see if forgot password text is clicked
            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_forgot, null) // calls dialog_forgot xml
            val userEmail = view.findViewById<EditText>(R.id.editBox)
            builder.setView(view)
            val dialog = builder.create()
            view.findViewById<Button>(R.id.btnReset).setOnClickListener {
                compareEmail(userEmail) // makes sure email is real
                dialog.dismiss()
            }
            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss() // cancels operation
            }
            if (dialog.window != null){
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()
        }

        binding.signupRedirectText.setOnClickListener{  // listens for someone to click the "sign up now" text
            val signupIntent = Intent(this, SignupActivity::class.java)
            startActivity(signupIntent)
        }
    }
    //Outside onCreate
    private fun compareEmail(email: EditText){
        if (email.text.toString().isEmpty()){
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseAuth.fetchSignInMethodsForEmail(email.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        // Email doesn't exist in Firebase
                        Toast.makeText(this, "Email does not exist", Toast.LENGTH_SHORT).show()
                    } else {
                        // Email exists, send reset email
                        firebaseAuth.sendPasswordResetEmail(email.text.toString())
                            .addOnCompleteListener { resetTask ->
                                if (resetTask.isSuccessful) {
                                    Toast.makeText(this, "Check your email", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Error checking email", Toast.LENGTH_SHORT).show()
                }
            }
    }
}