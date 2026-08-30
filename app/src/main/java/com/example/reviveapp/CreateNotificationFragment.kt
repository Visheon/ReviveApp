package com.example.reviveapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class CreateNotificationFragment : Fragment() {
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var nameInput: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private var editingNotification: NotificationItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_notification, container, false)

        // Initialize views
        hourPicker = view.findViewById(R.id.hourPicker)
        minutePicker = view.findViewById(R.id.minutePicker)
        nameInput = view.findViewById(R.id.notificationNameInput)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        // Set up number pickers
        setupTimePickers()

        // Get editing notification if exists
        arguments?.let {
            val CreateEditTitle = view?.findViewById<TextView>(R.id.createEditTitle)
            CreateEditTitle?.text = "Edit"
            val notificationId = it.getString("notificationId")
            if (notificationId != null) {
                editingNotification = NotificationItem(
                    id = notificationId,
                    name = it.getString("name") ?: "",
                    hour = it.getInt("hour"),
                    minute = it.getInt("minute"),
                    isEnabled = it.getBoolean("isEnabled")
                )
                // Update UI with notification data
                nameInput.setText(editingNotification?.name)
                hourPicker.value = editingNotification?.hour ?: 0
                minutePicker.value = editingNotification?.minute ?: 0
            }
        }

        // Set up button clicks
        saveButton.setOnClickListener { saveNotification() }
        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun setupTimePickers() {
        // Set up hour picker
        hourPicker.apply {
            minValue = 0
            maxValue = 23
            setFormatter { value -> String.format("%02d", value) }
        }

        // Set up minute picker
        minutePicker.apply {
            minValue = 0
            maxValue = 59
            setFormatter { value -> String.format("%02d", value) }
        }
    }

    private fun saveNotification() {
        val name = nameInput.text.toString()
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter a name for the notification", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val hour = hourPicker.value
        val minute = minutePicker.value
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val notification = editingNotification?.copy(
            name = name,
            hour = hour,
            minute = minute
        ) ?: NotificationItem(
            id = UUID.randomUUID().toString(),
            name = name,
            hour = hour,
            minute = minute,
            userId = userId
        )

        // Save to Firebase
        val database =
            FirebaseDatabase.getInstance()
        database.getReference("Information")
            .child(userId)
            .child("Notifications")
            .child(notification.id)
            .setValue(notification)
            .addOnSuccessListener {
                // Schedule the notification if it's enabled
                if (notification.isEnabled) {
                    context?.let { ctx ->
                        NotificationWorker.scheduleNotification(ctx, notification)
                    }
                }
                Toast.makeText(context, "Notification saved and scheduled", Toast.LENGTH_SHORT)
                    .show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save notification", Toast.LENGTH_SHORT).show()
            }
    }
}