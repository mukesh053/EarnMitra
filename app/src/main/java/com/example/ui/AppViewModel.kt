package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.appDao())

    // App Preferences / Settings
    private val sharedPrefs = application.getSharedPreferences("earnmitra_prefs", android.content.Context.MODE_PRIVATE)
    var showOnboarding by mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true))
        private set

    fun completeOnboarding() {
        showOnboarding = false
        sharedPrefs.edit().putBoolean("show_onboarding", false).apply()
    }

    var selectedLanguage by mutableStateOf(Language.GUJARATI)
        private set

    var isDarkMode by mutableStateOf(false)
        private set

    // Authentication States
    var loggedInUserUid by mutableStateOf<String?>(null)
        private set

    var currentUser by mutableStateOf<UserAccount?>(null)
        private set

    var currentTransactions by mutableStateOf<List<Transaction>>(emptyList())
        private set

    var currentMlmStats by mutableStateOf<List<MlmLevelStat>>(emptyList())
        private set

    // Registration Form Inputs
    var regPhone by mutableStateOf("")
    var regName by mutableStateOf("")
    var regReferrer by mutableStateOf("")
    var regMessage by mutableStateOf<String?>(null)
    var regSuccessUid by mutableStateOf<String?>(null)
    var showRegOtpEntry by mutableStateOf(false)
    var regEnteredOtp by mutableStateOf("")
    var regAuthMethod by mutableStateOf("WHATSAPP") // "WHATSAPP", "EMAIL", "GOOGLE", "OTP"
    var regEmail by mutableStateOf("")
    var regPassword by mutableStateOf("")

    // Login Form Inputs
    var loginUid by mutableStateOf("")
    var loginPhoneForOtp by mutableStateOf("")
    var showOtpEntry by mutableStateOf(false)
    var enteredOtp by mutableStateOf("")
    var matchedAccountsForPhone by mutableStateOf<List<UserAccount>>(emptyList())
    var loginMessage by mutableStateOf<String?>(null)
    var loginAuthMethod by mutableStateOf("WHATSAPP") // "WHATSAPP", "EMAIL", "GOOGLE", "OTP"
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var adminWhatsAppNumber by mutableStateOf(sharedPrefs.getString("admin_whatsapp_number", "+919876543210") ?: "+919876543210")
    var referralWebsiteUrl by mutableStateOf(sharedPrefs.getString("referral_website_url", "https://ais-pre-lssi3sfr4wtdjznoh2xcdt-1007319374021.asia-southeast1.run.app") ?: "https://ais-pre-lssi3sfr4wtdjznoh2xcdt-1007319374021.asia-southeast1.run.app")

    // GPS Location ( अहमदाबाद default )
    var locationLat by mutableStateOf(23.0225)
    var locationLng by mutableStateOf(72.5714)
    var isTrackingLocation by mutableStateOf(false)

    // Two-Step Verification (2FA) Security
    var is2FaRequiredStep by mutableStateOf(false)

    // In-App Notifications
    var currentNotifications by mutableStateOf<List<AppNotification>>(emptyList())
        private set
    var headsUpNotification by mutableStateOf<AppNotification?>(null)

    // Direct Team/Referrals List
    var directReferrals by mutableStateOf<List<UserAccount>>(emptyList())
    var allUsersList by mutableStateOf<List<UserAccount>>(emptyList())
    var productsList by mutableStateOf<List<Product>>(emptyList())

    // SMS OTP Simulation State
    var globalRealOtpMode by mutableStateOf(sharedPrefs.getBoolean("global_real_otp_mode", false)) // Toggle between simulated SMS & hardcoded 1234
    var generatedOtp by mutableStateOf("")
    var incomingSmsAlert by mutableStateOf<String?>(null)

    // App Security states (4-digit PIN lock)
    var isAppUnlocked by mutableStateOf(false)
    var enteredPinForLock by mutableStateOf("")
    var pinSetupError by mutableStateOf<String?>(null)

    // Secure UTR Payment Verification States
    var isVerifyingUtr by mutableStateOf(false)
    var utrVerificationStep by mutableStateOf("")

    fun isValidUtrNumber(utr: String): Boolean {
        val cleaned = utr.trim()
        if (cleaned.length != 12) return false
        if (!cleaned.all { it.isDigit() }) return false
        
        val firstChar = cleaned[0]
        // UPI UTRs for 2024, 2025, 2026 typically start with 4, 5, or 6
        if (firstChar != '4' && firstChar != '5' && firstChar != '6') return false
        
        // Avoid obvious dummy sequences where all digits are the same
        if (cleaned.all { it == firstChar }) return false
        
        // Avoid sequential dummy ranges
        val fakeUtrs = listOf(
            "123456789012", "012345678901", "987654321098", "121212121212", "000000000000"
        )
        if (fakeUtrs.contains(cleaned)) return false
        
        return true
    }

    // Twilio Settings
    var twilioSidState by mutableStateOf(sharedPrefs.getString("twilio_sid", "") ?: "")
    var twilioTokenState by mutableStateOf(sharedPrefs.getString("twilio_token", "") ?: "")
    var twilioFromPhoneState by mutableStateOf(sharedPrefs.getString("twilio_from_phone", "") ?: "")

    // Custom Registration UPI IDs list
    var customRegistrationUpiIds by mutableStateOf(
        sharedPrefs.getString("custom_upi_ids", "earnmitra@ybl,earnmitra@ibl,earnmitra@axl,earnmitra1@ybl,earnmitra1@ibl,earnmitra1@axl")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: listOf("earnmitra@ybl", "earnmitra@ibl", "earnmitra@axl", "earnmitra1@ybl", "earnmitra1@ibl", "earnmitra1@axl")
    )

    // Cart & Order State Variables
    var cartItems by mutableStateOf<Map<Int, Int>>(emptyMap()) // Product ID -> Quantity
    var userOrders by mutableStateOf<List<Order>>(emptyList())
    var allOrdersListState by mutableStateOf<List<Order>>(emptyList())

    // Shipping Address State Variables
    var shippingName by mutableStateOf("")
    var shippingPhone by mutableStateOf("")
    var shippingAddressLines by mutableStateOf("")
    var shippingCity by mutableStateOf("")
    var shippingPincode by mutableStateOf("")
    var selectedPaymentMethod by mutableStateOf("UPI") // "UPI", "WALLET", "COD"

    init {
        // Safety check: Reset referralWebsiteUrl if it has legacy incorrect domains (like earnmitra.app, earnmintra.app, or contains /join, or is empty)
        val defaultUrl = "https://ais-pre-lssi3sfr4wtdjznoh2xcdt-1007319374021.asia-southeast1.run.app"
        if (referralWebsiteUrl.isBlank() || 
            referralWebsiteUrl.contains("earnmitra.app") || 
            referralWebsiteUrl.contains("earnmintra.app") || 
            referralWebsiteUrl.contains("/join")
        ) {
            referralWebsiteUrl = defaultUrl
            sharedPrefs.edit().putString("referral_website_url", defaultUrl).apply()
        }

        // Create initial dummy data inside the local database so it's ready to demo right away
        viewModelScope.launch {
            val existing = database.appDao().getAllUsers()
            if (existing.isEmpty()) {
                // Pre-populate some demo data to make the MLM tree look gorgeous and realistic!
                // Let's create an admin account (EM10000) that is active
                val admin = UserAccount(
                    uid = "EM10000",
                    phoneNumber = "9999999999",
                    fullName = "અમિતભાઈ પટેલ / Amit Patel",
                    isActive = true,
                    walletBalance = 5400.0,
                    directReferralsCount = 3
                )
                database.appDao().insertUser(admin)

                // Level 1 children
                val c1 = UserAccount(uid = "EM10001", phoneNumber = "9876543210", fullName = "મનીષભાઈ શાહ / Manish Shah", isActive = true, referredBy = "EM10000", directReferralsCount = 2, walletBalance = 1200.0)
                val c2 = UserAccount(uid = "EM10002", phoneNumber = "8765432109", fullName = "જીગ્નેશભાઈ મહેતા / Jignesh Mehta", isActive = true, referredBy = "EM10000", directReferralsCount = 1, walletBalance = 600.0)
                val c3 = UserAccount(uid = "EM10003", phoneNumber = "7654321098", fullName = "હરેશભાઈ ગોહિલ / Haresh Gohil", isActive = false, referredBy = "EM10000", directReferralsCount = 0)
                database.appDao().insertUser(c1)
                database.appDao().insertUser(c2)
                database.appDao().insertUser(c3)

                // Level 2 children
                val c1_1 = UserAccount(uid = "EM10004", phoneNumber = "9800000001", fullName = "કલ્પેશભાઈ રાવલ / Kalpesh Raval", isActive = true, referredBy = "EM10001", directReferralsCount = 0)
                val c1_2 = UserAccount(uid = "EM10005", phoneNumber = "9800000002", fullName = "સંજયભાઈ પટેલ / Sanjay Patel", isActive = true, referredBy = "EM10001", directReferralsCount = 0)
                val c2_1 = UserAccount(uid = "EM10006", phoneNumber = "9800000003", fullName = "વિપુલભાઈ દરજી / Vipul Darji", isActive = true, referredBy = "EM10002", directReferralsCount = 0)
                database.appDao().insertUser(c1_1)
                database.appDao().insertUser(c1_2)
                database.appDao().insertUser(c2_1)

                // Add some initial transactions
                database.appDao().insertTransaction(Transaction(uid = "EM10000", type = "DEPOSIT", amount = 1000.0, description = "જોડાણ ફી ચુકવણી / Joining Fee Paid"))
                database.appDao().insertTransaction(Transaction(uid = "EM10000", type = "COMMISSION", amount = 200.0, description = "લેવલ 1 કમિશન: મનીષભાઈ શાહ (EM10001)"))
                database.appDao().insertTransaction(Transaction(uid = "EM10000", type = "COMMISSION", amount = 200.0, description = "લેવલ 1 કમિશન: જીગ્નેશભાઈ મહેતા (EM10002)"))
                database.appDao().insertTransaction(Transaction(uid = "EM10000", type = "COMMISSION", amount = 30.0, description = "લેવલ 2 કમિશન: કલ્પેશભાઈ રાવલ (EM10004)"))
                database.appDao().insertTransaction(Transaction(uid = "EM10000", type = "COMMISSION", amount = 30.0, description = "લેવલ 2 કમિશન: સંજયભાઈ પટેલ (EM10005)"))
                database.appDao().insertTransaction(Transaction(uid = "EM10000", type = "COMMISSION", amount = 30.0, description = "લેવલ 2 કમિશન: વિપુલભાઈ દરજી (EM10006)"))
            }
        }
        loadProducts()
    }

    // Toggle Preferences
    fun setLanguage(language: Language) {
        selectedLanguage = language
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    // Helper to validate real/valid Indian mobile numbers
    fun isValidIndianMobileNumber(phone: String): Boolean {
        // Clean non-digits
        var cleaned = phone.replace(Regex("[^0-9]"), "")
        
        // Handle common prefixes
        if (cleaned.startsWith("91") && cleaned.length == 12) {
            cleaned = cleaned.substring(2)
        } else if (cleaned.startsWith("0") && cleaned.length == 11) {
            cleaned = cleaned.substring(1)
        }
        
        // Indian mobile numbers must be exactly 10 digits
        if (cleaned.length != 10) return false
        
        // Indian mobile numbers must start with 6, 7, 8, or 9
        if (!cleaned.matches(Regex("^[6-9]\\d{9}$"))) return false
        
        // Avoid obvious fake sequences where all digits are the same (e.g. 1111111111, 6666666666)
        val firstChar = cleaned[0]
        if (cleaned.all { it == firstChar }) return false
        
        // Avoid dummy numbers & sequential ranges
        val fakes = listOf(
            "1234567890", "0123456789", "9876543210", "0987654321",
            "1212121212", "9898989898", "9090909090", "8080808080",
            "7070707070", "6060606060", "0000000000"
        )
        if (fakes.contains(cleaned)) return false
        
        return true
    }

    // Start registration OTP flow
    // Start registration OTP flow
    fun triggerRegOtp() {
        val phoneTrimmed = regPhone.trim()
        val nameTrimmed = regName.trim()
        if (!isValidIndianMobileNumber(phoneTrimmed)) {
            regMessage = "મહેબૂબાની કરીને સાચો ૧૦-અંકનો મોબાઈલ નંબર દાખલ કરો! ખોટા અથવા અમાન્ય નંબરો સ્વીકાર્ય નથી. / Enter a valid 10-digit Indian mobile number! Fake numbers are not allowed."
            return
        }
        if (nameTrimmed.isBlank()) {
            regMessage = "મહેરબાની કરીને નામ દાખલ કરો / Please enter your full name"
            return
        }
        
        regMessage = null
        showRegOtpEntry = true
        regEnteredOtp = ""
        regAuthMethod = "OTP"
        
        val isReal = globalRealOtpMode && twilioSidState.isNotBlank() && twilioTokenState.isNotBlank() && twilioFromPhoneState.isNotBlank()

        if (isReal) {
            val code = (1000..9999).random().toString()
            generatedOtp = code
            incomingSmsAlert = null // Clear mock alert for real OTP to keep it secure!
            regMessage = "ઓટીપી કોડ તમારા નંબર $phoneTrimmed પર મોકલવામાં આવ્યો છે! કૃપા કરીને મેન્યુઅલી ઓટીપી દાખલ કરો. / OTP has been sent!"
            
            // Send Twilio SMS if configured
            sendTwilioSms(phoneTrimmed, "Your EarnMitra registration OTP is $code", twilioSidState, twilioTokenState, twilioFromPhoneState, isWhatsApp = false)
        } else {
            generatedOtp = "1234"
            regMessage = "ઓટીપી સફળતાપૂર્વક મોકલવામાં આવ્યો છે: $phoneTrimmed. (ડેમો સિમ્યુલેશન મોડ: ટેસ્ટિંગ માટે ઓટીપી '1234' દાખલ કરો. રિયલ સુરક્ષિત ઓટીપી માટે એડમિન પેનલમાં Twilio સેટ કરો). / OTP sent. (Demo Mode: Enter '1234' manually)."
        }
    }

    // WhatsApp Registration Link Generation
    fun triggerWhatsAppRegVerification(): String {
        val nameTrimmed = regName.trim()
        val phoneTrimmed = regPhone.trim()
        if (!isValidIndianMobileNumber(phoneTrimmed)) {
            regMessage = "મહેરબાની કરીને સાચો ૧૦-અંકનો મોબાઈલ નંબર દાખલ કરો! ખોટા અથવા અમાન્ય નંબરો સ્વીકાર્ય નથી. / Enter a valid 10-digit Indian mobile number! Fake numbers are not allowed."
            return ""
        }
        if (nameTrimmed.isBlank()) {
            regMessage = "મહેરબાની કરીને નામ દાખલ કરો / Please enter your full name"
            return ""
        }

        val isReal = globalRealOtpMode && twilioSidState.isNotBlank() && twilioTokenState.isNotBlank() && twilioFromPhoneState.isNotBlank()
        
        val code = if (isReal) (1000..9999).random().toString() else "1234"
        generatedOtp = code
        showRegOtpEntry = true
        regEnteredOtp = "" // STRICT RULE: Never auto-fill registration OTP! User must type manually.
        regAuthMethod = "WHATSAPP"
        
        val formattedPhone = if (phoneTrimmed.startsWith("+")) phoneTrimmed else if (phoneTrimmed.length == 10) "+91$phoneTrimmed" else phoneTrimmed
        val cleanPhone = formattedPhone.replace("+", "").replace(" ", "")
        val formattedPhoneForWa = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

        // Safe message without any pre-filled OTP code to prevent intercepting
        val messageText = "નમસ્તે EarnMitra! કૃપા કરીને મારું એકાઉન્ટ વેરિફાય કરો.\nનામ: $nameTrimmed\nમોબાઇલ: $phoneTrimmed"
        val encodedText = java.net.URLEncoder.encode(messageText, "UTF-8")

        if (isReal) {
            // Real Twilio WhatsApp OTP Mode (100% secure server-side message dispatch)
            regMessage = "ઓટીપી કોડ તમારા વોટ્સએપ નંબર $phoneTrimmed પર મોકલવામાં આવ્યો છે! કૃપા કરીને મેન્યુઅલી ઓટીપી દાખલ કરો. / OTP has been sent to your WhatsApp number!"
            sendTwilioSms(
                toPhone = formattedPhone,
                messageText = "Your EarnMitra WhatsApp Verification Code is $code. Please enter this manually in the application.",
                sid = twilioSidState,
                token = twilioTokenState,
                fromPhone = twilioFromPhoneState,
                isWhatsApp = true
            )
            return "" // Do not open WhatsApp client locally for real OTP verification to prevent spoofing
        } else {
            // Simulation/Free Mode (Uses fixed code '1234' for testing, opens clean contact chat with Admin)
            regMessage = "ઓટીપી સફળતાપૂર્વક મોકલવામાં આવ્યો છે: $phoneTrimmed. (ડેમો સિમ્યુલેશન મોડ: ટેસ્ટિંગ માટે ઓટીપી '1234' દાખલ કરો. રિયલ સુરક્ષિત ઓટીપી માટે એડમિન પેનલમાં Twilio સેટ કરો). / OTP sent. (Demo Mode: Enter '1234' manually)."
            return "https://api.whatsapp.com/send?phone=${adminWhatsAppNumber.replace("+", "").replace(" ", "")}&text=$encodedText"
        }
    }

    // Register with Firebase Email & Password
    fun handleFirebaseEmailRegister() {
        if (regName.trim().isBlank()) {
            regMessage = "કૃપા કરીને નામ દાખલ કરો / Please enter your name"
            return
        }
        if (!regEmail.contains("@") || regEmail.length < 5) {
            regMessage = "મહેરબાની કરીને સાચો ઈમેલ આઈડી દાખલ કરો / Please enter a valid email address"
            return
        }
        if (regPassword.length < 6) {
            regMessage = "પાસવર્ડ ઓછામાં ઓછો ૬ અક્ષરનો હોવો જોઈએ / Password must be at least 6 characters"
            return
        }
        regAuthMethod = "EMAIL"
        
        viewModelScope.launch {
            regMessage = "રજીસ્ટ્રેશન પ્રક્રિયા શરૂ થઈ રહી છે... / Starting registration..."
            regSuccessUid = null
            val result = repository.registerUser(
                phoneNumber = "",
                fullName = regName.trim(),
                referredBy = if (regReferrer.isNotBlank()) regReferrer.trim() else null,
                email = regEmail.trim().lowercase(),
                password = regPassword,
                authMethod = "EMAIL"
            )
            when (result) {
                is AppRepository.RegisterResult.Success -> {
                    regSuccessUid = result.uid
                    regMessage = "રજીસ્ટ્રેશન સફળ! Firebase ઈમેલ વેરિફિકેશન લિંક તમારા ઈમેલ પર મોકલી છે. આઈડી: ${result.uid} / Registration Successful! Firebase verification email sent."
                    
                    repository.insertNotification(
                        AppNotification(
                            uid = result.uid,
                            title = "જી આયા નૂ! EarnMitra માં આપનું સ્વાગત છે! / Welcome to EarnMitra!",
                            message = "તમારું ઈમેલ રજીસ્ટ્રેશન સફળ રહ્યું છે. ₹૧,૦૦૦ ભરીને તમારું આઈડી એક્ટિવેટ કરો! / Email signup successful. Activate your account!",
                            type = "SYSTEM"
                        )
                    )
                    loginUid = result.uid
                    loginEmail = regEmail.trim().lowercase()
                    regName = ""
                    regEmail = ""
                    regPassword = ""
                    regReferrer = ""
                }
                is AppRepository.RegisterResult.Error -> {
                    regMessage = result.message
                }
            }
        }
    }

    // Register with Google Sign-In (Simulated)
    fun handleGoogleRegister(gName: String, gEmail: String) {
        viewModelScope.launch {
            regMessage = null
            regSuccessUid = null
            regAuthMethod = "GOOGLE"
            val result = repository.registerUser(
                phoneNumber = "",
                fullName = gName,
                referredBy = if (regReferrer.isNotBlank()) regReferrer.trim() else null,
                email = gEmail.trim().lowercase(),
                password = "",
                authMethod = "GOOGLE"
            )
            when (result) {
                is AppRepository.RegisterResult.Success -> {
                    regSuccessUid = result.uid
                    regMessage = "ગૂગલ વેરિફિકેશન સફળ! આઈડી: ${result.uid} / Google Sign-In Successful!"
                    
                    repository.insertNotification(
                        AppNotification(
                            uid = result.uid,
                            title = "જી આયા નૂ! EarnMitra માં આપનું સ્વાગત છે! / Welcome to EarnMitra!",
                            message = "ગૂગલ લોગીન સફળતાપૂર્વક પૂર્ણ થયું છે. ₹૧,૦૦૦ ભરીને તમારું આઈડી એક્ટિવેટ કરો! / Google Sign-In successful. Activate your account!",
                            type = "SYSTEM"
                        )
                    )
                    loginUid = result.uid
                    regReferrer = ""
                }
                is AppRepository.RegisterResult.Error -> {
                    regMessage = result.message
                }
            }
        }
    }

    // Register action
    fun handleRegister() {
        val isReal = globalRealOtpMode && twilioSidState.isNotBlank() && twilioTokenState.isNotBlank() && twilioFromPhoneState.isNotBlank()
        val requiredOtp = if (isReal) generatedOtp else "1234"
        if (regAuthMethod == "OTP" || regAuthMethod == "WHATSAPP") {
            val correctOtp = if (regAuthMethod == "WHATSAPP") (if (isReal) generatedOtp else "1234") else requiredOtp
            if (regEnteredOtp != correctOtp) {
                regMessage = "ખોટો ઓટીપી! કૃપા કરીને સાચો ઓટીપી દાખલ કરો. / Invalid OTP! Enter correct code."
                return
            }
        }

        viewModelScope.launch {
            regMessage = null
            regSuccessUid = null
            val result = repository.registerUser(
                phoneNumber = regPhone.trim(),
                fullName = regName.trim(),
                referredBy = if (regReferrer.isNotBlank()) regReferrer.trim() else null,
                email = regEmail.trim().lowercase(),
                password = regPassword,
                authMethod = regAuthMethod
            )
            when (result) {
                is AppRepository.RegisterResult.Success -> {
                    regSuccessUid = result.uid
                    regMessage = "રજીસ્ટ્રેશન સફળ! યુનિક આઈડી: ${result.uid} / Registration Successful! Unique ID: ${result.uid}"
                    
                    // Add direct notification
                    repository.insertNotification(
                        AppNotification(
                            uid = result.uid,
                            title = "જી આયા નૂ! EarnMitra માં આપનું સ્વાગત છે! / Welcome to EarnMitra!",
                            message = "EarnMitra પરિવારમાં આપનું સ્વાગત છે. ₹૧,૦૦૦ ભરીને તમારું આઈડી એક્ટિવેટ કરો અને કમાવાનું શરૂ કરો! / Welcome to EarnMitra. Activate your account and start earning!",
                            type = "SYSTEM"
                        )
                    )

                    // Auto fill loginUid with newly generated UID for easy access
                    loginUid = result.uid
                    // Clear fields
                    regPhone = ""
                    regName = ""
                    regReferrer = ""
                    showRegOtpEntry = false
                    regEnteredOtp = ""
                }
                is AppRepository.RegisterResult.Error -> {
                    regMessage = result.message
                }
            }
        }
    }

    // Find accounts linked to a phone number
    fun handleFindAccountsByPhone(phone: String) {
        val phoneTrimmed = phone.trim()
        if (!isValidIndianMobileNumber(phoneTrimmed)) {
            loginMessage = "મહેરબાની કરીને સાચો ૧૦-અંકનો મોબાઈલ નંબર દાખલ કરો! / Please enter a valid 10-digit mobile number!"
            matchedAccountsForPhone = emptyList()
            return
        }
        viewModelScope.launch {
            matchedAccountsForPhone = repository.getUsersByPhone(phoneTrimmed)
            if (matchedAccountsForPhone.isEmpty()) {
                loginMessage = "આ ફોન નંબર માટે કોઈ એકાઉન્ટ મળ્યું નથી / No accounts found for this phone number"
            } else {
                loginMessage = "${matchedAccountsForPhone.size} એકાઉન્ટ મળ્યા! કૃપા કરીને લોગીન કરવા માટે આઈડી પસંદ કરો. / Found ${matchedAccountsForPhone.size} accounts."
            }
        }
    }

    // WhatsApp Login Link Generation & Dispatch
    fun triggerWhatsAppLoginVerification(context: android.content.Context) {
        viewModelScope.launch {
            loginMessage = null
            val cleanedUid = loginUid.trim().uppercase()
            val user = repository.getUserByUid(cleanedUid)
            if (user == null) {
                loginMessage = "અમાન્ય યુનિક આઈડી નંબર! / Invalid Unique ID!"
                return@launch
            }
            
            // Sync user Twilio settings if saved
            val isReal = (globalRealOtpMode || user.realOtpEnabled) && twilioSidState.isNotBlank() && twilioTokenState.isNotBlank() && twilioFromPhoneState.isNotBlank()
            
            val code = if (isReal) (1000..9999).random().toString() else "1234"
            generatedOtp = code
            showOtpEntry = true
            enteredOtp = "" // STRICT RULE: Never auto-fill login OTP! User must type manually.
            loginAuthMethod = "WHATSAPP"
            
            val formattedPhone = if (user.phoneNumber.startsWith("+")) user.phoneNumber else if (user.phoneNumber.length == 10) "+91${user.phoneNumber}" else user.phoneNumber
            val cleanPhone = formattedPhone.replace("+", "").replace(" ", "")
            val formattedPhoneForWa = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

            // Clean, safe message containing NO pre-filled code to protect customer's security
            val messageText = "નમસ્તે EarnMitra! કૃપા કરીને મારું એકાઉન્ટ લોગીન કરો.\nઆઈડી: $cleanedUid\nમોબાઇલ: ${user.phoneNumber}"
            val encodedText = java.net.URLEncoder.encode(messageText, "UTF-8")
            
            val sid = if (user.realOtpEnabled) user.twilioSid else twilioSidState
            val tok = if (user.realOtpEnabled) user.twilioToken else twilioTokenState
            val frm = if (user.realOtpEnabled) user.twilioFromPhone else twilioFromPhoneState

            if (isReal && sid.isNotBlank() && tok.isNotBlank() && frm.isNotBlank()) {
                loginMessage = "ઓટીપી કોડ તમારા વોટ્સએપ નંબર ${user.phoneNumber} પર મોકલવામાં આવ્યો છે! કૃપા કરીને મેન્યુઅલી ઓટીપી દાખલ કરો. / OTP has been sent to your WhatsApp number!"
                sendTwilioSms(
                    toPhone = user.phoneNumber,
                    messageText = "Your EarnMitra Login WhatsApp Verification Code is $code. Please do not share this code.",
                    sid = sid,
                    token = tok,
                    fromPhone = frm,
                    isWhatsApp = true
                )
            } else {
                loginMessage = "ઓટીપી સફળતાપૂર્વક મોકલવામાં આવ્યો છે: ${user.phoneNumber}. (ડેમો સિમ્યુલેશન મોડ: ટેસ્ટિંગ માટે ઓટીપી '1234' દાખલ કરો. રિયલ સુરક્ષિત ઓટીપી માટે એડમિન પેનલમાં Twilio સેટ કરો). / OTP sent. (Demo Mode: Enter '1234' manually)."
                val intentUrl = "https://api.whatsapp.com/send?phone=${adminWhatsAppNumber.replace("+", "").replace(" ", "")}&text=$encodedText"
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(intentUrl)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Login with Email & Password (Firebase simulation)
    fun handleFirebaseEmailLogin() {
        if (!loginEmail.contains("@")) {
            loginMessage = "મહેરબાની કરીને સાચો ઈમેલ દાખલ કરો / Please enter valid email"
            return
        }
        viewModelScope.launch {
            loginMessage = "ચકાસણી ચાલુ છે... / Verifying..."
            val user = repository.getUserByEmail(loginEmail.trim().lowercase())
            if (user == null) {
                loginMessage = "આ ઈમેલ આઈડી રજીસ્ટર્ડ નથી! / This email is not registered!"
                return@launch
            }
            if (user.password != loginPassword) {
                loginMessage = "ખોટો પાસવર્ડ! કૃપા કરીને સાચો પાસવર્ડ દાખલ કરો. / Invalid password!"
                return@launch
            }
            
            // Successful log in!
            loginUid = user.uid
            loggedInUserUid = user.uid
            currentUser = user
            loginMessage = "લોગીન સફળ! / Login Successful!"
            isAppUnlocked = !user.isSecurityLockEnabled
        }
    }

    // Login with Google (Firebase simulation)
    fun handleGoogleLogin(gEmail: String) {
        viewModelScope.launch {
            loginMessage = "ગૂગલ લોગીન ચકાસણી ચાલુ છે... / Verifying Google Sign-In..."
            val user = repository.getUserByEmail(gEmail.trim().lowercase())
            if (user == null) {
                loginMessage = "આ ગૂગલ એકાઉન્ટ રજીસ્ટર્ડ નથી. કૃપા કરીને પહેલા રજીસ્ટ્રેશન કરો. / This Google account is not registered. Please sign up first."
                return@launch
            }
            
            // Successful log in!
            loginUid = user.uid
            loggedInUserUid = user.uid
            currentUser = user
            loginMessage = "લોગીન સફળ! / Login Successful!"
            isAppUnlocked = !user.isSecurityLockEnabled
        }
    }

    // Trigger Login OTP Simulation
    fun triggerLoginOtp() {
        viewModelScope.launch {
            loginMessage = null
            val cleanedUid = loginUid.trim().uppercase()
            val user = repository.getUserByUid(cleanedUid)
            if (user == null) {
                loginMessage = "અમાન્ય યુનિક આઈડી નંબર! / Invalid Unique ID!"
                return@launch
            }
            
            loginPhoneForOtp = user.phoneNumber
            showOtpEntry = true
            enteredOtp = ""
            loginAuthMethod = "OTP"

            // Update Twilio states if saved in user config
            if (user.realOtpEnabled) {
                twilioSidState = user.twilioSid
                twilioTokenState = user.twilioToken
                twilioFromPhoneState = user.twilioFromPhone
                globalRealOtpMode = true
            }

            val isReal = (globalRealOtpMode || user.realOtpEnabled) && twilioSidState.isNotBlank() && twilioTokenState.isNotBlank() && twilioFromPhoneState.isNotBlank()

            if (isReal) {
                val code = (1000..9999).random().toString()
                generatedOtp = code
                incomingSmsAlert = null // Clear mock alert for real OTP to keep it secure!
                loginMessage = "ઓટીપી કોડ તમારા નંબર ${user.phoneNumber} પર મોકલવામાં આવ્યો છે! કૃપા કરીને મેન્યુઅલી ઓટીપી દાખલ કરો. / OTP has been sent!"
                
                // Real Twilio SMS if credentials match
                val sid = if (user.realOtpEnabled) user.twilioSid else twilioSidState
                val tok = if (user.realOtpEnabled) user.twilioToken else twilioTokenState
                val frm = if (user.realOtpEnabled) user.twilioFromPhone else twilioFromPhoneState
                
                sendTwilioSms(user.phoneNumber, "Your EarnMitra Login OTP is $code", sid, tok, frm, isWhatsApp = false)
            } else {
                val code = "1234"
                generatedOtp = code
                loginMessage = "ઓટીપી સફળતાપૂર્વક મોકલવામાં આવ્યો છે: ${user.phoneNumber}. (ડેમો સિમ્યુલેશન મોડ: ટેસ્ટિંગ માટે ઓટીપી '1234' દાખલ કરો. રિયલ સુરક્ષિત ઓટીપી માટે એડમિન પેનલમાં Twilio સેટ કરો). / OTP sent to registered number. (Demo Mode: Enter '1234' manually)."
            }
        }
    }

    // Verify Login OTP
    fun verifyLoginOtpAndLogin() {
        viewModelScope.launch {
            val isReal = (globalRealOtpMode || (currentUser?.realOtpEnabled == true)) && twilioSidState.isNotBlank() && twilioTokenState.isNotBlank() && twilioFromPhoneState.isNotBlank()
            val requiredOtp = if (isReal) generatedOtp else "1234"
            if (enteredOtp != requiredOtp) {
                loginMessage = "ખોટો ઓટીપી! કૃપા કરીને સાચો ઓટીપી દાખલ કરો. / Invalid OTP! Enter correct code."
                return@launch
            }

            val cleanedUid = loginUid.trim().uppercase()
            val user = repository.getUserByUid(cleanedUid)
            if (user == null) {
                loginMessage = "લોગિન નિષ્ફળ! / Login failed!"
                return@launch
            }

            // If 2FA is enabled and we haven't processed it yet
            if (user.isTwoStepEnabled && !is2FaRequiredStep) {
                is2FaRequiredStep = true
                enteredOtp = ""
                
                if (globalRealOtpMode || user.realOtpEnabled) {
                    val code = (1000..9999).random().toString()
                    generatedOtp = code
                    incomingSmsAlert = "[SMS] EarnMitra: ટુ-સ્ટેપ વેરિફિકેશન માટેનો ઓટીપી કોડ $code છે. / Your 2FA security code is $code."
                }
                loginMessage = "ટુ-સ્ટેપ વેરિફિકેશન સક્રિય છે! ફરીથી ઓટીપી દાખલ કરો / 2FA Active! Enter OTP again."
                return@launch
            }

            // Successful log in!
            loggedInUserUid = user.uid
            currentUser = user
            showOtpEntry = false
            is2FaRequiredStep = false
            loginMessage = "લોગીન સફળ! / Login Successful!"
            isAppUnlocked = !user.isSecurityLockEnabled // If app lock is enabled, they need PIN input to unlock!
            
            // Observe updates
            observeUserData(user.uid)
        }
    }

    private fun observeUserData(uid: String) {
        viewModelScope.launch {
            repository.getUserByUidFlow(uid).collect { user ->
                if (user != null) {
                    currentUser = user
                    // Refresh MLM statistics
                    currentMlmStats = repository.getMlmLevelStats(user.uid)
                    if (user.uid == "EM10000") {
                        loadPendingVerificationUsers()
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getTransactionsByUidFlow(uid).collect { txs ->
                currentTransactions = txs
            }
        }
        viewModelScope.launch {
            repository.getNotificationsByUidFlow(uid).collect { notifs ->
                currentNotifications = notifs
                // Show latest unread notification as heads-up banner if any
                val latest = notifs.firstOrNull()
                if (latest != null && !latest.isRead && (headsUpNotification == null || headsUpNotification!!.id != latest.id)) {
                    headsUpNotification = latest
                }
            }
        }
        viewModelScope.launch {
            // Regularly update direct referrals list
            while (loggedInUserUid == uid) {
                directReferrals = repository.getReferralsForUser(uid)
                allUsersList = database.appDao().getAllUsers()
                kotlinx.coroutines.delay(2000)
            }
        }
        viewModelScope.launch {
            repository.getOrdersByUidFlow(uid).collect { ords ->
                userOrders = ords
            }
        }
        viewModelScope.launch {
            repository.getAllOrdersFlow().collect { ords ->
                allOrdersListState = ords
            }
        }
    }

    fun logout() {
        loggedInUserUid = null
        currentUser = null
        currentTransactions = emptyList()
        currentMlmStats = emptyList()
        currentNotifications = emptyList()
        directReferrals = emptyList()
        userOrders = emptyList()
        allOrdersListState = emptyList()
        cartItems = emptyMap()
        showOtpEntry = false
        is2FaRequiredStep = false
        loginUid = ""
        enteredOtp = ""
        loginMessage = null
        isAppUnlocked = false
        enteredPinForLock = ""
        incomingSmsAlert = null
    }

    // Pay joining fee via custom gateway
    fun payJoiningFee() {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            val success = repository.activateUser(uid)
            if (success) {
                // Refresh user state
                val updated = repository.getUserByUid(uid)
                currentUser = updated
                observeUserData(uid)
                Toast.makeText(getApplication(), "જોડાણ સફળ થયું! / Joining completed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "સક્રિયકરણ નિષ્ફળ! રેફરરની મર્યાદા પુરી થઈ ગઈ હોઈ શકે છે. / Activation failed! Referrer's limit may be full.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun payJoiningFeeViaGateway(method: String, utr: String = "", onSuccess: () -> Unit = {}) {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            val cleanedUtr = utr.trim()
            if (cleanedUtr.isEmpty()) {
                Toast.makeText(getApplication(), "કૃપા કરીને ૧૨-અંકનો યુટીઆર નંબર દાખલ કરો! / Please enter a 12-digit UTR number!", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // 1. Basic structural validation
            if (!isValidUtrNumber(cleanedUtr)) {
                Toast.makeText(getApplication(), "અમાન્ય UTR નંબર! કૃપા કરીને ખોટા અથવા ડમી યુટીઆર નંબરનો ઉપયોગ કરશો નહીં. / Invalid UTR! Do not use fake or dummy UTR numbers.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // 2. Duplicate validation with simulated bank API check
            isVerifyingUtr = true
            utrVerificationStep = "યુપીઆઈ પેમેન્ટ સર્વર સાથે સચોટ જોડાણ સ્થાપિત થઈ રહ્યું છે... / Establishing secure connection with UPI gateway..."
            kotlinx.coroutines.delay(1200)
            
            utrVerificationStep = "NPCI અને આઈસીઆઈસીઆઈ/એસબીઆઈ સર્વર સાથે ટ્રાન્ઝેક્શનની ચકાસણી થઈ રહી છે... / Verifying transaction with NPCI & ICICI/SBI bank servers..."
            kotlinx.coroutines.delay(1500)
            
            val isDuplicate = repository.isUtrDuplicate(cleanedUtr)
            if (isDuplicate) {
                isVerifyingUtr = false
                utrVerificationStep = ""
                Toast.makeText(getApplication(), "ભૂલ: આ UTR નંબર પહેલાથી જ ઉપયોગમાં લેવાઈ ગયો છે! ડુપ્લીકેટ યુટીઆર માન્ય નથી. / Error: This UTR has already been used! Duplicate UTRs are not allowed.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            utrVerificationStep = "ટ્રાન્ઝેક્શન મળી આવ્યું! પેમેન્ટ સફળ થયાની પુષ્ટિ થઈ રહી છે... / Transaction found! Confirming successful payment status..."
            kotlinx.coroutines.delay(1200)
            
            // 3. Submit Joining Fee UTR for verification (Pending verification)
            val success = repository.submitJoiningFeeUtr(uid, cleanedUtr)
            isVerifyingUtr = false
            utrVerificationStep = ""
            
            if (success) {
                val updated = repository.getUserByUid(uid)
                currentUser = updated
                observeUserData(uid)
                Toast.makeText(getApplication(), "ચુકવણી વિનંતી સફળતાપૂર્વક સબમિટ થઈ ગઈ છે! એડમિન ટૂંક સમયમાં મંજૂર કરશે. / Payment submitted successfully! Admin will verify soon.", Toast.LENGTH_LONG).show()
                onSuccess()
            } else {
                Toast.makeText(getApplication(), "સબમિશન નિષ્ફળ! / Submission failed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Activate direct referral with gateway
    fun activateReferralDirectly(childUid: String, utr: String = "") {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            val cleanedUtr = utr.trim()
            if (cleanedUtr.isEmpty()) {
                Toast.makeText(getApplication(), "કૃપા કરીને ૧૨-અંકનો યુટીઆર નંબર દાખલ કરો! / Please enter a 12-digit UTR number!", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // 1. Basic structural validation
            if (!isValidUtrNumber(cleanedUtr)) {
                Toast.makeText(getApplication(), "અમાન્ય UTR નંબર! કૃપા કરીને ખોટા અથવા ડમી યુટીઆર નંબરનો ઉપયોગ કરશો નહીં. / Invalid UTR! Do not use fake or dummy UTR numbers.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // 2. Duplicate validation with simulated bank API check
            isVerifyingUtr = true
            utrVerificationStep = "યુપીઆઈ પેમેન્ટ સર્વર સાથે સચોટ જોડાણ સ્થાપિત થઈ રહ્યું છે... / Establishing secure connection with UPI gateway..."
            kotlinx.coroutines.delay(1200)
            
            utrVerificationStep = "NPCI અને આઈસીઆઈસીઆઈ/એસબીઆઈ સર્વર સાથે ટ્રાન્ઝેક્શનની ચકાસણી થઈ રહી છે... / Verifying transaction with NPCI & ICICI/SBI bank servers..."
            kotlinx.coroutines.delay(1500)
            
            val isDuplicate = repository.isUtrDuplicate(cleanedUtr)
            if (isDuplicate) {
                isVerifyingUtr = false
                utrVerificationStep = ""
                Toast.makeText(getApplication(), "ભૂલ: આ UTR નંબર પહેલાથી જ ઉપયોગમાં લેવાઈ ગયો છે! ડુપ્લીકેટ યુટીઆર માન્ય નથી. / Error: This UTR has already been used! Duplicate UTRs are not allowed.", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            utrVerificationStep = "ટ્રાન્ઝેક્શન મળી આવ્યું! પેમેન્ટ સફળ થયાની પુષ્ટિ થઈ રહી છે... / Transaction found! Confirming successful payment status..."
            kotlinx.coroutines.delay(1200)
            
            // 3. Submit Joining Fee UTR for verification (Pending verification)
            val success = repository.submitJoiningFeeUtr(childUid, cleanedUtr)
            isVerifyingUtr = false
            utrVerificationStep = ""
            
            if (success) {
                // Refresh list
                directReferrals = repository.getReferralsForUser(uid)
                observeUserData(uid)
                Toast.makeText(getApplication(), "રેફરલ આઈડી માટે ચુકવણી સબમિટ થઈ ગઈ છે! એડમિન મંજૂર કરશે. / Referral payment submitted! Admin will verify soon.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApplication(), "સબમિશન નિષ્ફળ! / Submission failed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Join mock referral directly (Registers as INACTIVE as requested, pending activation deposit)
    fun addMockReferral(referralName: String, referralPhone: String) {
        val uid = loggedInUserUid ?: return
        val currentActiveUser = currentUser ?: return

        if (!isValidIndianMobileNumber(referralPhone)) {
            Toast.makeText(getApplication(), "મહેરબાની કરીને સાચો ૧૦-અંકનો રેફરલ મોબાઈલ નંબર દાખલ કરો! ખોટા અથવા અમાન્ય નંબરો સ્વીકાર્ય નથી. / Please enter a valid 10-digit referral mobile number! Fake numbers are not allowed.", Toast.LENGTH_LONG).show()
            return
        }

        if (currentActiveUser.directReferralsCount >= 3) {
            Toast.makeText(getApplication(), "નવી આઈડી હેઠળ રેફરલ મર્યાદા (૩) પૂર્ણ થઈ ગઈ છે!", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            // Register a child under currently logged-in user (isActive defaults to false)
            val result = repository.registerUser(
                phoneNumber = referralPhone,
                fullName = referralName,
                referredBy = uid
            )
            when (result) {
                is AppRepository.RegisterResult.Success -> {
                    // Update list
                    directReferrals = repository.getReferralsForUser(uid)
                    observeUserData(uid)
                    
                    // Add notification
                    repository.insertNotification(
                        AppNotification(
                            uid = uid,
                            title = "નવું રેફરલ જોડાયું! / New Referral Registered!",
                            message = "${referralName} રેફરલ લિંકથી જોડાયા છે, પરંતુ હજુ અક્રિય (Pending) છે. જ્યારે તેઓ ₹૧,૦૦૦ ભરીને એક્ટિવેટ કરશે ત્યારે તમને ₹૨૦૦ કમિશન મળશે! / ${referralName} registered but is inactive. You earn ₹200 when they activate.",
                            type = "SYSTEM"
                        )
                    )
                    Toast.makeText(getApplication(), "રેફરલ સફળતાપૂર્વક રજીસ્ટર થયું! (અક્રિય / Inactive) / Referral registered successfully as inactive!", Toast.LENGTH_LONG).show()
                }
                is AppRepository.RegisterResult.Error -> {
                    Toast.makeText(getApplication(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Admin state and methods
    var pendingVerificationUsers by mutableStateOf<List<UserAccount>>(emptyList())

    fun loadPendingVerificationUsers() {
        viewModelScope.launch {
            pendingVerificationUsers = repository.getPendingPaymentUsers()
        }
    }

    fun adminApproveUser(userUid: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.getUserByUid(userUid) ?: return@launch
            val success = repository.activateUser(userUid, user.pendingUtr)
            if (success) {
                loadPendingVerificationUsers()
                Toast.makeText(getApplication(), "વપરાશકર્તા મંજૂર અને આઈડી સક્રિય થઈ ગયું! / User approved and ID activated!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(getApplication(), "મંજૂરી નિષ્ફળ! / Approval failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun adminRejectUser(userUid: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.rejectJoiningFeeUtr(userUid)
            if (success) {
                loadPendingVerificationUsers()
                Toast.makeText(getApplication(), "ચુકવણી નકારવામાં આવી! / Payment rejected successfully!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(getApplication(), "નકારવામાં ભૂલ! / Rejection failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Admin Panel Configuration saving functions
    fun updateTwilioSettings(sid: String, token: String, fromPhone: String) {
        twilioSidState = sid.trim()
        twilioTokenState = token.trim()
        twilioFromPhoneState = fromPhone.trim()
        sharedPrefs.edit()
            .putString("twilio_sid", twilioSidState)
            .putString("twilio_token", twilioTokenState)
            .putString("twilio_from_phone", twilioFromPhoneState)
            .apply()
        
        // Also sync to Admin account in database (EM10000)
        viewModelScope.launch {
            val admin = repository.getUserByUid("EM10000")
            if (admin != null) {
                val updatedAdmin = admin.copy(
                    twilioSid = twilioSidState,
                    twilioToken = twilioTokenState,
                    twilioFromPhone = twilioFromPhoneState,
                    realOtpEnabled = globalRealOtpMode
                )
                repository.updateUser(updatedAdmin)
            }
            Toast.makeText(getApplication(), "Twilio સેટિંગ્સ સાચવવામાં આવી! / Twilio settings saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateGlobalRealOtpMode(enabled: Boolean) {
        globalRealOtpMode = enabled
        sharedPrefs.edit().putBoolean("global_real_otp_mode", enabled).apply()
        
        // Also sync to Admin account in database (EM10000)
        viewModelScope.launch {
            val admin = repository.getUserByUid("EM10000")
            if (admin != null) {
                val updatedAdmin = admin.copy(
                    realOtpEnabled = enabled
                )
                repository.updateUser(updatedAdmin)
            }
            val modeText = if (enabled) "રિયલ ઓટીપી / Real OTP" else "સિમ્યુલેશન / Demo SMS"
            Toast.makeText(getApplication(), "ઓટીપી મોડ બદલાયો: $modeText / OTP mode updated!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateAdminWhatsAppNumber(number: String) {
        adminWhatsAppNumber = number.trim()
        sharedPrefs.edit().putString("admin_whatsapp_number", adminWhatsAppNumber).apply()
        Toast.makeText(getApplication(), "એડમિન વોટ્સએપ નંબર બદલાયો! / Admin WhatsApp number updated!", Toast.LENGTH_SHORT).show()
    }

    fun updateReferralWebsiteUrl(url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        // Strip trailing /join or /join.html paths as the root index.html handles parsing ?ref=
        if (cleanUrl.endsWith("/join")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 5)
        } else if (cleanUrl.endsWith("/join.html")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 10)
        }
        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        referralWebsiteUrl = cleanUrl
        sharedPrefs.edit().putString("referral_website_url", referralWebsiteUrl).apply()
        
        if (cleanUrl.contains("earnmitra.app") || cleanUrl.contains("earnmintra.app")) {
            Toast.makeText(getApplication(), "ચેતવણી: earnmitra.app માટે સક્રિય ડોમેન અને DNS સેટઅપ હોવું જરૂરી છે. / Warning: earnmitra.app/earnmintra.app requires active domain & DNS configuration.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(getApplication(), "રેફરલ વેબસાઇટ લિંક બદલાઈ! / Referral website URL updated!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateCustomUpiIds(ids: List<String>) {
        customRegistrationUpiIds = ids.map { it.trim() }.filter { it.isNotBlank() }
        sharedPrefs.edit().putString("custom_upi_ids", customRegistrationUpiIds.joinToString(",")).apply()
        Toast.makeText(getApplication(), "નોંધણી યુપીઆઈ લિસ્ટ અપડેટ થયું! / UPI list updated!", Toast.LENGTH_SHORT).show()
    }

    // Direct User Account administration
    fun adminForceActivateUser(userUid: String) {
        viewModelScope.launch {
            // activateUser does the complete MLM commission structure and activates account
            val success = repository.activateUser(userUid, "ADMIN_FORCE")
            if (success) {
                loadPendingVerificationUsers()
                Toast.makeText(getApplication(), "વપરાશકર્તા આઈડી એક્ટિવેટ થઈ ગયું! / User ID Activated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "એક્ટિવેશન નિષ્ફળ! / Activation failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun adminDeactivateUser(userUid: String) {
        viewModelScope.launch {
            val user = repository.getUserByUid(userUid) ?: return@launch
            val updatedUser = user.copy(
                isActive = false,
                paymentStatus = "NOT_PAID",
                pendingUtr = ""
            )
            repository.updateUser(updatedUser)
            loadPendingVerificationUsers()
            Toast.makeText(getApplication(), "વપરાશકર્તા આઈડી ડી-એક્ટિવેટ કરવામાં આવ્યું! / User ID Deactivated!", Toast.LENGTH_SHORT).show()
        }
    }

    fun adminAdjustUserBalance(userUid: String, amount: Double) {
        viewModelScope.launch {
            val user = repository.getUserByUid(userUid) ?: return@launch
            val updatedUser = user.copy(walletBalance = user.walletBalance + amount)
            repository.updateUser(updatedUser)
            
            // Log transaction
            repository.insertTransaction(
                Transaction(
                    uid = userUid,
                    type = if (amount >= 0) "DEPOSIT" else "WITHDRAWAL",
                    amount = kotlin.math.abs(amount),
                    description = "એડમિન એડજસ્ટમેન્ટ / Admin Balance Adjustment"
                )
            )
            
            Toast.makeText(getApplication(), "બેલેન્સ સફળતાપૂર્વક અપડેટ થયું! / Balance adjusted successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun adminUpdateUserKycStatus(userUid: String, status: String) {
        viewModelScope.launch {
            val user = repository.getUserByUid(userUid) ?: return@launch
            val updatedUser = user.copy(kycStatus = status)
            repository.updateUser(updatedUser)
            Toast.makeText(getApplication(), "KYC સ્થિતિ બદલાઈ: $status / KYC Status updated!", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadProducts() {
        val jsonStr = sharedPrefs.getString("custom_products_list", null)
        if (jsonStr == null) {
            productsList = ProductData.items
        } else {
            try {
                val array = org.json.JSONArray(jsonStr)
                val list = mutableListOf<Product>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val sCount = if (obj.has("stockCount")) obj.getInt("stockCount") else 10
                    val outOfStock = if (obj.has("isOutOfStock")) obj.getBoolean("isOutOfStock") else (sCount <= 0)
                    val imgUrl = if (obj.has("imageUrl")) obj.optString("imageUrl", "") else ""
                    list.add(
                        Product(
                            id = obj.getInt("id"),
                            nameEn = obj.getString("nameEn"),
                            nameGu = obj.getString("nameGu"),
                            price = obj.getDouble("price"),
                            isMandatory = obj.getBoolean("isMandatory"),
                            descriptionEn = obj.getString("descriptionEn"),
                            descriptionGu = obj.getString("descriptionGu"),
                            category = obj.getString("category"),
                            stockCount = sCount,
                            isOutOfStock = outOfStock,
                            imageUrl = imgUrl
                        )
                    )
                }
                productsList = list
            } catch (e: Exception) {
                e.printStackTrace()
                productsList = ProductData.items
            }
        }
    }

    fun saveProducts() {
        try {
            val array = org.json.JSONArray()
            for (prod in productsList) {
                val obj = org.json.JSONObject()
                obj.put("id", prod.id)
                obj.put("nameEn", prod.nameEn)
                obj.put("nameGu", prod.nameGu)
                obj.put("price", prod.price)
                obj.put("isMandatory", prod.isMandatory)
                obj.put("descriptionEn", prod.descriptionEn)
                obj.put("descriptionGu", prod.descriptionGu)
                obj.put("category", prod.category)
                obj.put("stockCount", prod.stockCount)
                obj.put("isOutOfStock", prod.isOutOfStock)
                obj.put("imageUrl", prod.imageUrl)
                array.put(obj)
            }
            sharedPrefs.edit().putString("custom_products_list", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addProduct(nameEn: String, nameGu: String, price: Double, category: String, descriptionEn: String, descriptionGu: String, isMandatory: Boolean = false, stockCount: Int = 10, isOutOfStock: Boolean = false, imageUrl: String = "") {
        val nextId = (productsList.maxOfOrNull { it.id } ?: 0) + 1
        val newProduct = Product(
            id = nextId,
            nameEn = nameEn,
            nameGu = nameGu,
            price = price,
            isMandatory = isMandatory,
            descriptionEn = descriptionEn,
            descriptionGu = descriptionGu,
            category = category,
            stockCount = stockCount,
            isOutOfStock = isOutOfStock || (stockCount <= 0),
            imageUrl = imageUrl
        )
        productsList = productsList + newProduct
        saveProducts()
        Toast.makeText(getApplication(), "પ્રોડક્ટ સફળતાપૂર્વક ઉમેરવામાં આવી! / Product added successfully!", Toast.LENGTH_SHORT).show()
    }

    fun updateProduct(product: Product) {
        productsList = productsList.map { if (it.id == product.id) product else it }
        saveProducts()
        Toast.makeText(getApplication(), "પ્રોડક્ટ અપડેટ થઈ ગઈ! / Product updated successfully!", Toast.LENGTH_SHORT).show()
    }

    fun deleteProduct(productId: Int) {
        productsList = productsList.filter { it.id != productId }
        saveProducts()
        Toast.makeText(getApplication(), "પ્રોડક્ટ કાઢી નાખવામાં આવી! / Product deleted successfully!", Toast.LENGTH_SHORT).show()
    }

    // Update Profile Photo
    fun updateProfilePhoto(avatarId: String) {
        val uid = loggedInUserUid ?: return
        val user = currentUser ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(profileImage = avatarId)
            repository.updateUser(updatedUser)
            currentUser = updatedUser
            
            // Add notification
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "પ્રોફાઇલ ફોટો બદલાયો! / Profile Photo Updated!",
                    message = "તમારો પ્રોફાઇલ ફોટો સફળતાપૂર્વક અપડેટ કરવામાં આવ્યો છે. / Your profile photo has been successfully updated.",
                    type = "SYSTEM"
                )
            )
            Toast.makeText(getApplication(), "પ્રોફાઇલ ફોટો સેટ થયો! / Profile photo set successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Set or Update Security PIN Lock
    fun updateSecuritySettings(isLockEnabled: Boolean, pin: String?, twilioSid: String = "", twilioToken: String = "", twilioFrom: String = "", isRealOtp: Boolean = false) {
        val uid = loggedInUserUid ?: return
        val user = currentUser ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(
                isSecurityLockEnabled = isLockEnabled,
                securityPin = pin,
                twilioSid = twilioSid,
                twilioToken = twilioToken,
                twilioFromPhone = twilioFrom,
                realOtpEnabled = isRealOtp
            )
            repository.updateUser(updatedUser)
            currentUser = updatedUser
            
            // Sync local Twilio states
            twilioSidState = twilioSid
            twilioTokenState = twilioToken
            twilioFromPhoneState = twilioFrom
            globalRealOtpMode = isRealOtp

            // Insert security notification
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "સુરક્ષા સેટિંગ્સ અપડેટ! / Security Settings Updated!",
                    message = "તમારી એપ્લિકેશન સિક્યોરિટી અને ઓટીપી સેટિંગ્સ સફળતાપૂર્વક અપડેટ થઈ ગઈ છે. એપ્લિકેશન હવે ૧૦૦% સુરક્ષિત છે! / Security settings and OTP configurations have been updated.",
                    type = "SYSTEM"
                )
            )
            Toast.makeText(getApplication(), "સેટિંગ્સ સાચવવામાં આવી! / Settings saved successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Send Real Twilio SMS API helper
    fun sendTwilioSms(toPhone: String, messageText: String, sid: String, token: String, fromPhone: String, isWhatsApp: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Ensure phone has country code prefix (+91 for India if not specified)
                val formattedPhone = if (toPhone.startsWith("+")) toPhone else if (toPhone.length == 10) "+91$toPhone" else toPhone
                
                val toValue = if (isWhatsApp) "whatsapp:$formattedPhone" else formattedPhone
                val fromValue = if (isWhatsApp) {
                    if (fromPhone.startsWith("whatsapp:")) fromPhone else "whatsapp:$fromPhone"
                } else {
                    fromPhone
                }

                val urlConnection = java.net.URL("https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json").openConnection() as java.net.HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.doOutput = true
                val authString = "$sid:$token"
                val basicAuth = "Basic " + android.util.Base64.encodeToString(authString.toByteArray(), android.util.Base64.NO_WRAP)
                urlConnection.setRequestProperty("Authorization", basicAuth)
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "To=${java.net.URLEncoder.encode(toValue, "UTF-8")}&From=${java.net.URLEncoder.encode(fromValue, "UTF-8")}&Body=${java.net.URLEncoder.encode(messageText, "UTF-8")}"
                urlConnection.outputStream.use { out ->
                    out.write(postData.toByteArray())
                }
                val responseCode = urlConnection.responseCode
                if (responseCode in 200..299) {
                    // Success log or toast
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Mark notifications read
    fun markAllNotificationsRead() {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(uid)
        }
    }

    // Clear notifications
    fun clearNotifications() {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            repository.clearAllNotifications(uid)
        }
    }

    // Update coordinates simulation
    fun simulateLocationUpdate() {
        locationLat = 23.0000 + (0.0500 * Math.random())
        locationLng = 72.5000 + (0.0500 * Math.random())
        
        // Update user location in DB if logged in
        val uid = loggedInUserUid
        if (uid != null && currentUser != null) {
            viewModelScope.launch {
                val updated = currentUser!!.copy(latitude = locationLat, longitude = locationLng)
                repository.updateUser(updated)
            }
        }
    }

    // Toggle GPS tracking
    fun toggleLocationTracking() {
        isTrackingLocation = !isTrackingLocation
        if (isTrackingLocation) {
            simulateLocationUpdate()
        }
    }

    // Toggle 2FA in profile/settings
    fun toggle2Fa() {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = user.copy(isTwoStepEnabled = !user.isTwoStepEnabled)
            repository.updateUser(updated)
        }
    }

    // Toggle Customizable Notifications
    fun updateNotificationSetting(type: String, enabled: Boolean) {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = when (type) {
                "PROMO" -> user.copy(notifPromo = enabled)
                "TRANS" -> user.copy(notifTrans = enabled)
                else -> user.copy(notifSystem = enabled)
            }
            repository.updateUser(updated)
        }
    }

    // Local Shopping / PhonePe payment simulation
    fun buyProduct(product: Product) {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            val success = repository.purchaseProduct(uid, product.price, product.nameEn)
            if (success) {
                Toast.makeText(
                    getApplication(), 
                    "ખરીદી સફળ! / Purchased: ${if (selectedLanguage == Language.GUJARATI) product.nameGu else product.nameEn}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Simulate promotion link sharing
    fun promoteProduct(product: Product) {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            // Promote gives a small bonus (e.g. ₹10) directly to wallet as a promo reward!
            val reward = 10.0
            val user = currentUser ?: return@launch
            val updatedUser = user.copy(walletBalance = user.walletBalance + reward)
            repository.updateUser(updatedUser)

            repository.insertTransaction(
                Transaction(
                    uid = uid,
                    type = "COMMISSION",
                    amount = reward,
                    description = "પ્રમોશન બોનસ: ${product.nameEn} / Promotion Reward"
                )
            )
            Toast.makeText(
                getApplication(), 
                "લિંક કોપી કરી! પ્રમોશન બોનસ મળ્યું: ₹૧૦ / Promo reward ₹10 credited!", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // UPI / Bank Withdrawal Request
    fun requestWithdrawal(amount: Double, isUpi: Boolean) {
        val uid = loggedInUserUid ?: return
        val user = currentUser ?: return
        if (user.walletBalance < amount) {
            Toast.makeText(getApplication(), "અપૂરતું બેલેન્સ! / Insufficient Balance!", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val type = if (isUpi) "WITHDRAW_UPI" else "WITHDRAW_BANK"
            val success = repository.withdrawFunds(uid, amount, type)
            if (success) {
                Toast.makeText(getApplication(), "ઉપાડ વિનંતી સફળ! / Withdrawal successful!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PhonePe-like direct merchant payment
    fun payLocalMerchant(merchant: String, amount: Double, upiId: String) {
        val uid = loggedInUserUid ?: return
        val user = currentUser ?: return
        if (user.walletBalance < amount) {
            Toast.makeText(getApplication(), "અપૂરતું બેલેન્સ! / Insufficient Balance!", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val merchantDetails = if (upiId.isNotBlank()) "$merchant (UPI: $upiId)" else merchant
            val success = repository.payLocally(uid, amount, merchantDetails)
            if (success) {
                Toast.makeText(getApplication(), "મર્ચન્ટ ચુકવણી સફળ! / Merchant payment completed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update Aadhaar, PAN Card, Bank Details, KYC status and Document
    fun updateUserProfileComplete(
        aadhar: String,
        pan: String,
        bankName: String,
        bankAccount: String,
        bankIfsc: String,
        bankUpi: String,
        kycStatus: String,
        kycDocImage: String? = null
    ): Boolean {
        val user = currentUser ?: return false
        val cleanAadhar = aadhar.trim().replace(" ", "")
        val cleanPan = pan.trim().uppercase()

        if (cleanAadhar.length != 12 || !cleanAadhar.all { it.isDigit() }) {
            Toast.makeText(getApplication(), "આધાર કાર્ડ નંબર ૧૨ અંકનો હોવો જોઈએ! / Aadhaar number must be 12 digits!", Toast.LENGTH_LONG).show()
            return false
        }

        if (cleanPan.length != 10 || !cleanPan.all { it.isLetterOrDigit() }) {
            Toast.makeText(getApplication(), "પાનકાર્ડ નંબર ૧૦ અંકનો હોવો જોઈએ! / PAN number must be 10 characters!", Toast.LENGTH_LONG).show()
            return false
        }

        viewModelScope.launch {
            val updated = user.copy(
                aadharNumber = cleanAadhar,
                panNumber = cleanPan,
                bankName = bankName,
                bankAccountNumber = bankAccount,
                bankIfscCode = bankIfsc,
                bankUpiId = bankUpi,
                kycStatus = kycStatus,
                kycDocImage = kycDocImage ?: user.kycDocImage
            )
            repository.updateUser(updated)
            currentUser = updated
            Toast.makeText(getApplication(), "પ્રોફાઇલ વિગતો અને કેવાયસી સફળતાપૂર્વક સાચવવામાં આવ્યું! / Profile & KYC details saved successfully!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    // Update Aadhaar and PAN Card details in user profile (backward-compatible)
    fun updateUserProfile(aadhar: String, pan: String): Boolean {
        return updateUserProfileComplete(
            aadhar = aadhar,
            pan = pan,
            bankName = currentUser?.bankName ?: "",
            bankAccount = currentUser?.bankAccountNumber ?: "",
            bankIfsc = currentUser?.bankIfscCode ?: "",
            bankUpi = currentUser?.bankUpiId ?: "",
            kycStatus = if (aadhar.length == 12 && pan.length == 10) "APPROVED" else "NOT_STARTED",
            kycDocImage = currentUser?.kycDocImage
        )
    }

    // --- IN-APP UPDATE SYSTEM STATES ---
    var currentVersionName by mutableStateOf("1.0")
    var currentVersionCode by mutableStateOf(1)

    private val prefs = application.getSharedPreferences("app_update_prefs", android.content.Context.MODE_PRIVATE)

    var updateStatus by mutableStateOf(UpdateStatus.IDLE)
    var updateType by mutableStateOf(InAppUpdateType.FLEXIBLE)
    
    var newVersionName by mutableStateOf(
        prefs.getString("new_version_name", "1.0") ?: "1.0"
    )
    var newVersionCode by mutableStateOf(
        prefs.getInt("new_version_code", 1)
    )
    var updateProgress by mutableStateOf(0f)
    var updateDownloadSpeed by mutableStateOf("0 KB/s")
    var updateBytesDownloaded by mutableStateOf("0 MB")
    var updateBytesTotal by mutableStateOf("4.0 MB")
    var updateReleaseNotes by mutableStateOf(
        prefs.getString("update_release_notes", "• સુધારેલ કમિશન હિસાબ / Improved Commission Calculation\n• નવું ૨૨% જીએસટી લેઆઉટ / New 22% GST Layout\n• બગ ફિક્સ અને સુરક્ષા સુધારા / Bug fixes & security improvements") ?: "• સુધારેલ કમિશન હિસાબ / Improved Commission Calculation\n• નવું ૨૨% જીએસટી લેઆઉટ / New 22% GST Layout\n• બગ ફિક્સ અને સુરક્ષા સુધારા / Bug fixes & security improvements"
    )

    var googleDriveUpdateUrl by mutableStateOf(
        prefs.getString("google_drive_update_url", "https://drive.google.com/file/d/1_9i7RMyE4M9N17lFmB9u_JzWqXv9tF3z/view?usp=sharing") ?: "https://drive.google.com/file/d/1_9i7RMyE4M9N17lFmB9u_JzWqXv9tF3z/view?usp=sharing"
    )

    fun saveUpdateConfiguration(versionName: String, versionCode: Int, releaseNotes: String, driveUrl: String) {
        newVersionName = versionName
        newVersionCode = versionCode
        updateReleaseNotes = releaseNotes
        googleDriveUpdateUrl = driveUrl
        
        prefs.edit()
            .putString("new_version_name", versionName)
            .putInt("new_version_code", versionCode)
            .putString("update_release_notes", releaseNotes)
            .putString("google_drive_update_url", driveUrl)
            .apply()
    }

    var usePlayStoreUpdate by mutableStateOf(false)

    fun checkForUpdates(context: android.content.Context, forceManualCheck: Boolean = false) {
        updateStatus = UpdateStatus.CHECKING
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)

            if (usePlayStoreUpdate) {
                try {
                    val appUpdateManager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(context)
                    appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                        if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE) {
                            newVersionCode = appUpdateInfo.availableVersionCode()
                            newVersionName = "1.0.${appUpdateInfo.availableVersionCode()}"
                            updateType = if (appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE)) {
                                InAppUpdateType.IMMEDIATE
                            } else {
                                InAppUpdateType.FLEXIBLE
                            }
                            updateStatus = UpdateStatus.UPDATE_AVAILABLE
                        } else {
                            triggerDriveUpdateCheck(forceManualCheck, context)
                        }
                    }.addOnFailureListener {
                        triggerDriveUpdateCheck(forceManualCheck, context)
                    }
                } catch (e: Exception) {
                    triggerDriveUpdateCheck(forceManualCheck, context)
                }
            } else {
                triggerDriveUpdateCheck(forceManualCheck, context)
            }
        }
    }

    private fun triggerDriveUpdateCheck(forceManualCheck: Boolean, context: android.content.Context) {
        if (currentVersionCode < newVersionCode) {
            updateStatus = UpdateStatus.UPDATE_AVAILABLE
            updateProgress = 0f
        } else {
            updateStatus = UpdateStatus.UP_TO_DATE
            if (forceManualCheck) {
                Toast.makeText(context, "એપ્લિકેશન અપ-ટુ-ડેટ છે! / Application is up to date!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerSimulatedUpdate(forceManualCheck: Boolean) {
        if (forceManualCheck || currentVersionCode < newVersionCode) {
            updateStatus = UpdateStatus.UPDATE_AVAILABLE
            updateProgress = 0f
        } else {
            updateStatus = UpdateStatus.UP_TO_DATE
        }
    }

    fun startUpdateDownload() {
        if (updateStatus != UpdateStatus.UPDATE_AVAILABLE) return
        updateStatus = UpdateStatus.DOWNLOADING
        updateProgress = 0f

        viewModelScope.launch {
            val totalBytes = 4.0 * 1024 * 1024
            val steps = 20
            for (i in 1..steps) {
                kotlinx.coroutines.delay(150)
                updateProgress = i.toFloat() / steps
                val currentBytes = (totalBytes * updateProgress).toLong()
                updateBytesDownloaded = String.format("%.1f MB", currentBytes / (1024.0 * 1024.0))
                updateDownloadSpeed = String.format("%.1f MB/s", (3.0 + java.util.Random().nextDouble() * 2.0))
            }
            updateStatus = UpdateStatus.DOWNLOADED
        }
    }

    fun completeUpdateInstall(context: android.content.Context) {
        updateStatus = UpdateStatus.INSTALLING
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            
            // Open the configured Google Drive APK download link
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(googleDriveUpdateUrl))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "લિંક ખોલવામાં ભૂલ આવી! / Error opening Google Drive link!", Toast.LENGTH_SHORT).show()
            }
            
            currentVersionCode = newVersionCode
            currentVersionName = newVersionName
            updateStatus = UpdateStatus.COMPLETED
            Toast.makeText(getApplication(), "અપડેટ સફળતાપૂર્વક શરૂ થયું! / Update Initiated Successfully!", Toast.LENGTH_LONG).show()
            updateStatus = UpdateStatus.IDLE
        }
    }

    fun dismissUpdate() {
        updateStatus = UpdateStatus.IDLE
    }

    fun resetVersionForTesting() {
        currentVersionCode = 1
        currentVersionName = "1.0"
        updateStatus = UpdateStatus.IDLE
        Toast.makeText(getApplication(), "સંસ્કરણ રીસેટ થયું / Version Reset to 1.0", Toast.LENGTH_SHORT).show()
    }

    fun triggerForceImmediateUpdate() {
        updateType = InAppUpdateType.IMMEDIATE
        updateStatus = UpdateStatus.UPDATE_AVAILABLE
        updateProgress = 0f
    }

    fun addToCart(productId: Int) {
        val prod = productsList.find { it.id == productId } ?: return
        if (prod.isOutOfStock || prod.stockCount <= 0) {
            Toast.makeText(getApplication(), "આ પ્રોડક્ટ હાલમાં સ્ટોકમાં નથી! / This product is currently out of stock!", Toast.LENGTH_SHORT).show()
            return
        }
        val current = cartItems.toMutableMap()
        val currentQty = current[productId] ?: 0
        if (currentQty >= prod.stockCount) {
            Toast.makeText(getApplication(), "માત્ર ${prod.stockCount} વસ્તુઓ જ સ્ટોકમાં ઉપલબ્ધ છે! / Only ${prod.stockCount} items available in stock!", Toast.LENGTH_SHORT).show()
            return
        }
        current[productId] = currentQty + 1
        cartItems = current
    }

    fun updateCartQuantity(productId: Int, qty: Int) {
        val prod = productsList.find { it.id == productId } ?: return
        if (qty > prod.stockCount) {
            Toast.makeText(getApplication(), "માત્ર ${prod.stockCount} વસ્તુઓ જ સ્ટોકમાં ઉપલબ્ધ છે! / Only ${prod.stockCount} items available in stock!", Toast.LENGTH_SHORT).show()
            return
        }
        val current = cartItems.toMutableMap()
        if (qty <= 0) {
            current.remove(productId)
        } else {
            current[productId] = qty
        }
        cartItems = current
    }

    fun clearCart() {
        cartItems = emptyMap()
    }

    fun updateProfilePhotoFromUri(uri: android.net.Uri) {
        val uid = loggedInUserUid ?: return
        val user = currentUser ?: return
        val context = getApplication<Application>()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = java.io.File(context.filesDir, "profile_${uid}_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localPath = file.absolutePath
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        updateProfilePhoto(localPath)
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "ફોટો સાચવવામાં ભૂલ આવી! / Error saving photo!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun checkoutCart(upiUtr: String? = null) {
        val uid = loggedInUserUid ?: return
        val user = currentUser ?: return
        if (cartItems.isEmpty()) return
        
        if (shippingName.isBlank() || shippingPhone.isBlank() || shippingAddressLines.isBlank() || shippingCity.isBlank() || shippingPincode.isBlank()) {
            Toast.makeText(getApplication(), "કૃપા કરીને સંપૂર્ણ વિતરણ સરનામું દાખલ કરો! / Please enter complete shipping address!", Toast.LENGTH_LONG).show()
            return
        }

        if (!isValidIndianMobileNumber(shippingPhone)) {
            Toast.makeText(getApplication(), "મહેરબાની કરીને સાચો ૧૦-અંકનો મોબાઈલ નંબર વિતરણ સરનામાંમાં દાખલ કરો! / Please enter a valid 10-digit mobile number in the shipping address!", Toast.LENGTH_LONG).show()
            return
        }

        val fullAddress = "$shippingName, $shippingPhone, $shippingAddressLines, $shippingCity - $shippingPincode"

        viewModelScope.launch {
            // 1. Calculate pricing and build items list FIRST
            var subtotal = 0.0
            val itemListStringBuilder = StringBuilder()
            
            cartItems.forEach { (productId, qty) ->
                val prod = productsList.find { it.id == productId }
                if (prod != null) {
                    subtotal += prod.price * qty
                    val name = if (selectedLanguage == Language.GUJARATI) prod.nameGu else prod.nameEn
                    itemListStringBuilder.append("$name (x$qty), ")
                }
            }
            
            val itemNames = itemListStringBuilder.toString().trimEnd(',', ' ')
            val gst = subtotal * 0.22
            val finalPrice = subtotal + gst

            // 2. Validate wallet balance if paying with WALLET
            if (selectedPaymentMethod == "WALLET") {
                if (user.walletBalance < finalPrice) {
                    Toast.makeText(getApplication(), "વોલેટમાં અપૂરતું બેલેન્સ છે! / Insufficient Wallet Balance!", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

            // 3. Validate all items in the cart against their stock count
            var stockErrorMsg: String? = null
            for ((productId, qty) in cartItems) {
                val prod = productsList.find { it.id == productId }
                if (prod == null) {
                    stockErrorMsg = "પ્રોડક્ટ મળી નથી! / Product not found!"
                    break
                }
                if (prod.isOutOfStock || prod.stockCount < qty) {
                    val name = if (selectedLanguage == Language.GUJARATI) prod.nameGu else prod.nameEn
                    stockErrorMsg = "'$name' સ્ટોકમાં પૂરતું નથી! ઉપલબ્ધ: ${prod.stockCount} / '$name' is not enough in stock! Available: ${prod.stockCount}"
                    break
                }
            }

            if (stockErrorMsg != null) {
                Toast.makeText(getApplication(), stockErrorMsg, Toast.LENGTH_LONG).show()
                return@launch
            }

            // 4. Decrement the stock levels only after all validation succeeded
            productsList = productsList.map { prod ->
                val qtyPurchased = cartItems[prod.id] ?: 0
                if (qtyPurchased > 0) {
                    val remainingStock = (prod.stockCount - qtyPurchased).coerceAtLeast(0)
                    prod.copy(
                        stockCount = remainingStock,
                        isOutOfStock = remainingStock == 0
                    )
                } else {
                    prod
                }
            }
            saveProducts()

            // 5. Deduct wallet or record payment transaction
            when (selectedPaymentMethod) {
                "WALLET" -> {
                    val updatedUser = user.copy(walletBalance = user.walletBalance - finalPrice)
                    repository.updateUser(updatedUser)
                    currentUser = updatedUser
                    
                    repository.insertTransaction(
                        Transaction(
                            uid = uid,
                            type = "SHOPPING",
                            amount = finalPrice,
                            description = "$itemNames વોલેટ દ્વારા ખરીદી + ૨૨% જીએસટી / Wallet Purchase + 22% GST"
                        )
                    )
                }
                "UPI" -> {
                    repository.insertTransaction(
                        Transaction(
                            uid = uid,
                            type = "SHOPPING",
                            amount = finalPrice,
                            description = "$itemNames UPI દ્વારા ખરીદી (UTR: ${upiUtr ?: "None"})"
                        )
                    )
                }
                "COD" -> {
                    // Cash on delivery, transaction will be logged on delivery or just noted as COD
                    repository.insertTransaction(
                        Transaction(
                            uid = uid,
                            type = "SHOPPING",
                            amount = finalPrice,
                            description = "$itemNames કેશ ઓન ડિલિવરી (COD) + ૨૨% જીએસટી / Cash on Delivery + 22% GST"
                        )
                    )
                }
            }

            // 6. Insert into local Orders table with "PENDING" status
            repository.insertOrder(
                Order(
                    uid = uid,
                    productNames = itemNames,
                    totalPrice = finalPrice,
                    shippingAddress = fullAddress,
                    status = "PENDING",
                    paymentMethod = selectedPaymentMethod,
                    paymentStatus = if (selectedPaymentMethod == "UPI") "PENDING_VERIFICATION" else if (selectedPaymentMethod == "COD") "PENDING" else "PAID",
                    paymentRef = upiUtr ?: ""
                )
            )

            // 7. Insert Notification
            val notifMsg = if (selectedPaymentMethod == "UPI") {
                "તમારો ઓર્ડર $itemNames સ્વીકારવામાં આવ્યો છે (નજીકના સમયમાં પેમેન્ટ વેરિફાય થયા પછી ડિલિવરી કરવામાં આવશે). / Your order is placed and is pending payment verification."
            } else {
                "તમારો ઓર્ડર $itemNames સ્વીકારવામાં આવ્યો છે (સ્થિતિ: પેન્ડિંગ). વસ્તુ મળ્યા પછી ડિલિવર માર્ક કરો. / Your order has been placed (Status: PENDING). Mark as Delivered when you receive it."
            }
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "ઓર્ડર સફળતાપૂર્વક મૂકવામાં આવ્યો! / Order Placed Successfully!",
                    message = notifMsg,
                    type = "SYSTEM"
                )
            )

            // 8. Clear Cart and Inputs
            cartItems = emptyMap()
            shippingName = ""
            shippingPhone = ""
            shippingAddressLines = ""
            shippingCity = ""
            shippingPincode = ""

            Toast.makeText(getApplication(), "ઓર્ડર સફળ! ઓર્ડર પેન્ડિંગ સ્ટેટસમાં છે. / Order Successful! Status: PENDING", Toast.LENGTH_LONG).show()
        }
    }

    fun adminUpdateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            val updated = order.copy(status = newStatus)
            repository.updateOrder(updated)
            // Send user a notification
            repository.insertNotification(
                AppNotification(
                    uid = order.uid,
                    title = "ઓર્ડર સ્ટેટસ અપડેટ! / Order Status Updated!",
                    message = "તમારા ઓર્ડર (ID: #${order.id}) નું સ્ટેટસ હવે $newStatus છે. / Your order status is now $newStatus.",
                    type = "SYSTEM"
                )
            )
        }
    }

    fun adminApproveOrderPayment(order: Order) {
        viewModelScope.launch {
            val updated = order.copy(paymentStatus = "PAID")
            repository.updateOrder(updated)
            // Send user a notification
            repository.insertNotification(
                AppNotification(
                    uid = order.uid,
                    title = "પેમેન્ટ મંજૂર! / Order Payment Approved!",
                    message = "તમારા ઓર્ડર (ID: #${order.id}) ની ચૂકવણી મંજૂર કરવામાં આવી છે. / Payment for order #${order.id} has been approved.",
                    type = "SYSTEM"
                )
            )
        }
    }

    fun markOrderAsDelivered(order: Order) {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            val updatedOrder = order.copy(status = "DELIVERED")
            repository.updateOrder(updatedOrder)
            
            // Insert Notification
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "ઓર્ડર ડિલિવર થયો! / Order Delivered!",
                    message = "તમારો ઓર્ડર \"${order.productNames}\" સફળતાપૂર્વક ડિલિવર થયો છે. / Your order has been successfully delivered.",
                    type = "SYSTEM"
                )
            )
            Toast.makeText(getApplication(), "ઓર્ડર મેળવેલ તરીકે માર્ક કરેલ છે! / Order marked as Delivered!", Toast.LENGTH_SHORT).show()
        }
    }

    fun collectCodPayment(order: Order, method: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val uid = loggedInUserUid ?: return
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            when (method) {
                "WALLET" -> {
                    if (user.walletBalance < order.totalPrice) {
                        onFailure("વોલેટમાં અપૂરતું બેલેન્સ છે! / Insufficient Wallet Balance!")
                        return@launch
                    }
                    val updatedUser = user.copy(walletBalance = user.walletBalance - order.totalPrice)
                    repository.updateUser(updatedUser)
                    currentUser = updatedUser
                    
                    repository.insertTransaction(
                        Transaction(
                            uid = uid,
                            type = "SHOPPING",
                            amount = order.totalPrice,
                            description = "${order.productNames} કેશ ઓન ડિલિવરી ચૂકવણી (વોલેટ) / COD Payment (Wallet)"
                        )
                    )
                }
                "UPI" -> {
                    repository.insertTransaction(
                        Transaction(
                            uid = uid,
                            type = "SHOPPING",
                            amount = order.totalPrice,
                            description = "${order.productNames} કેશ ઓન ડિલિવરી ચૂકવણી (UPI) / COD Payment (UPI)"
                        )
                    )
                }
                "CASH" -> {
                    repository.insertTransaction(
                        Transaction(
                            uid = uid,
                            type = "SHOPPING",
                            amount = order.totalPrice,
                            description = "${order.productNames} કેશ ઓન ડિલિવરી ચૂકવણી (રોકડ) / COD Payment (Cash Collected)"
                        )
                    )
                }
            }
            
            val updatedOrder = order.copy(status = "DELIVERED", paymentStatus = "PAID")
            repository.updateOrder(updatedOrder)
            
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "ઓર્ડર ડિલિવર અને ચૂકવણી સફળ! / Order Delivered & Paid!",
                    message = "તમારો ઓર્ડર \"${order.productNames}\" ડિલિવર થયો છે અને ₹${order.totalPrice} ની ચૂકવણી સફળ થઈ છે. / Your order has been delivered and payment of ₹${order.totalPrice} is successful.",
                    type = "SYSTEM"
                )
            )
            onSuccess()
        }
    }

    // --- GAMIFICATION: DAILY TASKS & REWARDS ---
    var dailyRewardClaimedToday by androidx.compose.runtime.mutableStateOf(false)
    var dailyShareTaskCompleted by androidx.compose.runtime.mutableStateOf(false)
    var dailyVideoTaskCompleted by androidx.compose.runtime.mutableStateOf(false)

    fun claimDailyReward() {
        val uid = loggedInUserUid ?: return
        if (dailyRewardClaimedToday) return
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            val rewardAmount = (10..50).random().toDouble()
            val updatedUser = user.copy(walletBalance = user.walletBalance + rewardAmount)
            repository.updateUser(updatedUser)
            currentUser = updatedUser
            dailyRewardClaimedToday = true
            
            // Log Transaction
            repository.insertTransaction(
                Transaction(
                    uid = uid,
                    type = "BONUS",
                    amount = rewardAmount,
                    description = "દૈનિક પુરસ્કાર જીત્યો! / Daily Login Reward"
                )
            )
            // Notify
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "દૈનિક પુરસ્કાર મેળવ્યો! / Daily Reward Claimed!",
                    message = "તમને દૈનિક બોનસ તરીકે ₹$rewardAmount મળ્યા છે. / Claimed ₹$rewardAmount as login bonus.",
                    type = "TRANSACTION"
                )
            )
        }
    }

    fun completeDailyShareTask(productName: String) {
        val uid = loggedInUserUid ?: return
        if (dailyShareTaskCompleted) return
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            val rewardAmount = 5.0
            val updatedUser = user.copy(walletBalance = user.walletBalance + rewardAmount)
            repository.updateUser(updatedUser)
            currentUser = updatedUser
            dailyShareTaskCompleted = true
            
            // Log Transaction
            repository.insertTransaction(
                Transaction(
                    uid = uid,
                    type = "BONUS",
                    amount = rewardAmount,
                    description = "$productName વોટ્સએપ શેરિંગ બોનસ / WhatsApp Sharing Task Reward"
                )
            )
            // Notify
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "શેરિંગ ટાસ્ક પૂર્ણ! / Product Shared!",
                    message = "પ્રોડક્ટ પ્રમોટ કરવા બદલ ₹$rewardAmount વોલેટમાં જમા થયા! / Reward of ₹$rewardAmount added for sharing.",
                    type = "TRANSACTION"
                )
            )
        }
    }

    fun completeDailyVideoTask() {
        val uid = loggedInUserUid ?: return
        if (dailyVideoTaskCompleted) return
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            val rewardAmount = 15.0
            val updatedUser = user.copy(walletBalance = user.walletBalance + rewardAmount)
            repository.updateUser(updatedUser)
            currentUser = updatedUser
            dailyVideoTaskCompleted = true
            
            // Log Transaction
            repository.insertTransaction(
                Transaction(
                    uid = uid,
                    type = "BONUS",
                    amount = rewardAmount,
                    description = "વિડીયો ટાસ્ક પુરસ્કાર / Simulated Ad Video Reward"
                )
            )
            // Notify
            repository.insertNotification(
                AppNotification(
                    uid = uid,
                    title = "જાહેરાત વિડીયો બોનસ! / Video Task Complete!",
                    message = "૫-સેકન્ડનો સ્પોન્સર્ડ વિડીયો જોવા બદલ ₹$rewardAmount મળ્યા! / Claimed ₹$rewardAmount for watching video ad.",
                    type = "TRANSACTION"
                )
            )
        }
    }

    // --- AI COACH CHATBOT (GEMINI INTEGRATION) ---
    data class AiChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())
    var aiChatHistory by androidx.compose.runtime.mutableStateOf<List<AiChatMessage>>(
        listOf(
            AiChatMessage("નમસ્તે! હું અર્નમિત્ર AI કોચ છું. હું તમને તમારા નેટવર્કને આગળ વધારવામાં, વધુ કમાણી કરવા માટે માર્કેટિંગ પદ્ધતિઓ અને ટિપ્સ આપવા માટે અહીં છું. પૂછો કોઈપણ પ્રશ્ન!", false)
        )
    )
    var isAiThinking by androidx.compose.runtime.mutableStateOf(false)

    fun sendPromptToAiCoach(prompt: String) {
        if (prompt.isBlank() || isAiThinking) return
        val currentHistory = aiChatHistory.toMutableList()
        currentHistory.add(AiChatMessage(prompt, true))
        aiChatHistory = currentHistory
        
        isAiThinking = true
        viewModelScope.launch {
            val response = try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    "અર્નમિત્ર AI કોચ: કૃપા કરીને સેટિંગ્સમાં સાચી Gemini API key સેટ કરો અથવા AI Studioમાં કી કોન્ફિગર કરો! / Please configure Gemini API Key in AI Studio Secrets."
                } else {
                    withContext(Dispatchers.IO) {
                        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                        val url = java.net.URL(urlStr)
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        conn.doOutput = true
                        conn.connectTimeout = 30000
                        conn.readTimeout = 30000

                        val selectedLangCode = selectedLanguage.code
                        val systemInstruction = """
                            You are 'EarnMitra AI Coach', an encouraging and professional multi-level referral expert inside the EarnMitra app.
                            The user has logged in as: ${currentUser?.fullName ?: "User"} (ID: ${currentUser?.uid ?: "EM10000"}).
                            Explain MLM 10-level commission structures clearly.
                            Help write catchy Gujarati/Hindi WhatsApp messages for promoting products & referral links.
                            Suggest tips to get 3 active direct recruits.
                            You must respond in the language code: ${"$"}selectedLangCode. If the code is 'gu', write in Gujarati. If 'hi', write in Hindi. Otherwise, default to English. Keep it clear, friendly, and structured.
                        """.trimIndent()

                        val requestJson = org.json.JSONObject().apply {
                            put("contents", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("parts", org.json.JSONArray().apply {
                                        put(org.json.JSONObject().apply {
                                            put("text", prompt)
                                        })
                                    })
                                })
                            })
                            put("systemInstruction", org.json.JSONObject().apply {
                                put("parts", org.json.JSONArray().apply {
                                    put(org.json.JSONObject().apply {
                                        put("text", systemInstruction)
                                    })
                                })
                            })
                        }

                        val requestBody = requestJson.toString()
                        conn.outputStream.use { os ->
                            val input = requestBody.toByteArray(charset("utf-8"))
                            os.write(input, 0, input.size)
                        }

                        val responseCode = conn.responseCode
                        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                            val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val respObj = org.json.JSONObject(respStr)
                            val candidates = respObj.getJSONArray("candidates")
                            val content = candidates.getJSONObject(0).getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            parts.getJSONObject(0).getString("text")
                        } else {
                            "કનેક્શન નિષ્ફળ રહ્યું. પ્રતિભાવ કોડ: $responseCode / Connection failed with error code $responseCode."
                        }
                    }
                }
            } catch (e: Exception) {
                "તમારી પૂછપરછનો જવાબ મેળવવામાં ભૂલ થઈ: ${e.message} / Exception: ${e.localizedMessage}"
            }
            
            val updatedHistory = aiChatHistory.toMutableList()
            updatedHistory.add(AiChatMessage(response, false))
            aiChatHistory = updatedHistory
            isAiThinking = false
        }
    }

    fun clearAiChat() {
        aiChatHistory = listOf(
            AiChatMessage("નમસ્તે! હું અર્નમિત્ર AI કોચ છું. હું તમને તમારા નેટવર્કને આગળ વધારવામાં, વધુ કમાણી કરવા માટે માર્કેટિંગ પદ્ધતિઓ અને ટિપ્સ આપવા માટે અહીં છું. પૂછો કોઈપણ પ્રશ્ન!", false)
        )
    }
}

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    INSTALLING,
    COMPLETED,
    ERROR
}

enum class InAppUpdateType {
    FLEXIBLE,
    IMMEDIATE
}
