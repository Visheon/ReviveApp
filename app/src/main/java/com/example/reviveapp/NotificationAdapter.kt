package com.example.reviveapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val context: Context,
    private var notifications: List<NotificationItem>,
    private val onToggleChanged: (NotificationItem, Boolean) -> Unit,
    private val onItemLongClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.notificationName)
        val timeTextView: TextView = itemView.findViewById(R.id.notificationTime)
        val toggleSwitch: Switch = itemView.findViewById(R.id.notificationToggle)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(notifications[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.nameTextView.text = notification.name
        holder.timeTextView.text = notification.formattedTime
        holder.toggleSwitch.isChecked = notification.isEnabled

        holder.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleChanged(notification, isChecked)
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<NotificationItem>) {
        this.notifications = newNotifications
        notifyDataSetChanged()
    }
}