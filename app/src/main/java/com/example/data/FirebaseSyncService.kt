package com.example.data

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

object FirebaseSyncService {
    private const val TAG = "FirebaseSyncService"
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // Sync user account to Cloud Firestore
    fun saveUserToFirestore(user: UserAccount, onComplete: (Boolean) -> Unit = {}) {
        val userMap = hashMapOf(
            "uid" to user.uid,
            "phoneNumber" to user.phoneNumber,
            "fullName" to user.fullName,
            "isActive" to user.isActive,
            "referredBy" to user.referredBy,
            "directReferralsCount" to user.directReferralsCount,
            "walletBalance" to user.walletBalance,
            "isTwoStepEnabled" to user.isTwoStepEnabled,
            "createdTimestamp" to user.createdTimestamp,
            "latitude" to user.latitude,
            "longitude" to user.longitude,
            "notifPromo" to user.notifPromo,
            "notifTrans" to user.notifTrans,
            "notifSystem" to user.notifSystem,
            "aadharNumber" to user.aadharNumber,
            "panNumber" to user.panNumber,
            "profileImage" to user.profileImage,
            "kycStatus" to user.kycStatus,
            "kycDocImage" to user.kycDocImage,
            "email" to user.email,
            "password" to user.password,
            "authMethod" to user.authMethod,
            "bankName" to user.bankName,
            "bankAccountNumber" to user.bankAccountNumber,
            "bankIfscCode" to user.bankIfscCode,
            "bankUpiId" to user.bankUpiId,
            "lastSynced" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User ${user.uid} successfully synced to Firestore!")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing user ${user.uid} to Firestore", e)
                onComplete(false)
            }
    }

    // Sync transaction to Cloud Firestore
    fun saveTransactionToFirestore(transaction: Transaction) {
        val transactionId = if (transaction.id == 0L) {
            "${transaction.uid}_${transaction.timestamp}"
        } else {
            transaction.id.toString()
        }

        val txMap = hashMapOf(
            "id" to transaction.id,
            "uid" to transaction.uid,
            "type" to transaction.type,
            "amount" to transaction.amount,
            "description" to transaction.description,
            "timestamp" to transaction.timestamp
        )

        firestore.collection("transactions")
            .document(transactionId)
            .set(txMap)
            .addOnSuccessListener {
                Log.d(TAG, "Transaction synced: $transactionId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing transaction", e)
            }
    }

    // Sync order to Cloud Firestore
    fun saveOrderToFirestore(order: Order) {
        val orderId = if (order.id == 0L) {
            "${order.uid}_${order.timestamp}"
        } else {
            order.id.toString()
        }

        val orderMap = hashMapOf(
            "id" to order.id,
            "uid" to order.uid,
            "productNames" to order.productNames,
            "totalPrice" to order.totalPrice,
            "shippingAddress" to order.shippingAddress,
            "timestamp" to order.timestamp,
            "status" to order.status
        )

        firestore.collection("orders")
            .document(orderId)
            .set(orderMap)
            .addOnSuccessListener {
                Log.d(TAG, "Order synced: $orderId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing order", e)
            }
    }

    // Sync app notification to Cloud Firestore
    fun saveNotificationToFirestore(notification: AppNotification) {
        val notifId = if (notification.id == 0L) {
            "${notification.uid}_${notification.timestamp}"
        } else {
            notification.id.toString()
        }

        val notifMap = hashMapOf(
            "id" to notification.id,
            "uid" to notification.uid,
            "title" to notification.title,
            "message" to notification.message,
            "type" to notification.type,
            "timestamp" to notification.timestamp,
            "isRead" to notification.isRead
        )

        firestore.collection("notifications")
            .document(notifId)
            .set(notifMap)
            .addOnSuccessListener {
                Log.d(TAG, "Notification synced: $notifId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing notification", e)
            }
    }

    // Upload KYC Document to Firebase Storage & get Download URL
    fun uploadKycDocument(uid: String, fileUri: Uri, onComplete: (String?) -> Unit) {
        val storageRef = storage.reference.child("kyc_documents/$uid/document.jpg")
        
        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        Log.d(TAG, "KYC Document uploaded successfully! URL: $downloadUri")
                        onComplete(downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting KYC Document download URL", e)
                        onComplete(null)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading KYC Document to Firebase Storage", e)
                onComplete(null)
            }
    }

    // Save update configuration to Cloud Firestore
    fun saveUpdateConfigToFirestore(versionName: String, versionCode: Int, releaseNotes: String, driveUrl: String, onComplete: (Boolean) -> Unit = {}) {
        val updateMap = hashMapOf(
            "versionName" to versionName,
            "versionCode" to versionCode.toLong(),
            "releaseNotes" to releaseNotes,
            "googleDriveUpdateUrl" to driveUrl,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("metadata")
            .document("update_config")
            .set(updateMap)
            .addOnSuccessListener {
                Log.d(TAG, "Update configuration successfully saved to Firestore!")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving update configuration to Firestore", e)
                onComplete(false)
            }
    }

    // Fetch update configuration from Cloud Firestore
    fun fetchUpdateConfigFromFirestore(onComplete: (versionName: String?, versionCode: Int?, releaseNotes: String?, driveUrl: String?) -> Unit) {
        firestore.collection("metadata")
            .document("update_config")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val versionName = document.getString("versionName")
                    val versionCode = document.getLong("versionCode")?.toInt()
                    val releaseNotes = document.getString("releaseNotes")
                    val driveUrl = document.getString("googleDriveUpdateUrl")
                    onComplete(versionName, versionCode, releaseNotes, driveUrl)
                } else {
                    onComplete(null, null, null, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching update config from Firestore", e)
                onComplete(null, null, null, null)
            }
    }
}
