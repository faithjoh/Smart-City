package com.smartcity.parkingapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.model.Notification
import com.smartcity.parkingapp.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val listener: OnNotificationClickListener
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    interface OnNotificationClickListener {
        fun onNotificationClick(notification: Notification, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_notification,
            parent,
            false
        )
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.notification_title)
        private val messageTextView: TextView = itemView.findViewById(R.id.notification_message)
        private val timeTextView: TextView = itemView.findViewById(R.id.notification_time)
        private val iconImageView: ImageView = itemView.findViewById(R.id.notification_icon)
        private val unreadIndicator: View = itemView.findViewById(R.id.unread_indicator)
        
        fun bind(notification: Notification) {
            titleTextView.text = notification.title
            messageTextView.text = notification.message
            timeTextView.text = formatTime(notification.timestamp)
            
            // Set unread indicator visibility
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Set icon and background tint based on notification type
            setNotificationStyle(notification.type)
            
            // Set click listener
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNotificationClick(notification, position)
                }
            }
        }
        
        private fun formatTime(timestamp: Date): String {
            val now = Date()
            val diffInMillis = now.time - timestamp.time
            
            return when {
                diffInMillis < 60 * 1000 -> "Just now"
                diffInMillis < 60 * 60 * 1000 -> "${diffInMillis / (60 * 1000)} minutes ago"
                diffInMillis < 24 * 60 * 60 * 1000 -> "${diffInMillis / (60 * 60 * 1000)} hours ago"
                diffInMillis < 48 * 60 * 60 * 1000 -> "Yesterday"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
            }
        }
        
        private fun setNotificationStyle(type: NotificationType) {
            val context = itemView.context
            
            // Set icon and color based on notification type
            when (type) {
                NotificationType.PAYMENT_SUCCESS -> {
                    iconImageView.setImageResource(R.drawable.ic_info)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.green))
                }
                NotificationType.PAYMENT_DUE -> {
                    iconImageView.setImageResource(R.drawable.ic_alert)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.red))
                }
                NotificationType.PARKING_ENTRY -> {
                    iconImageView.setImageResource(R.drawable.ic_directions)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.blue))
                }
                NotificationType.PARKING_EXIT -> {
                    iconImageView.setImageResource(R.drawable.ic_directions)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.orange))
                }
                NotificationType.ALERT -> {
                    iconImageView.setImageResource(R.drawable.ic_alert)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.orange))
                }
                NotificationType.SYSTEM -> {
                    iconImageView.setImageResource(R.drawable.ic_dialer)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.purple))
                }
                else -> {
                    iconImageView.setImageResource(R.drawable.ic_info)
                    iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.green))
                }
            }
        }
    }
} 