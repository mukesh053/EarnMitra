package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM user_accounts WHERE uid = :uid")
    suspend fun getUserByUid(uid: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE uid = :uid")
    fun getUserByUidFlow(uid: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE phoneNumber = :phone")
    suspend fun getUsersByPhone(phone: String): List<UserAccount>

    @Query("SELECT * FROM user_accounts WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts")
    suspend fun getAllUsers(): List<UserAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("SELECT * FROM transactions WHERE uid = :uid ORDER BY timestamp DESC")
    fun getTransactionsByUidFlow(uid: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE description LIKE :pattern")
    suspend fun findTransactionsByDescription(pattern: String): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM notifications WHERE uid = :uid ORDER BY timestamp DESC")
    fun getNotificationsByUidFlow(uid: String): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE uid = :uid")
    suspend fun markAllNotificationsAsRead(uid: String)

    @Query("DELETE FROM notifications WHERE uid = :uid")
    suspend fun clearAllNotifications(uid: String)

    @Query("SELECT * FROM orders WHERE uid = :uid ORDER BY timestamp DESC")
    fun getOrdersByUidFlow(uid: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Query("DELETE FROM user_accounts WHERE uid = :uid")
    suspend fun deleteUserByUid(uid: String)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE paymentStatus = 'PENDING_VERIFICATION'")
    suspend fun getPendingPaymentUsers(): List<UserAccount>
}
