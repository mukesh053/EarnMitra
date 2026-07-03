package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val uid: String, // Unique ID generated on registration (e.g. EM10001)
    val phoneNumber: String,
    val fullName: String,
    val isActive: Boolean = false, // Becomes true after paying ₹1,000 joining fee
    val referredBy: String? = null, // Who referred this user
    val directReferralsCount: Int = 0, // Max 3
    val walletBalance: Double = 0.0,
    val isTwoStepEnabled: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 23.0225, // Default Ahmedabad Gujarat
    val longitude: Double = 72.5714,
    val notifPromo: Boolean = true,
    val notifTrans: Boolean = true,
    val notifSystem: Boolean = true,
    val aadharNumber: String = "",
    val panNumber: String = "",
    val profileImage: String? = null, // Path or code for selected avatar
    val securityPin: String? = null, // 4-digit security PIN for app lock and transactions
    val isSecurityLockEnabled: Boolean = false, // If true, requires PIN on startup
    val realOtpEnabled: Boolean = false, // If true, tries to send real SMS
    val twilioSid: String = "", // Twilio Configuration Sid
    val twilioToken: String = "", // Twilio Auth Token
    val twilioFromPhone: String = "", // Twilio Sender Number
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfscCode: String = "",
    val bankUpiId: String = "",
    val kycStatus: String = "NOT_STARTED", // "NOT_STARTED", "PENDING", "APPROVED"
    val kycDocImage: String? = null, // Simulated uploaded document image (e.g. URI or mock text)
    val email: String = "",
    val password: String = "",
    val authMethod: String = "OTP" // "OTP", "WHATSAPP", "GOOGLE", "EMAIL"
)

