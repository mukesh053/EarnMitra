package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val productNames: String, // Comma separated items with quantities (e.g. "Peanut Oil (x2), Khadi Kurta (x1)")
    val totalPrice: Double,
    val shippingAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "DELIVERED", // "PENDING", "SHIPPED", "DELIVERED"
    val paymentMethod: String = "UPI", // "UPI", "WALLET", "COD"
    val paymentStatus: String = "PAID", // "PAID", "PENDING"
    val paymentRef: String = "" // Added to support UTR numbers / reference details
)
