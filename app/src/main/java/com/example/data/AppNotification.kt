package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String, // Recipient user ID
    val title: String, // Notification Title (e.g., "કમિશન જમા થયું! / Commission Credited!")
    val message: String, // Detailed message
    val type: String, // "DEPOSIT", "COMMISSION", "WITHDRAWAL", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
