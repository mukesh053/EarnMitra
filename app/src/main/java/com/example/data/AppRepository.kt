package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    suspend fun getUserByUid(uid: String): UserAccount? {
        return appDao.getUserByUid(uid)
    }

    suspend fun getUserByEmail(email: String): UserAccount? {
        return appDao.getUserByEmail(email)
    }

    fun getUserByUidFlow(uid: String): Flow<UserAccount?> {
        return appDao.getUserByUidFlow(uid)
    }

    suspend fun getUsersByPhone(phone: String): List<UserAccount> {
        return appDao.getUsersByPhone(phone)
    }

    fun getTransactionsByUidFlow(uid: String): Flow<List<Transaction>> {
        return appDao.getTransactionsByUidFlow(uid)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        appDao.insertTransaction(transaction)
        try {
            FirebaseSyncService.saveTransactionToFirestore(transaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUser(user: UserAccount) {
        appDao.updateUser(user)
        try {
            FirebaseSyncService.saveUserToFirestore(user)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Generate unique ID in sequence/randomly
    suspend fun generateUniqueId(): String {
        val count = appDao.getAllUsers().size + 1
        var idCandidate = "EM${10000 + count}"
        // Make sure it doesn't collide
        while (appDao.getUserByUid(idCandidate) != null) {
            val randomNum = (10000..99999).random()
            idCandidate = "EM$randomNum"
        }
        return idCandidate
    }

    sealed class RegisterResult {
        data class Success(val uid: String) : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }

    // Register a new user
    suspend fun registerUser(
        phoneNumber: String,
        fullName: String,
        referredBy: String?,
        email: String = "",
        password: String = "",
        authMethod: String = "OTP"
    ): RegisterResult {
        if (authMethod == "OTP" || authMethod == "WHATSAPP") {
            if (phoneNumber.length < 10) {
                return RegisterResult.Error("મહેરબાની કરીને સાચો મોબાઈલ નંબર દાખલ કરો / Please enter a valid mobile number")
            }
        } else if (authMethod == "EMAIL") {
            if (!email.contains("@") || email.length < 5) {
                return RegisterResult.Error("મહેરબાની કરીને સાચો ઈમેલ આઈડી દાખલ કરો / Please enter a valid email address")
            }
            if (password.length < 6) {
                return RegisterResult.Error("પાસવર્ડ ઓછામાં ઓછો ૬ અક્ષરનો હોવો જોઈએ / Password must be at least 6 characters")
            }
        }

        if (fullName.isBlank()) {
            return RegisterResult.Error("મહેરબાની કરીને નામ દાખલ કરો / Please enter your full name")
        }

        if (authMethod == "EMAIL" || authMethod == "GOOGLE") {
            val existing = appDao.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                return RegisterResult.Error("આ ઈમેલ આઈડી પહેલાથી જ રજીસ્ટર્ડ છે / This email is already registered")
            }
        }

        var parent: UserAccount? = null
        if (!referredBy.isNullOrBlank()) {
            parent = appDao.getUserByUid(referredBy.trim().uppercase())
            if (parent == null) {
                return RegisterResult.Error("આ રેફરલ આઈડી અસ્તિત્વમાં નથી / This Referral ID does not exist")
            }
            if (!parent.isActive) {
                return RegisterResult.Error("રેફરર એકાઉન્ટ હજી સક્રિય નથી / Referrer account is not active yet")
            }
            if (parent.directReferralsCount >= 3) {
                return RegisterResult.Error("આ આઈડી હેઠળ રેફરલ મર્યાદા (૩) પૂર્ણ થઈ ગઈ છે / Referral limit (3) reached under this ID")
            }
        }

        val newUid = generateUniqueId()
        val newUser = UserAccount(
            uid = newUid,
            phoneNumber = phoneNumber,
            fullName = fullName,
            referredBy = parent?.uid,
            isActive = false, // Must pay joining fee to activate
            walletBalance = 0.0,
            email = email,
            password = password,
            authMethod = authMethod
        )

        appDao.insertUser(newUser)
        try {
            FirebaseSyncService.saveUserToFirestore(newUser)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return RegisterResult.Success(newUid)
    }

    // Check if a UTR has already been used in any transaction description
    suspend fun isUtrDuplicate(utr: String): Boolean {
        if (utr.isBlank()) return false
        val pattern = "%(UTR: ${utr.trim()})%"
        return appDao.findTransactionsByDescription(pattern).isNotEmpty()
    }

    // Activate user and distribute commissions
    suspend fun activateUser(uid: String, utr: String = ""): Boolean {
        val user = appDao.getUserByUid(uid) ?: return false
        if (user.isActive) return true

        // If user has a referrer, we allow activation regardless of the limit to avoid trapping paid registrations
        val parentId = user.referredBy

        // Set user as active
        val activatedUser = user.copy(
            isActive = true,
            paymentStatus = "APPROVED",
            pendingUtr = utr.trim()
        )
        updateUser(activatedUser)

        val txDesc = if (utr.isNotBlank()) {
            "જોડાણ ફી ચુકવણી / Joining Fee Paid (UTR: ${utr.trim()})"
        } else {
            "જોડાણ ફી ચુકવણી / Joining Fee Paid"
        }

        // Add payment transaction
        appDao.insertTransaction(
            Transaction(
                uid = uid,
                type = "DEPOSIT",
                amount = 1000.0,
                description = txDesc
            )
        )

        // Notification for the newly activated user
        appDao.insertNotification(
            AppNotification(
                uid = uid,
                title = "આઈડી સક્રિય થઈ ગયું! / ID Activated!",
                message = "તમારું EarnMitra આઈડી ₹૧,૦૦૦ ડિપોઝિટ સાથે સફળતાપૂર્વક સક્રિય થયું છે. / Your EarnMitra ID has been successfully activated with ₹1,000 deposit.",
                type = "DEPOSIT"
            )
        )

        // Distribute MLM commissions up to 10 levels
        var currentParentId = user.referredBy
        var currentLevel = 1

        while (currentParentId != null && currentLevel <= 10) {
            val parentUser = appDao.getUserByUid(currentParentId) ?: break
            
            // Commission amount: ₹200 for level 1, ₹30 for level 2 to 10
            val commission = if (currentLevel == 1) 200.0 else 30.0
            val isLevel1 = currentLevel == 1
            val newCount = if (isLevel1) parentUser.directReferralsCount + 1 else parentUser.directReferralsCount

            if (parentUser.isActive) {
                val updatedParent = parentUser.copy(
                    walletBalance = parentUser.walletBalance + commission,
                    directReferralsCount = newCount
                )
                appDao.updateUser(updatedParent)

                // Record transaction for parent
                appDao.insertTransaction(
                    Transaction(
                        uid = parentUser.uid,
                        type = "COMMISSION",
                        amount = commission,
                        description = "લેવલ $currentLevel કમિશન: ${user.fullName} ($uid) / Level $currentLevel Commission"
                    )
                )

                // Notification for parent user
                appDao.insertNotification(
                    AppNotification(
                        uid = parentUser.uid,
                        title = "રેફરલ કમિશન મળ્યું! / Commission Received!",
                        message = "તમારા રેફરલ ${user.fullName} એ આઈડી સક્રિય કર્યું. તમને લેવલ $currentLevel નું ₹$commission કમિશન મળ્યું છે! / Referral ${user.fullName} activated ID. You earned ₹$commission commission!",
                        type = "COMMISSION"
                    )
                )
            } else {
                if (isLevel1) {
                    val updatedParent = parentUser.copy(directReferralsCount = newCount)
                    appDao.updateUser(updatedParent)
                }
            }

            currentParentId = parentUser.referredBy
            currentLevel++
        }

        return true
    }

    suspend fun submitJoiningFeeUtr(uid: String, utr: String): Boolean {
        val user = appDao.getUserByUid(uid) ?: return false
        val updatedUser = user.copy(
            paymentStatus = "PENDING_VERIFICATION",
            pendingUtr = utr.trim()
        )
        updateUser(updatedUser)

        // Insert notification
        val notification = AppNotification(
            uid = uid,
            title = "ચુકવણી વિનંતી સબમિટ! / Payment Submitted!",
            message = "તમારી ₹૧,૦૦૦ ની ચુકવણી (UTR: ${utr.trim()}) ની ચકાસણી એડમિન દ્વારા થઈ રહી છે. / Your payment of ₹1,000 (UTR: ${utr.trim()}) is being verified by the admin.",
            type = "DEPOSIT"
        )
        appDao.insertNotification(notification)
        try {
            FirebaseSyncService.saveNotificationToFirestore(notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    suspend fun rejectJoiningFeeUtr(uid: String): Boolean {
        val user = appDao.getUserByUid(uid) ?: return false
        val updatedUser = user.copy(
            paymentStatus = "REJECTED",
            pendingUtr = ""
        )
        updateUser(updatedUser)

        // Insert notification
        val notification = AppNotification(
            uid = uid,
            title = "ચુકવણી અસ્વીકાર! / Payment Rejected!",
            message = "એડમિન દ્વારા તમારી ₹૧,૦૦૦ ની ચુકવણીની પુષ્ટિ થઈ શકી નથી. કૃપા કરીને સાચો UTR દાખલ કરો. / Admin could not verify your payment. Please enter a valid UTR number.",
            type = "SYSTEM"
        )
        appDao.insertNotification(notification)
        try {
            FirebaseSyncService.saveNotificationToFirestore(notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    // Get referral tree level-wise counts and commissions for a user
    suspend fun getMlmLevelStats(uid: String): List<MlmLevelStat> {
        val allUsers = appDao.getAllUsers()
        val stats = mutableListOf<MlmLevelStat>()

        // Initialize levels 1 to 10
        val levelExpectedReferrals = listOf(3, 9, 27, 81, 243, 729, 2187, 6561, 19683, 59049)
        val levelRates = listOf(200.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0, 30.0)

        for (i in 1..10) {
            stats.add(
                MlmLevelStat(
                    level = i,
                    expectedCount = levelExpectedReferrals[i - 1],
                    ratePerReferral = levelRates[i - 1],
                    actualCount = 0,
                    earnedCommission = 0.0
                )
            )
        }

        // BFS to traverse down the referral tree
        var currentLevelUids = listOf(uid)
        var level = 1

        while (currentLevelUids.isNotEmpty() && level <= 10) {
            val nextLevelUids = mutableListOf<String>()
            var activeReferralsCount = 0

            for (pId in currentLevelUids) {
                val children = allUsers.filter { it.referredBy == pId }
                nextLevelUids.addAll(children.map { it.uid })
                
                // Count active children
                activeReferralsCount += children.count { it.isActive }
            }

            val statIndex = level - 1
            stats[statIndex] = stats[statIndex].copy(
                actualCount = activeReferralsCount,
                earnedCommission = activeReferralsCount * stats[statIndex].ratePerReferral
            )

            currentLevelUids = nextLevelUids
            level++
        }

        return stats
    }

    // Process Withdrawal
    suspend fun withdrawFunds(uid: String, amount: Double, type: String): Boolean {
        val user = appDao.getUserByUid(uid) ?: return false
        if (user.walletBalance < amount) return false

        val updatedUser = user.copy(walletBalance = user.walletBalance - amount)
        appDao.updateUser(updatedUser)

        val netAmount = amount * 0.78
        val gstAmount = amount * 0.22

        appDao.insertTransaction(
            Transaction(
                uid = uid,
                type = type, // "WITHDRAW_UPI" or "WITHDRAW_BANK"
                amount = amount,
                description = "ઉપાડ: ₹${String.format("%.2f", netAmount)} + ૨૨% જીએસટી: ₹${String.format("%.2f", gstAmount)} / Net: ₹${String.format("%.2f", netAmount)} + 22% GST: ₹${String.format("%.2f", gstAmount)}"
            )
        )

        // Notification for the withdrawal request
        appDao.insertNotification(
            AppNotification(
                uid = uid,
                title = "ઉપાડ વિનંતી સબમિટ થઈ! / Withdrawal Submitted!",
                message = "₹${String.format("%.2f", amount)} નો ઉપાડ મેળવવાની વિનંતી સફળતાપૂર્વક સબમિટ થઈ છે. ૨૨% જીએસટી કપાયા બાદ ૨૪ કલાકમાં તમારા ખાતામાં જમા થશે. / Withdrawal request of ₹${String.format("%.2f", amount)} is processing. Net amount will reflect in your account in 24 hours.",
                type = "WITHDRAWAL"
            )
        )
        return true
    }

    // Process Product Purchase (Mandatory/Optional)
    suspend fun purchaseProduct(uid: String, productPrice: Double, productName: String): Boolean {
        val user = appDao.getUserByUid(uid) ?: return false
        val gst = productPrice * 0.22
        val totalCost = productPrice + gst
        
        // Product is purchased. We can deduct from wallet or represent payment gateway.
        // Let's deduct from wallet if balance is sufficient, else simulate online payment.
        val fromWallet = user.walletBalance >= totalCost
        if (fromWallet) {
            val updatedUser = user.copy(walletBalance = user.walletBalance - totalCost)
            appDao.updateUser(updatedUser)
            appDao.insertTransaction(
                Transaction(
                    uid = uid,
                    type = "SHOPPING",
                    amount = totalCost,
                    description = "$productName ખરીદી + ૨૨% જીએસટી (₹${String.format("%.2f", gst)}) / Purchase + 22% GST"
                )
            )
        } else {
            appDao.insertTransaction(
                Transaction(
                    uid = uid,
                    type = "SHOPPING",
                    amount = totalCost,
                    description = "$productName ઓનલાઇન ખરીદી + ૨૨% જીએસટી (₹${String.format("%.2f", gst)}) / Online Purchase + 22% GST"
                )
            )
        }
        return true
    }

    // Simulate direct local payment (PhonePe-like direct pay) using commission
    suspend fun payLocally(uid: String, amount: Double, merchantName: String): Boolean {
        val user = appDao.getUserByUid(uid) ?: return false
        val gst = amount * 0.22
        val totalCost = amount + gst
        if (user.walletBalance < totalCost) return false

        val updatedUser = user.copy(walletBalance = user.walletBalance - totalCost)
        appDao.updateUser(updatedUser)

        appDao.insertTransaction(
            Transaction(
                uid = uid,
                type = "LOCAL_PAY",
                amount = totalCost,
                description = "$merchantName ને ચુકવણી + ૨૨% જીએસટી (₹${String.format("%.2f", gst)}) / Merchant Pay + 22% GST"
            )
        )
        return true
    }

    // --- DIRECT REFERRALS & NOTIFICATIONS ---
    suspend fun getPendingPaymentUsers(): List<UserAccount> {
        return appDao.getPendingPaymentUsers()
    }

    suspend fun getReferralsForUser(uid: String): List<UserAccount> {
        return appDao.getAllUsers().filter { it.referredBy == uid }
    }

    fun getNotificationsByUidFlow(uid: String): Flow<List<AppNotification>> {
        return appDao.getNotificationsByUidFlow(uid)
    }

    suspend fun insertNotification(notification: AppNotification) {
        appDao.insertNotification(notification)
        try {
            FirebaseSyncService.saveNotificationToFirestore(notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllNotificationsAsRead(uid: String) {
        appDao.markAllNotificationsAsRead(uid)
    }

    suspend fun clearAllNotifications(uid: String) {
        appDao.clearAllNotifications(uid)
    }

    fun getOrdersByUidFlow(uid: String): Flow<List<Order>> {
        return appDao.getOrdersByUidFlow(uid)
    }

    fun getAllOrdersFlow(): Flow<List<Order>> {
        return appDao.getAllOrdersFlow()
    }

    suspend fun insertOrder(order: Order) {
        appDao.insertOrder(order)
        try {
            FirebaseSyncService.saveOrderToFirestore(order)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateOrder(order: Order) {
        appDao.updateOrder(order)
        try {
            FirebaseSyncService.saveOrderToFirestore(order)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class MlmLevelStat(
    val level: Int,
    val expectedCount: Int,
    val ratePerReferral: Double,
    val actualCount: Int,
    val earnedCommission: Double
)
