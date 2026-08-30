package com.example.reviveapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.database.Exclude
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class NotificationItem(
    var id: String = "", // Unique identifier for the notification
    var name: String = "", // Name/description of the notification
    var hour: Int = 0, // Hour in 24-hour format
    var minute: Int = 0, // Minute
    var isEnabled: Boolean = true, // Whether the notification is active
    var userId: String = "", // To associate notifications with specific users

) {

    constructor() : this("", "", 0, 0, true, "")

    // Computed properties
    @get:Exclude
    val timeInMinutes: Int
        get() = hour * 60 + minute

    @get:Exclude
    val formattedTime: String
        get() = String.format("%02d:%02d", hour, minute)

}



class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun doWork(): Result {
        return try {
            val title = inputData.getString(KEY_NOTIFICATION_TITLE) ?: run {
                return Result.failure()
            }

            showNotification(title)
            scheduleNextNotification()
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error in doWork", e)
            Result.failure()
        }
    }

    private fun showNotification(title: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.revive_logo)
            .setContentTitle(title)
            .setContentText("Time to log your meal!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.d("NotificationWorker", "Notification shown successfully with id: $notificationId")
    }

    private fun scheduleNextNotification() {
        val notificationId = inputData.getString("notification_id") ?: return
        scheduleNotification(context, notificationId)
    }

    companion object {
        private const val CHANNEL_ID = "meal_reminder_channel"
        const val KEY_NOTIFICATION_TITLE = "notification_title"

        fun scheduleNotification(context: Context, notification: NotificationItem) {
            try {
                Log.d("NotificationWorker", "Scheduling notification: ${notification.name} for ${notification.formattedTime}")

                val data = workDataOf(
                    KEY_NOTIFICATION_TITLE to notification.name,
                    "notification_id" to notification.id
                )

                // Create a flex interval of 10 minutes
                val flexInterval = 10L

                val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                    24, TimeUnit.HOURS,  // Repeat interval
                    flexInterval, TimeUnit.MINUTES  // Flex interval
                )
                    .setInputData(data)
                    .addTag(notification.id)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        notification.id,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest
                    )

                Log.d("NotificationWorker", "Work request enqueued successfully")
            } catch (e: Exception) {
                Log.e("NotificationWorker", "Error scheduling notification", e)
            }
        }

        fun scheduleNotification(context: Context, notificationId: String) {
            WorkManager.getInstance(context)
                .getWorkInfosByTagLiveData(notificationId)
                .observeForever { workInfos ->
                    Log.d("NotificationWorker", "Work status: ${workInfos?.firstOrNull()?.state}")
                    if (workInfos.isNullOrEmpty()) {
                        // Re-schedule if work info is not found
                        val data = workDataOf(
                            "notification_id" to notificationId
                        )

                        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                            24, TimeUnit.HOURS,
                            15, TimeUnit.MINUTES
                        )
                            .setInputData(data)
                            .addTag(notificationId)
                            .build()

                        WorkManager.getInstance(context)
                            .enqueueUniquePeriodicWork(
                                notificationId,
                                ExistingPeriodicWorkPolicy.REPLACE,
                                workRequest
                            )
                    }
                }
        }
        fun cancelNotification(context: Context, notificationId: String) {
            try {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(notificationId)
                Log.d("NotificationWorker", "Cancelled notification with id: $notificationId")
            } catch (e: Exception) {
                Log.e("NotificationWorker", "Error cancelling notification", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Meal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to log your meals"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
                setShowBadge(true)
                enableLights(true)
                lightColor = Color.GREEN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
