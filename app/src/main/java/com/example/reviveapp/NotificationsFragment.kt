package com.example.reviveapp

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class NotificationsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var createButton: Button
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var databaseReference: DatabaseReference
    private val notifications = mutableListOf<NotificationItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkNotificationPermission()
        setupViews(view)
        setupFirebase()
        setupRecyclerView()
        loadNotifications()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        createButton = view.findViewById(R.id.createNotificationButton)

        createButton.setOnClickListener {
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, CreateNotificationFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun setupFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("Information")
            .child(userId ?: "")
            .child("Notifications")
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            requireContext(),
            notifications,
            onToggleChanged = { notification, isEnabled ->
                updateNotificationState(notification, isEnabled)
            },
            onItemLongClick = { notification ->
                showEditDeleteDialog(notification)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun loadNotifications() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(NotificationItem::class.java)
                    notification?.let { notifications.add(it) }
                }
                val sortedNotifications = notifications.sortedWith(compareBy({ it.hour }, { it.minute }))
                notificationAdapter.updateNotifications(sortedNotifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateNotificationState(notification: NotificationItem, isEnabled: Boolean) {
        notification.isEnabled = isEnabled
        databaseReference.child(notification.id).setValue(notification)
            .addOnSuccessListener {
                if (isEnabled) {
                    context?.let { ctx ->
                        NotificationWorker.scheduleNotification(ctx, notification)
                    }
                } else {
                    context?.let { ctx ->
                        NotificationWorker.cancelNotification(ctx, notification.id)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update notification", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDeleteDialog(notification: NotificationItem) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_meal_options, null)

        // Get UI elements
        val titleText = view.findViewById<TextView>(R.id.dialogTitle)
        val editButton = view.findViewById<Button>(R.id.editButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        titleText.text = "Notification Options"

        builder.setView(view)
        val dialog = builder.create()

        editButton.setOnClickListener {
            dialog.dismiss()
            editNotification(notification)
        }

        deleteButton.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(notification)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(notification: NotificationItem) {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_meal_options, null)

        val titleText = view.findViewById<TextView>(R.id.dialogTitle)
        val confirmButton = view.findViewById<Button>(R.id.editButton)
        val cancelButton = view.findViewById<Button>(R.id.deleteButton)
        val bottomButton = view.findViewById<Button>(R.id.cancelButton)

        titleText.text = "Delete Notification"
        confirmButton.text = "Delete"
        cancelButton.text = "Keep"
        confirmButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#400a06"))
        cancelButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#053411"))
        bottomButton.visibility = View.GONE

        builder.setView(view)
        val dialog = builder.create()

        confirmButton.setOnClickListener {
            dialog.dismiss()
            deleteNotification(notification)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        dialog.show()
    }

    private fun editNotification(notification: NotificationItem) {
        val fragment = CreateNotificationFragment().apply {
            arguments = Bundle().apply {
                putString("notificationId", notification.id)
                putInt("hour", notification.hour)
                putInt("minute", notification.minute)
                putString("name", notification.name)
                putBoolean("isEnabled", notification.isEnabled)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteNotification(notification: NotificationItem) {
        // Cancel scheduled notification first
        cancelNotification(notification)

        // Delete from Firebase
        databaseReference.child(notification.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete notification", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleNotification(notification: NotificationItem) {
        NotificationWorker.scheduleNotification(requireContext(), notification)
    }

    private fun cancelNotification(notification: NotificationItem) {
        NotificationWorker.cancelNotification(requireContext(), notification.id)
    }
}