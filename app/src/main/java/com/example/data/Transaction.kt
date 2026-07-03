package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String, // Owner's unique id
    val type: String, // "DEPOSIT", "WITHDRAW_UPI", "WITHDRAW_BANK", "COMMISSION", "SHOPPING", "LOCAL_PAY"
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
