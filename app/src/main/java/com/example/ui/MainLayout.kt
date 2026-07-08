package com.example.ui

import kotlinx.coroutines.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.zIndex
import android.widget.Toast
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val lang = viewModel.selectedLanguage
    val isDark = viewModel.isDarkMode

    val contentColorScheme = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF4CAF50),
            secondary = Color(0xFFFFC107),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFFF57F17),
            background = Color(0xFFF5F5F5),
            surface = Color.White,
            onBackground = Color(0xFF212121),
            onSurface = Color(0xFF212121)
        )
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(context, forceManualCheck = false)
    }

    MaterialTheme(
        colorScheme = contentColorScheme
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            InAppUpdateOverlay(viewModel = viewModel)

            if (viewModel.isVerifyingUtr) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Security,
                                contentDescription = "Security Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "સુરક્ષિત પેમેન્ટ ચકાસણી / Secure Payment Verification",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = viewModel.utrVerificationStep,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "કૃપા કરીને રાહ જુઓ, આ જોડાણ સીધું બેંક અને NPCI યુપીઆઈ ગેટવે સાથે એનક્રિપ્ટ કરેલ છે. કોઈ પણ ખોટી કે ડુપ્લીકેટ યુટીઆર નંબર સ્વીકારવામાં આવશે નહિ. / Please wait, this connection is directly encrypted with bank and NPCI UPI gateways. No fake or duplicate UTRs will be accepted.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    },
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                )
            }

            if (viewModel.showOnboarding) {
                OnboardingScreen(viewModel = viewModel)
            } else {
                Surface(
                    modifier = modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val user = viewModel.currentUser

                    if (viewModel.loggedInUserUid == null || user == null) {
                        // Not logged in
                        AuthScreen(viewModel = viewModel)
                    } else {
                        // Logged in
                        if (!user.isActive) {
                            // Requires payment
                            PendingPaymentScreen(viewModel = viewModel, user = user)
                        } else {
                            // Pin lock check
                            if (user.isSecurityLockEnabled && !viewModel.isAppUnlocked) {
                                PinLockScreen(viewModel = viewModel, user = user)
                            } else {
                                // Fully active and unlocked!
                                MainAppScaffold(viewModel = viewModel, user = user)
                            }
                        }
                    }
                }
            }

            // Top-level floating overlays
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(99f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmsAlertOverlay(viewModel = viewModel)
                HeadsUpNotificationOverlay(viewModel = viewModel)
            }
        }
    }
}

// Authentication Screen (Login & Register)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AppViewModel) {
    val lang = viewModel.selectedLanguage
    var isRegisterTab by remember { mutableStateOf(false) }
    var termsAgreed by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showGoogleSelectorDialog by remember { mutableStateOf(false) }
    var googleSelectIsRegister by remember { mutableStateOf(true) }

    if (showTermsDialog) {
        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
    }

    if (showGoogleSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSelectorDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Google", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("લોગિન / Sign In", fontSize = 14.sp)
                }
            },
            text = {
                Column {
                    Text("ચાલુ રાખવા માટે ગૂગલ એકાઉન્ટ પસંદ કરો / Choose an account to continue to EarnMitra:", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val googleAccounts = listOf(
                        Pair("Mukesh Chaudhary", "chaudharymukesh053@gmail.com"),
                        Pair("EarnMitra Guest", "guest.user@gmail.com")
                    )
                    
                    googleAccounts.forEach { (gName, gEmail) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showGoogleSelectorDialog = false
                                    if (googleSelectIsRegister) {
                                        viewModel.handleGoogleRegister(gName, gEmail)
                                    } else {
                                        viewModel.handleGoogleLogin(gEmail)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(gName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(gName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(gEmail, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    // Custom account entry option
                    var customGName by remember { mutableStateOf("") }
                    var customGEmail by remember { mutableStateOf("") }
                    var showCustomGForm by remember { mutableStateOf(false) }
                    
                    if (!showCustomGForm) {
                        TextButton(onClick = { showCustomGForm = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("બીજું એકાઉન્ટ વાપરો / Use another account", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customGName,
                            onValueChange = { customGName = it },
                            label = { Text("પૂરું નામ / Full Name", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                        OutlinedTextField(
                            value = customGEmail,
                            onValueChange = { customGEmail = it },
                            label = { Text("ગૂગલ ઈમેલ / Google Email", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                        Button(
                            onClick = {
                                if (customGName.isNotBlank() && customGEmail.contains("@")) {
                                    showGoogleSelectorDialog = false
                                    if (googleSelectIsRegister) {
                                        viewModel.handleGoogleRegister(customGName, customGEmail)
                                    } else {
                                        viewModel.handleGoogleLogin(customGEmail)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("આગળ વધો / Continue", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleSelectorDialog = false }) {
                    Text("રદ કરો / Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = Translation.get("app_title", lang),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Quick language switcher in Auth
                    var showLangMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showLangMenu = true }) {
                        Icon(Icons.Default.Language, contentDescription = "Language")
                    }
                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        Language.values().forEach { l ->
                            DropdownMenuItem(
                                text = { Text(l.displayName) },
                                onClick = {
                                    viewModel.setLanguage(l)
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Premium Quick Language Selector Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ભાષા પસંદ કરો / Select Language:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Language.values().forEach { l ->
                            val isSelected = viewModel.selectedLanguage == l
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(l) },
                                label = {
                                    Text(
                                        text = l.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Creative visual art (Canvas) representing connection & growth
            AppLogo(
                modifier = Modifier
                    .size(140.dp)
                    .padding(bottom = 15.dp)
            )

            Text(
                text = Translation.get("tagline", lang),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Switch Tab Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surface),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { isRegisterTab = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isRegisterTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (!isRegisterTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(Translation.get("login_btn", lang), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isRegisterTab = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegisterTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isRegisterTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(Translation.get("register_btn", lang), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Main Forms
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isRegisterTab) {
                        // Login Screen Form
                        Text(
                            text = Translation.get("login_title", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 15.dp)
                        )

                        // Login Auth Method Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val loginMethods = listOf(
                                Triple("WHATSAPP", "🟢 WhatsApp", Icons.Default.Phone),
                                Triple("EMAIL", "📧 Email", Icons.Default.Email),
                                Triple("GOOGLE", "🔴 Google", Icons.Default.AccountCircle),
                                Triple("OTP", "💬 SMS OTP", Icons.Default.Sms)
                            )
                            loginMethods.forEach { (m, label, icon) ->
                                val selected = viewModel.loginAuthMethod == m
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            viewModel.loginAuthMethod = m
                                            viewModel.showOtpEntry = false
                                            viewModel.enteredOtp = ""
                                            viewModel.loginMessage = null
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (viewModel.loginAuthMethod == "WHATSAPP" || viewModel.loginAuthMethod == "OTP") {
                            // Input fields
                            OutlinedTextField(
                                value = viewModel.loginUid,
                                onValueChange = { viewModel.loginUid = it },
                                label = { Text(Translation.get("uid_hint", lang)) },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Interactive option to recover Unique IDs from Phone number!
                            var phoneLookup by remember { mutableStateOf("") }
                            var showLookupDialog by remember { mutableStateOf(false) }

                            TextButton(
                                onClick = { showLookupDialog = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "આઈડી ભૂલી ગયા? ફોનથી શોધો / Forgot ID? Find by Phone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (showLookupDialog) {
                                var lookupCountryCode by remember { mutableStateOf(countryCodes[0]) }
                                AlertDialog(
                                    onDismissRequest = { showLookupDialog = false },
                                    title = { Text("મોબાઇલ નંબરથી આઈડી શોધો", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CountryCodePicker(
                                                    selectedCode = lookupCountryCode,
                                                    onCodeSelected = { lookupCountryCode = it }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                OutlinedTextField(
                                                    value = phoneLookup,
                                                    onValueChange = { phoneLookup = it },
                                                    label = { Text("૧૦ અંકનો ફોન નંબર", fontSize = 11.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = { 
                                                    val raw = phoneLookup.trim()
                                                    val full = if (raw.isNotEmpty() && !raw.startsWith("+")) "${lookupCountryCode.code} $raw" else raw
                                                    viewModel.handleFindAccountsByPhone(full) 
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("શોધો / Search")
                                            }

                                            if (viewModel.matchedAccountsForPhone.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("મળેલા એકાઉન્ટ્સ (લોગીન કરવા માટે ક્લિક કરો):", fontWeight = FontWeight.Bold)
                                                viewModel.matchedAccountsForPhone.forEach { acc ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable {
                                                                viewModel.loginUid = acc.uid
                                                                showLookupDialog = false
                                                            },
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${acc.fullName}\nID: ${acc.uid}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                            Badge(containerColor = if (acc.isActive) Color(0xFF4CAF50) else Color(0xFFFFC107)) {
                                                                Text(if (acc.isActive) "Active" else "Pending")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showLookupDialog = false }) {
                                            Text("બંધ કરો / Close")
                                        }
                                    }
                                )
                            }

                            if (viewModel.showOtpEntry) {
                                // OTP Input Fields
                                OutlinedTextField(
                                    value = viewModel.enteredOtp,
                                    onValueChange = { viewModel.enteredOtp = it },
                                    label = { Text(if (viewModel.loginAuthMethod == "WHATSAPP") "વોટ્સએપ કોડ દાખલ કરો / Enter WhatsApp Code" else Translation.get("otp_hint", lang)) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { viewModel.verifyLoginOtpAndLogin() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(Translation.get("login_btn", lang), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val context = LocalContext.current
                                if (viewModel.loginAuthMethod == "WHATSAPP") {
                                    Button(
                                        onClick = { viewModel.triggerWhatsAppLoginVerification(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp green
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verify via WhatsApp (૧૦૦% ફ્રી)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.triggerLoginOtp() },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(Translation.get("send_otp", lang), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (viewModel.loginAuthMethod == "EMAIL") {
                            // Firebase Email Login form
                            OutlinedTextField(
                                value = viewModel.loginEmail,
                                onValueChange = { viewModel.loginEmail = it },
                                label = { Text("રજીસ્ટર્ડ ઈમેલ / Registered Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            var passwordVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = viewModel.loginPassword,
                                onValueChange = { viewModel.loginPassword = it },
                                label = { Text("પાસવર્ડ / Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.handleFirebaseEmailLogin() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("ઈમેલથી લોગિન કરો / Login with Email", fontWeight = FontWeight.Bold)
                            }
                        } else if (viewModel.loginAuthMethod == "GOOGLE") {
                            // Google Login Button
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { 
                                    googleSelectIsRegister = false
                                    showGoogleSelectorDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)), // Google Red
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Google (૧૦૦% ફ્રી)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // Registration Screen Form
                        var regCountryCode by remember { mutableStateOf(countryCodes[0]) }
                        Text(
                            text = Translation.get("register_title", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Reg Auth Method Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val methods = listOf(
                                Triple("WHATSAPP", "🟢 WhatsApp", Icons.Default.Phone),
                                Triple("EMAIL", "📧 Email", Icons.Default.Email),
                                Triple("GOOGLE", "🔴 Google", Icons.Default.AccountCircle),
                                Triple("OTP", "💬 SMS OTP", Icons.Default.Sms)
                            )
                            methods.forEach { (m, label, icon) ->
                                val selected = viewModel.regAuthMethod == m
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            viewModel.regAuthMethod = m
                                            viewModel.showRegOtpEntry = false
                                            viewModel.regEnteredOtp = ""
                                            viewModel.regMessage = null
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (viewModel.regAuthMethod == "WHATSAPP" || viewModel.regAuthMethod == "OTP") {
                            OutlinedTextField(
                                value = viewModel.regName,
                                onValueChange = { viewModel.regName = it },
                                label = { Text(Translation.get("name_hint", lang)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CountryCodePicker(
                                    selectedCode = regCountryCode,
                                    onCodeSelected = { regCountryCode = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = viewModel.regPhone,
                                    onValueChange = { viewModel.regPhone = it },
                                    label = { Text(Translation.get("phone_hint", lang)) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = viewModel.regReferrer,
                                onValueChange = { viewModel.regReferrer = it },
                                label = { Text(Translation.get("referrer_hint", lang)) },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else if (viewModel.regAuthMethod == "EMAIL") {
                            OutlinedTextField(
                                value = viewModel.regName,
                                onValueChange = { viewModel.regName = it },
                                label = { Text(Translation.get("name_hint", lang)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.regEmail,
                                onValueChange = { viewModel.regEmail = it },
                                label = { Text("ઈમેલ આઈડી / Email ID") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            var passwordVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = viewModel.regPassword,
                                onValueChange = { viewModel.regPassword = it },
                                label = { Text("નવો પાસવર્ડ (૬ અંક) / Password (min 6)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.regReferrer,
                                onValueChange = { viewModel.regReferrer = it },
                                label = { Text(Translation.get("referrer_hint", lang)) },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else if (viewModel.regAuthMethod == "GOOGLE") {
                            OutlinedTextField(
                                value = viewModel.regReferrer,
                                onValueChange = { viewModel.regReferrer = it },
                                label = { Text(Translation.get("referrer_hint", lang)) },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = Translation.get("joining_fee_notice", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Terms & Conditions Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = termsAgreed,
                                onCheckedChange = { termsAgreed = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "હું નિયમો અને શરતો સ્વીકારું છું / I agree to the",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "નિયમો અને શરતો / Terms & Conditions",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { showTermsDialog = true }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (viewModel.showRegOtpEntry && (viewModel.regAuthMethod == "WHATSAPP" || viewModel.regAuthMethod == "OTP")) {
                            OutlinedTextField(
                                value = viewModel.regEnteredOtp,
                                onValueChange = { viewModel.regEnteredOtp = it },
                                label = { Text(if (viewModel.regAuthMethod == "WHATSAPP") "વોટ્સએપ કોડ દાખલ કરો / Enter WhatsApp Code" else "મોબાઈલ ઓટીપી દાખલ કરો / Enter Mobile OTP") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.showRegOtpEntry = false
                                        viewModel.regEnteredOtp = ""
                                        viewModel.regMessage = null
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("પાછા જાઓ / Back")
                                }

                                Button(
                                    onClick = { viewModel.handleRegister() },
                                    modifier = Modifier.weight(1.5f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = termsAgreed
                                ) {
                                    Text("ચકાસો અને રજીસ્ટર / Verify & Register", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            val context = LocalContext.current
                            if (viewModel.regAuthMethod == "WHATSAPP") {
                                Button(
                                    onClick = { 
                                        val rawPhone = viewModel.regPhone.trim()
                                        if (rawPhone.isNotEmpty() && !rawPhone.startsWith("+")) {
                                            viewModel.regPhone = "${regCountryCode.code} $rawPhone"
                                        }
                                        val url = viewModel.triggerWhatsAppRegVerification()
                                        if (url.isNotEmpty()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse(url)
                                                }
                                                context.startActivity(intent)
                                            } catch(e: Exception) {
                                                // Ignore
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = termsAgreed
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verify via WhatsApp (૧૦૦% ફ્રી)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else if (viewModel.regAuthMethod == "EMAIL") {
                                Button(
                                    onClick = { viewModel.handleFirebaseEmailRegister() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = termsAgreed
                                ) {
                                    Text("ઈમેલથી રજીસ્ટર કરો / Register with Email", fontWeight = FontWeight.Bold)
                                }
                            } else if (viewModel.regAuthMethod == "GOOGLE") {
                                Button(
                                    onClick = { 
                                        googleSelectIsRegister = true
                                        showGoogleSelectorDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)), // Google Red
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = termsAgreed
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign Up with Google (૧૦૦% ફ્રી)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        val rawPhone = viewModel.regPhone.trim()
                                        if (rawPhone.isNotEmpty() && !rawPhone.startsWith("+")) {
                                            viewModel.regPhone = "${regCountryCode.code} $rawPhone"
                                        }
                                        viewModel.triggerRegOtp() 
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = termsAgreed
                                ) {
                                    Text("મોબાઈલ ઓટીપી મેળવો / Get Mobile OTP", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // System messages (Alert Box)
                    viewModel.regMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (viewModel.regSuccessUid != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (viewModel.regSuccessUid != null) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (viewModel.regSuccessUid != null) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (viewModel.regSuccessUid != null) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                )
                            }
                        }
                    }

                    viewModel.loginMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                                Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Account Pending Payment Screen (₹1000 joining fee simulation)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPaymentScreen(viewModel: AppViewModel, user: UserAccount) {
    val lang = viewModel.selectedLanguage
    var selectedMethod by remember { mutableStateOf("UPI") }
    var successAlert by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }

    if (showCheckout) {
        UpiCheckoutDialog(
            targetUid = user.uid,
            targetName = user.fullName,
            viewModel = viewModel,
            onActivate = { utr ->
                viewModel.payJoiningFeeViaGateway(
                    method = selectedMethod,
                    utr = utr,
                    onSuccess = {
                        successAlert = true
                    }
                )
                showCheckout = false
            },
            onDismiss = { showCheckout = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("એકાઉન્ટ એક્ટિવેશન / Activation") },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (user.paymentStatus) {
                "PENDING_VERIFICATION" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ચુકવણી ચકાસણી બાકી છે! / Payment Verification Pending!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "તમારી ₹૧,૦૦૦ ની ચુકવણી વિનંતી સબમિટ થઈ ગઈ છે. (UTR: ${user.pendingUtr})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "એડમિન દ્વારા તમારી ચુકવણીની તપાસ અને આઈડી એક્ટિવેશન પ્રક્રિયા ચાલુ છે. કૃપા કરીને થોડી રાહ જુઓ અથવા વધુ માહિતી માટે એડમિનનો સંપર્ક કરો.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Contact Admin via WhatsApp
                            val encodedText = java.net.URLEncoder.encode("નમસ્તે એડમિન, મેં ₹૧,૦૦૦ ની જોડાણ ફી ચૂકવી દીધી છે. મારો UID: ${user.uid} અને UTR: ${user.pendingUtr} છે. કૃપા કરીને મારી આઈડી એક્ટિવેટ કરો. / Hello Admin, I have paid the joining fee of ₹1,000. My UID is ${user.uid} and UTR is ${user.pendingUtr}. Please activate my ID.", "UTF-8")
                            val url = "https://api.whatsapp.com/send?phone=${viewModel.adminWhatsAppNumber.replace("+", "").replace(" ", "")}&text=$encodedText"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            viewModel.getApplication<android.app.Application>().startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp green color
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("એડમિનનો સંપર્ક કરો / Contact Admin", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
                else -> {
                    if (user.paymentStatus == "REJECTED") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "ચુકવણી અસ્વીકાર કરવામાં આવી છે! / Payment Rejected!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "એડમિન દ્વારા તમારી છેલ્લી ₹૧,૦૦૦ ની ચુકવણી (UTR નંબર) ની પુષ્ટિ થઈ શકી નથી. કૃપા કરીને સાચો UTR શોધો અને ફરીથી સબમિટ કરો.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "તમારું આઈડી ${user.uid} હાલમાં સક્રિય નથી. એપ્લિકેશનનો ઉપયોગ કરવા માટે જોડાણ ફી ચૂકવવી ફરજિયાત છે.",
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "જોડાણ ફી: ₹૧,૦૦૦ (નોન-રિફંડપાત્ર)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )

                    Text("પેમેન્ટ ઓપ્શન્સ પસંદ કરો / Select Payment Method:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))

                    // Payment method selector
                    val paymentMethods = listOf("UPI (PhonePe/GPay)", "ડેબિટ/ક્રેડિટ કાર્ડ", "નેટ બેન્કિંગ")
                    paymentMethods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedMethod == method) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selectedMethod == method) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { selectedMethod = method }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(method, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showCheckout = true
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translation.get("pay_fee", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            if (successAlert) {
                AlertDialog(
                    onDismissRequest = { successAlert = false },
                    title = { Text("ચુકવણી સબમિટ થઈ ગઈ છે!") },
                    text = { Text("તમારી ચુકવણી એડમિન સમક્ષ મંજૂરી માટે સબમિટ કરી દેવામાં આવી છે. એડમિન વેરિફિકેશન પૂર્ણ થયા બાદ આઈડી સક્રિય થઈ જશે.") },
                    confirmButton = {
                        TextButton(onClick = { successAlert = false }) {
                            Text("બરાબર / OK")
                        }
                    }
                )
            }
        }
    }
}

// Main Scaffold with navigation tabs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: AppViewModel, user: UserAccount) {
    val lang = viewModel.selectedLanguage
    var currentTab by remember { mutableStateOf("DASHBOARD") }
    var showNotifCenter by remember { mutableStateOf(false) }

    if (showNotifCenter) {
        NotificationCenterDialog(viewModel = viewModel, onDismiss = { showNotifCenter = false })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Dynamic Logo Representation
                        AppLogo(
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Translation.get("app_title", lang),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    val unreadCount = viewModel.currentNotifications.count { !it.isRead }
                    IconButton(onClick = { showNotifCenter = true }) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(28.dp)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color.Red),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadCount",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "DASHBOARD",
                    onClick = { currentTab = "DASHBOARD" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(Translation.get("dashboard", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = currentTab == "WALLET",
                    onClick = { currentTab = "WALLET" },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                    label = { Text(Translation.get("wallet", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = currentTab == "PRODUCTS",
                    onClick = { currentTab = "PRODUCTS" },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Products") },
                    label = { Text(Translation.get("products", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = currentTab == "SETTINGS",
                    onClick = { currentTab = "SETTINGS" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(Translation.get("settings", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                if (user.uid == "EM10000") {
                    NavigationBarItem(
                        selected = currentTab == "ADMIN",
                        onClick = { currentTab = "ADMIN" },
                        icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = "Admin") },
                        label = { Text("એડમિન / Admin", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                "DASHBOARD" -> DashboardTab(viewModel = viewModel, user = user)
                "WALLET" -> WalletTab(viewModel = viewModel, user = user)
                "PRODUCTS" -> ProductsTab(viewModel = viewModel, user = user)
                "SETTINGS" -> SettingsTab(viewModel = viewModel, user = user)
                "ADMIN" -> AdminTab(viewModel = viewModel, user = user)
            }
        }
    }
}

// 1. Dashboard Tab with MLM Level Tree and Calculator
@Composable
fun DashboardTab(viewModel: AppViewModel, user: UserAccount) {
    val lang = viewModel.selectedLanguage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarIcon(user.profileImage, modifier = Modifier.size(54.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "યુનિક આઈડી / Unique ID: ${user.uid}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Badge(
                            containerColor = Color(0xFF4CAF50),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = Translation.get("status_active", lang),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Translation.get("total_earnings", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.2f", user.walletBalance)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // Referral Code box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("કોડ / Code: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(user.uid, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Referral Progress Block (Max 3 direct limit)
        item {
            val context = LocalContext.current
            var showCheckoutDialogForRef by remember { mutableStateOf<UserAccount?>(null) }

            if (showCheckoutDialogForRef != null) {
                UpiCheckoutDialog(
                    targetUid = showCheckoutDialogForRef!!.uid,
                    targetName = showCheckoutDialogForRef!!.fullName,
                    viewModel = viewModel,
                    onActivate = { utr ->
                        viewModel.activateReferralDirectly(showCheckoutDialogForRef!!.uid, utr)
                        showCheckoutDialogForRef = null
                    },
                    onDismiss = { showCheckoutDialogForRef = null }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translation.get("referral_limit_notice", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { user.directReferralsCount / 3f },
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = if (user.directReferralsCount >= 3) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${user.directReferralsCount}/3",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (user.directReferralsCount >= 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🔒 " + Translation.get("referral_system_locked", lang),
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- PREMIUM REFERRAL CARD & GENERATOR SECTION ---
                    val referralLink = if (viewModel.referralWebsiteUrl.endsWith("/")) {
                        "${viewModel.referralWebsiteUrl}?ref=${user.uid}"
                    } else {
                        "${viewModel.referralWebsiteUrl}/?ref=${user.uid}"
                    }

                    if (user.directReferralsCount >= 3) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "શેરિંગ બંધ છે / Sharing Disabled",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "તમે તમારા ૩ ડાયરેક્ટ રેફરલ્સની મર્યાદા પૂરી કરી લીધી છે. તેથી વોટ્સએપ શેર અને ક્યુઆર કોડ કાર્ડ સુરક્ષાના કારણોસર નિષ્ક્રિય કરવામાં આવ્યા છે.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You have reached your limit of 3 direct referrals! Sharing and card generation have been disabled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "મારું ડિજિટલ રેફરલ કાર્ડ / My Referral Card",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Premium Agent Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF1B5E20), Color(0xFF004D40))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "અર્નમિત્ર એજન્ટ કાર્ડ / EARNMITRA AGENT",
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "૧૦૦% પ્રમાણિત વ્યવસાયિક સભ્ય / 100% Certified Member",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 8.sp
                                            )
                                        }
                                        
                                        Badge(containerColor = Color(0xFF4CAF50)) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "નામ / Name:",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = user.fullName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "એજન્ટ આઈડી / Agent ID:",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = user.uid,
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        // Deterministic Dynamic QR code box
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SimulatedQrCode(
                                                uid = user.uid,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // WhatsApp and copy sharing buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val shareText = "નમસ્તે! અર્નમિત્ર ડિજિટલ નેટવર્ક સાથે જોડાઓ અને ઘરે બેઠા દરરોજ આકર્ષક કમિશન કમાવવાનું શરૂ કરો. મારી સાથે સીધા જોડાવા માટે આ લિંક પર ક્લિક કરો અથવા રજીસ્ટ્રેશનમાં મારો એજન્ટ આઈડી [${user.uid}] દાખલ કરો:\n\n$referralLink"
                                        val shareIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                            `package` = "com.whatsapp"
                                        }
                                        try {
                                            context.startActivity(shareIntent)
                                        } catch (e: Exception) {
                                            val chooser = android.content.Intent.createChooser(shareIntent, "Share Referral Code")
                                            context.startActivity(chooser)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.2f),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp શેર", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Referral Link", referralLink)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "લિંક કોપી થઈ ગઈ! / Referral link copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.9f),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("કોપી લિંક", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val shareText = "Earn money by referring members. Sign up using my link: $referralLink"
                                        val shareIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val chooser = android.content.Intent.createChooser(shareIntent, "Share Referral Link")
                                        context.startActivity(chooser)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.9f),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("અન્ય શેર", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- MY DIRECT TEAM SECTION ---
                    Text(
                        text = "તમારી ટીમ / My Direct Recruits (${viewModel.directReferrals.size}/3)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (viewModel.directReferrals.isEmpty()) {
                        Text(
                            text = "હજુ સુધી કોઈ રેફરલ જોડાયેલ નથી. / No direct referrals yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.directReferrals.forEach { child ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = child.fullName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "ID: ${child.uid} | Ph: ${child.phoneNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }

                                        if (child.isActive) {
                                            Badge(containerColor = Color(0xFF4CAF50)) {
                                                Text("સક્રિય / Active", color = Color.White, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    showCheckoutDialogForRef = child
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("એક્ટિવેટ / Activate", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 10-Level Genealogy Team Tree Diagram
        item {
            InteractiveTeamTreeCard(viewModel, user)
        }

        // Interactive Referral Tree Demo Simulator
        item {
            val context = LocalContext.current
            var mockName by remember { mutableStateOf("") }
            var mockPhone by remember { mutableStateOf("") }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "રેફરલ આમંત્રિત કરો (ટેસ્ટિંગ સિમ્યુલેટર) / Invite Referral Simulator",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "તમારા નેટવર્ક ગ્રોથ અને લેવલ કમિશન ચેક કરવા માટે નવું રેફરલ જોડો:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    var mockCountryCode by remember { mutableStateOf(countryCodes[0]) }

                    OutlinedTextField(
                        value = mockName,
                        onValueChange = { mockName = it },
                        label = { Text("રેફરલનું નામ / Referral Name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = viewModel.directReferrals.size < 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountryCodePicker(
                            selectedCode = mockCountryCode,
                            onCodeSelected = { mockCountryCode = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = mockPhone,
                            onValueChange = { mockPhone = it },
                            label = { Text("ફોન નંબર / Referral Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = viewModel.directReferrals.size < 3
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (mockName.isNotBlank() && viewModel.isValidIndianMobileNumber(mockPhone)) {
                                val full = if (!mockPhone.startsWith("+")) "${mockCountryCode.code} $mockPhone" else mockPhone
                                viewModel.addMockReferral(mockName, full)
                                mockName = ""
                                mockPhone = ""
                            } else if (!viewModel.isValidIndianMobileNumber(mockPhone)) {
                                Toast.makeText(context, "મહેરબાની કરીને સાચો ૧૦-અંકનો રેફરલ મોબાઈલ નંબર દાખલ કરો! ખોટા નંબરો માન્ય નથી. / Please enter a valid 10-digit Indian mobile number!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.directReferrals.size < 3 && mockName.isNotBlank() && mockPhone.isNotBlank()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translation.get("add_referral", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 10 levels MLM commission grid
        item {
            Text(
                text = Translation.get("level_earnings", lang),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        items(viewModel.currentMlmStats) { stat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "લેવલ / Level ${stat.level}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "મહત્તમ રેફરલ્સ / Max: ${stat.expectedCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "રેફરલ્સ: ${stat.actualCount}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "કમાણી: ₹${String.format(Locale.getDefault(), "%,.1f", stat.earnedCommission)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Commission Calculator Tool
        item {
            Spacer(modifier = Modifier.height(10.dp))
            CommissionCalculatorComponent(lang = lang)
        }

        // --- TOP EARNERS LEADERBOARD SECTION ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🏆 ટોપ અર્નર્સ લીડરબોર્ડ / Top Earners Leaderboard",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Text(
                        text = "અમારી એપ્લિકેશનના અગ્રણી કમિશન કમાનાર સભ્યો / Top 10 commission earning members of EarnMitra:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Mock top 10 earners
                    val topEarners = listOf(
                        Triple("EM10000", "અમિતભાઈ પટેલ / Amit Patel", 54200.0),
                        Triple("EM10012", "રાજેશભાઈ વ્યાસ / Rajesh Vyas", 48500.0),
                        Triple("EM10045", "દિનેશભાઈ ચૌધરી / Dinesh Chaudhary", 41200.0),
                        Triple("EM10098", "ભાવનાબેન સોલંકી / Bhavna Solanki", 37800.0),
                        Triple("EM10102", "જયેશભાઈ પરમાર / Jayesh Parmar", 32500.0),
                        Triple("EM10001", "મનીષભાઈ શાહ / Manish Shah", 29800.0),
                        Triple("EM10220", "કિરણબેન પ્રજાપતિ / Kiran Prajapati", 24400.0),
                        Triple("EM10002", "જીગ્નેશભાઈ મહેતા / Jignesh Mehta", 18200.0),
                        Triple("EM10188", "પંકજભાઈ જોષી / Pankaj Joshi", 15600.0),
                        Triple("EM10065", "અંજનાબેન દવે / Anjana Dave", 12100.0)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        topEarners.forEachIndexed { index, earner ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (earner.first == user.uid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank icon/badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            when (index) {
                                                0 -> Color(0xFFFFD700) // Gold
                                                1 -> Color(0xFFC0C0C0) // Silver
                                                2 -> Color(0xFFCD7F32) // Bronze
                                                else -> Color.Gray.copy(alpha = 0.15f)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (index < 3) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = earner.second,
                                        fontWeight = if (earner.first == user.uid) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID: ${earner.first}" + (if (earner.first == user.uid) " (તમે / You)" else ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", earner.third)}",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. Wallet & Transactions Tab
@Composable
fun WalletTab(viewModel: AppViewModel, user: UserAccount) {
    val lang = viewModel.selectedLanguage
    var withdrawAmount by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }
    var isUpiWithdraw by remember { mutableStateOf(true) }

    // Merchant local transactions states
    var merchantName by remember { mutableStateOf("") }
    var merchantAmount by remember { mutableStateOf("") }
    var merchantUpiId by remember { mutableStateOf("") }
    var showQrScannerSim by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Helper to parse scanned/uploaded UPI string
    fun parseUpiQrCode(scannedText: String): Triple<String, String, String> {
        var upiId = ""
        var name = ""
        var amount = ""
        
        if (scannedText.startsWith("upi://pay?", ignoreCase = true)) {
            val query = scannedText.substringAfter("upi://pay?")
            val params = query.split("&")
            for (param in params) {
                val parts = param.split("=")
                if (parts.size == 2) {
                    val key = parts[0].lowercase()
                    val value = try {
                        java.net.URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        parts[1]
                    }
                    when (key) {
                        "pa" -> upiId = value
                        "pn" -> name = value
                        "am" -> amount = value
                    }
                }
            }
        } else {
            // Fallback for raw UPI ID or plain text
            if (scannedText.contains("@")) {
                upiId = scannedText.trim()
                val left = scannedText.substringBefore("@").replace(".", " ")
                name = left.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else {
                name = scannedText.trim()
            }
        }
        return Triple(name, upiId, amount)
    }

    // Google Code Scanner (Live Camera)
    val gmsScanner = remember {
        try {
            com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
        } catch (e: Throwable) {
            android.util.Log.e("Scanner", "Failed to initialize GmsBarcodeScanning safely", e)
            null
        }
    }

    // Gallery QR Code Reader
    val qrGalleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
                val localScanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                localScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val rawValue = barcodes[0].rawValue ?: ""
                            if (rawValue.isNotBlank()) {
                                val (parsedName, parsedUpi, parsedAmount) = parseUpiQrCode(rawValue)
                                if (parsedName.isNotBlank()) merchantName = parsedName
                                if (parsedUpi.isNotBlank()) merchantUpiId = parsedUpi
                                if (parsedAmount.isNotBlank()) merchantAmount = parsedAmount
                                Toast.makeText(context, "ગેલેરી ક્યુઆર સ્કેન સફળ! / Gallery QR Scan Success!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "આ ઈમેજમાં કોઈ QR કોડ મળ્યો નથી. / No QR code found in this image.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "સ્કેન નિષ્ફળ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "ઈમેજ લોડ કરવામાં ભૂલ આવી: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "ઉપલબ્ધ વોલેટ બેલેન્સ / Available Wallet Balance",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", user.walletBalance)}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "તમામ કમિશન અને કમાણી અહીં જમા થશે. તમે તેને સીધા બેંક અથવા UPI માં ટ્રાન્સફર કરી શકો છો.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Advanced Interactive Earnings Chart
        item {
            EarningAnalyticsChartCard(viewModel)
        }

        // Withdrawal Request Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💸 કમિશન ઉપાડો / Withdraw Commission",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val isKycComplete = user.aadharNumber.length == 12 && user.panNumber.length == 10

                    if (!isKycComplete) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "કેવાયસી અપૂર્ણ છે! / KYC is Pending!",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "કૃપા કરીને પહેલા સેટિંગ્સ ટેબમાં જઈ તમારો આધાર અને પાનકાર્ડ નંબર ઉમેરો.",
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        "Please enter your Aadhaar and PAN in Settings tab to enable withdrawal.",
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Button(
                            onClick = { isUpiWithdraw = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUpiWithdraw) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.15f),
                                contentColor = if (isUpiWithdraw) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = isKycComplete
                        ) {
                            Text(Translation.get("withdraw_upi", lang), fontSize = 11.sp)
                        }

                        Button(
                            onClick = { isUpiWithdraw = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isUpiWithdraw) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.15f),
                                contentColor = if (!isUpiWithdraw) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = isKycComplete
                        ) {
                            Text(Translation.get("withdraw_bank", lang), fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text(Translation.get("wallet_withdraw_amount", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isKycComplete
                    )

                    val amtDouble = withdrawAmount.toDoubleOrNull() ?: 0.0
                    if (amtDouble > 0) {
                        val gstVal = amtDouble * 0.22
                        val netPayout = amtDouble * 0.78
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("કુલ ઉપાડ રકમ / Withdrawal Amount:", fontSize = 11.sp, color = Color.Gray)
                                    Text("₹${String.format("%.2f", amtDouble)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("૨૨% સરકારી જીએસટી / 22% Govt GST:", fontSize = 11.sp, color = Color(0xFFC62828))
                                    Text("- ₹${String.format("%.2f", gstVal)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("તમારા ખાતામાં જમા થશે / Net Payout:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text("₹${String.format("%.2f", netPayout)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }

                    if (isUpiWithdraw) {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI આઈડી દાખલ કરો (દા.ત. name@ybl)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isKycComplete
                        )
                    } else {
                        OutlinedTextField(
                            value = bankAccount,
                            onValueChange = { bankAccount = it },
                            label = { Text("બેંક ખાતા નંબર / Bank Account Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isKycComplete
                        )

                        OutlinedTextField(
                            value = bankIfsc,
                            onValueChange = { bankIfsc = it },
                            label = { Text("IFSC કોડ / IFSC Code") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isKycComplete
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && amt <= user.walletBalance) {
                                viewModel.requestWithdrawal(amt, isUpiWithdraw)
                                withdrawAmount = ""
                                upiId = ""
                                bankAccount = ""
                                bankIfsc = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isKycComplete && (withdrawAmount.toDoubleOrNull() ?: 0.0) > 0.0 && (withdrawAmount.toDoubleOrNull() ?: 0.0) <= user.walletBalance
                    ) {
                        Text("ટ્રાન્સફર કન્ફર્મ કરો / Confirm Withdrawal")
                    }
                }
            }
        }

        // PhonePe-like local merchant purchase using commission directly
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛍️ " + Translation.get("phonepe_local", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "તમારા લોકલ કરિયાણા કે અન્ય જરૂરી સામાન ખરીદવા માટે કમિશનનો સીધો ઉપયોગ કરો અથવા QR કોડ સ્કેન કરો:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (showQrScannerSim) {
                        // Simulated QR Scanner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Scanning viewport box
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                                    modifier = Modifier.size(120.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {}

                                // Green scanner horizontal line simulation
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(2.dp)
                                        .background(Color(0xFF4CAF50))
                                )

                                // Recording label
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "SCANNING...",
                                        color = Color.Red,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    "ક્યુઆર કોડ કેમેરા સામે રાખો / Position QR in frame",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "નજીકના ઉપલબ્ધ મર્ચન્ટ ક્યુઆર (પસંદ કરવા માટે ક્લિક કરો):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 3 Preset QRs for scanning simulation
                        val mockMerchants = listOf(
                            Triple("શ્રીજી જનરલ સ્ટોર / Shreeji General", "shreeji@paytm", "Shreeji General Store"),
                            Triple("કૃષ્ણ ડેરી / Krishna Dairy", "krishnadairy@ybl", "Krishna Dairy"),
                            Triple("પટેલ સ્વીટ્સ / Patel Sweets", "patelsweets@icici", "Patel Sweets")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mockMerchants.forEach { (display, upi, enName) ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            merchantName = display
                                            merchantUpiId = upi
                                            showQrScannerSim = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = display.split(" / ").first(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = upi,
                                            fontSize = 8.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Real Camera Scanner
                            Button(
                                onClick = {
                                    try {
                                        val scanner = gmsScanner
                                        if (scanner != null) {
                                            scanner.startScan()
                                                .addOnSuccessListener { barcode ->
                                                    val rawValue = barcode.rawValue ?: ""
                                                    if (rawValue.isNotBlank()) {
                                                        val (parsedName, parsedUpi, parsedAmount) = parseUpiQrCode(rawValue)
                                                        if (parsedName.isNotBlank()) merchantName = parsedName
                                                        if (parsedUpi.isNotBlank()) merchantUpiId = parsedUpi
                                                        if (parsedAmount.isNotBlank()) merchantAmount = parsedAmount
                                                        showQrScannerSim = false
                                                        Toast.makeText(context, "ક્યુઆર કોડ સ્કેન સફળ! / Scan Success!", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "સ્કેન અસફળ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                        } else {
                                            Toast.makeText(context, "કેમેરા સ્કેનર આ ઉપકરણ પર ઉપલબ્ધ નથી! / Camera Scanner not available on this device!", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "કેમેરા સ્કેનર શરૂ થઈ શક્યું નથી: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                    )
                                    Text("કેમેરા સ્કેન / Camera Scan", fontSize = 11.sp, maxLines = 1)
                                }
                            }

                            // 2. Gallery Scanner
                            Button(
                                onClick = {
                                    try {
                                        qrGalleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ગેલેરી ઓપન કરવામાં સમસ્યા આવી", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                    )
                                    Text("ગેલેરી QR / Gallery QR", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Toggle Demo Simulator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showQrScannerSim = !showQrScannerSim }
                            ) {
                                Icon(
                                    if (showQrScannerSim) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text(
                                    if (showQrScannerSim) "ડેમો બંધ કરો / Hide Demo Scanner" else "ડેમો સ્કેનર બતાવો / Show Demo Scanner",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val scanner = gmsScanner
                                        if (scanner != null) {
                                            scanner.startScan()
                                                .addOnSuccessListener { barcode ->
                                                    val rawValue = barcode.rawValue ?: ""
                                                    if (rawValue.isNotBlank()) {
                                                        val (parsedName, parsedUpi, parsedAmount) = parseUpiQrCode(rawValue)
                                                        if (parsedName.isNotBlank()) merchantName = parsedName
                                                        if (parsedUpi.isNotBlank()) merchantUpiId = parsedUpi
                                                        if (parsedAmount.isNotBlank()) merchantAmount = parsedAmount
                                                        showQrScannerSim = false
                                                        Toast.makeText(context, "ક્યુઆર કોડ સ્કેન સફળ! / Scan Success!", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context, "સ્કેન અસફળ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                        } else {
                                            Toast.makeText(context, "કેમેરા સ્કેનર આ ઉપકરણ પર ઉપલબ્ધ નથી! / Camera Scanner not available on this device!", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "કેમેરા સ્કેનર શરૂ થઈ શક્યું નથી: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                    )
                                    Text("કેમેરા સ્કેન / Camera Scan", fontSize = 11.sp, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        qrGalleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ગેલેરી ઓપન કરવામાં સમસ્યા આવી", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                    )
                                    Text("ગેલેરી QR / Gallery QR", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showQrScannerSim = !showQrScannerSim }
                            ) {
                                Icon(
                                    if (showQrScannerSim) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text(
                                    if (showQrScannerSim) "ડેમો બંધ કરો / Hide Demo Scanner" else "ડેમો સ્કેનર બતાવો / Show Demo Scanner",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        label = { Text("દુકાનદાર / મર્ચન્ટ નું નામ (e.g. Ram Grocery)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = merchantUpiId,
                        onValueChange = { merchantUpiId = it },
                        label = { Text("મર્ચન્ટ યુપીઆઈડી / Merchant UPI ID (Optional)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = merchantAmount,
                        onValueChange = { merchantAmount = it },
                        label = { Text("ચૂકવવાની રકમ / Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val mAmountDouble = merchantAmount.toDoubleOrNull() ?: 0.0
                    val totalMerchantDeduct = mAmountDouble * 1.22
                    if (mAmountDouble > 0) {
                        val gstVal = mAmountDouble * 0.22
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ચૂકવવાની રકમ / Merchant Payout:", fontSize = 11.sp, color = Color.Gray)
                                    Text("₹${String.format("%.2f", mAmountDouble)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("૨૨% સરકારી જીએસટી / 22% Govt GST:", fontSize = 11.sp, color = Color(0xFFC62828))
                                    Text("+ ₹${String.format("%.2f", gstVal)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("કુલ વોલેટમાંથી કપાશે / Total Debit:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("₹${String.format("%.2f", totalMerchantDeduct)}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                if (totalMerchantDeduct > user.walletBalance) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "અપૂરતું બેલેન્સ (જીએસટી સાથે)! / Insufficient balance (including GST)!",
                                        color = Color(0xFFC62828),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val amt = merchantAmount.toDoubleOrNull() ?: 0.0
                            if (merchantName.isNotBlank() && amt > 0 && totalMerchantDeduct <= user.walletBalance) {
                                viewModel.payLocalMerchant(merchantName, amt, merchantUpiId)
                                merchantName = ""
                                merchantAmount = ""
                                merchantUpiId = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = merchantName.isNotBlank() && mAmountDouble > 0.0 && totalMerchantDeduct <= user.walletBalance
                    ) {
                        Text("સીધા ચૂકવો / Direct Pay (PhonePe Style)")
                    }
                }
            }
        }

        // Transactions list
        item {
            Text(
                text = "લેવડ-દેવડનો ઇતિહાસ / Transaction History",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (viewModel.currentTransactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        "હજુ સુધી કોઈ ટ્રાન્ઝેક્શન થયેલ નથી / No transaction history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(viewModel.currentTransactions) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.description,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                            Text(
                                text = "$dateStr | ${tx.type}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        val isPositive = tx.type == "DEPOSIT" || tx.type == "COMMISSION"
                        val amountText = if (isPositive) "+ ₹${tx.amount}" else "- ₹${tx.amount}"
                        val amountColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)

                        Text(
                            text = amountText,
                            fontWeight = FontWeight.ExtraBold,
                            color = amountColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

// 3. Products Promotion & Shopping Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsTab(viewModel: AppViewModel, user: UserAccount) {
    val context = LocalContext.current
    val lang = viewModel.selectedLanguage
    var selectedCategory by remember { mutableStateOf("ALL") }
    var activeSubTab by remember { mutableStateOf(0) } // 0 for Products, 1 for Order History
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showUpiPaymentDetailsDialog by remember { mutableStateOf(false) }
    var orderUtrInput by remember { mutableStateOf("") }
    var orderUtrError by remember { mutableStateOf<String?>(null) }
    var selectedOrderUpiId by remember { mutableStateOf("") }
    var selectedOrderApp by remember { mutableStateOf("PhonePe") }
    var searchQuery by remember { mutableStateOf("") }
    var shippingCountryCode by remember { mutableStateOf(countryCodes[0]) }
    var showCodPaymentDialogForOrder by remember { mutableStateOf<Order?>(null) }
    var viewingProductDetails by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sub-tabs for Product List vs. Order History
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("🛍️ પ્રોડક્ટ લિસ્ટ / Shop", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("📦 મારા ઓર્ડર / My Orders", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (activeSubTab == 0) {
            // PRODUCTS TAB
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("પ્રોડક્ટ શોધો... / Search products...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category horizontally scrollable filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf("ALL", "GROCERY", "CLOTHING", "ELECTRONICS")
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val label = when (cat) {
                        "ALL" -> "બધા / All"
                        "GROCERY" -> "કરિયાણું / Grocery"
                        "CLOTHING" -> "કપડાં / Clothing"
                        "ELECTRONICS" -> "ઇલેક્ટ્રોનિક્સ / Electronics"
                        else -> cat
                    }
                    val icon = when (cat) {
                        "ALL" -> Icons.Default.Apps
                        "GROCERY" -> Icons.Default.ShoppingCart
                        "CLOTHING" -> Icons.Default.Checkroom
                        "ELECTRONICS" -> Icons.Default.Devices
                        else -> Icons.Default.ShoppingCart
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            val filteredProducts = viewModel.productsList.filter {
                (selectedCategory == "ALL" || it.category == selectedCategory) &&
                (searchQuery.isBlank() || 
                 it.nameEn.contains(searchQuery, ignoreCase = true) || 
                 it.nameGu.contains(searchQuery, ignoreCase = true) || 
                 it.descriptionEn.contains(searchQuery, ignoreCase = true) || 
                 it.descriptionGu.contains(searchQuery, ignoreCase = true))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts) { product ->
                    val name = if (lang == Language.GUJARATI) product.nameGu else product.nameEn
                    val desc = if (lang == Language.GUJARATI) product.descriptionGu else product.descriptionEn
                    val qtyInCart = viewModel.cartItems[product.id] ?: 0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingProductDetails = product }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product Image / colored box with icon
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (product.imageUrl.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = name,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val emoji = when (product.category) {
                                        "GROCERY" -> "🌾"
                                        "CLOTHING" -> "👕"
                                        "ELECTRONICS" -> "⚡"
                                        else -> "🛍️"
                                    }
                                    Text(emoji, fontSize = 28.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹${product.price}",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    if (product.isOutOfStock || product.stockCount <= 0) {
                                        Text(
                                            text = "આઉટ ઓફ સ્ટોક / Out Of Stock",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = "સ્ટોક / Stock: ${product.stockCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (product.stockCount <= 3) Color(0xFFE65100) else Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Cart Controls
                            if (product.isOutOfStock || product.stockCount <= 0) {
                                Button(
                                    onClick = { /* Disabled */ },
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.LightGray,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("સ્ટોક નથી / Sold Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (qtyInCart == 0) {
                                Button(
                                    onClick = { viewModel.updateCartQuantity(product.id, 1) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("ઉમેરો / Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(product.id, qtyInCart - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = qtyInCart.toString(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )

                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(product.id, qtyInCart + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (viewModel.cartItems.isNotEmpty()) {
                val totalCartPrice = viewModel.productsList.sumOf { p -> (viewModel.cartItems[p.id] ?: 0) * p.price }
                Button(
                    onClick = { showCheckoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ચેકઆઉટ અને ઓર્ડર કરો / Checkout (₹${totalCartPrice})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // ORDER HISTORY SUBTAB
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.userOrders.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "હજુ સુધી કોઈ ઓર્ડર નથી / No orders placed yet",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(viewModel.userOrders) { order ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = if (order.status == "DELIVERED") Color(0xFF2E7D32) else Color(0xFFE65100),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "ઓર્ડર આઈડી / Order ID: #${order.id}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    // Order Status Badge
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (order.status == "DELIVERED") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (order.status == "DELIVERED") Icons.Default.CheckCircle else Icons.Default.Pending,
                                                contentDescription = null,
                                                tint = if (order.status == "DELIVERED") Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (order.status == "DELIVERED") "ડિલિવર / Delivered" else "પેન્ડિંગ / Pending",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (order.status == "DELIVERED") Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = order.productNames,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "કુલ કિંમત / Total Paid: ₹${String.format("%.2f", order.totalPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "સરનામું / Shipping to: ${order.shippingAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OrderTrackingStepper(order = order)
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ચૂકવણી પ્રકાર / Payment: ${if (order.paymentMethod == "COD") "કેશ ઓન ડિલિવરી (COD)" else order.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = when (order.paymentStatus) {
                                                "PAID" -> Color(0xFFE8F5E9)
                                                "PENDING_VERIFICATION" -> Color(0xFFFFF3E0)
                                                else -> Color(0xFFFFEBEE)
                                            }
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when (order.paymentStatus) {
                                                "PAID" -> "ચૂકવેલ / PAID"
                                                "PENDING_VERIFICATION" -> "વેરિફિકેશન બાકી / VERIFICATION PENDING"
                                                else -> "બાકી ચૂકવણી / PENDING"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (order.paymentStatus) {
                                                "PAID" -> Color(0xFF2E7D32)
                                                "PENDING_VERIFICATION" -> Color(0xFFEF6C00)
                                                else -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (order.paymentRef.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(UTR: ${order.paymentRef})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Mark as delivered button
                                if (order.status == "PENDING") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (order.paymentMethod == "COD" && order.paymentStatus == "PENDING") {
                                                showCodPaymentDialogForOrder = order
                                            } else {
                                                viewModel.markOrderAsDelivered(order)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (order.paymentMethod == "COD" && order.paymentStatus == "PENDING") 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                Color(0xFF2E7D32)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (order.paymentMethod == "COD" && order.paymentStatus == "PENDING") 
                                                Icons.Default.Payment 
                                            else 
                                                Icons.Default.Check, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (order.paymentMethod == "COD" && order.paymentStatus == "PENDING") 
                                                "ચૂકવણી કરો અને મેળવો / Pay & Confirm Delivery" 
                                            else 
                                                "ઓર્ડર મળ્યો / Mark as Delivered", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (viewingProductDetails != null) {
            val product = viewingProductDetails!!
            val name = if (lang == Language.GUJARATI) product.nameGu else product.nameEn
            val desc = if (lang == Language.GUJARATI) product.descriptionGu else product.descriptionEn
            val qtyInCart = viewModel.cartItems[product.id] ?: 0

            AlertDialog(
                onDismissRequest = { viewingProductDetails = null },
                title = null,
                confirmButton = {
                    Button(
                        onClick = { viewingProductDetails = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("બંધ કરો / Close")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Large Image / colored box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val emoji = when (product.category) {
                                    "GROCERY" -> "🌾"
                                    "CLOTHING" -> "👕"
                                    "ELECTRONICS" -> "⚡"
                                    else -> "🛍️"
                                }
                                Text(emoji, fontSize = 64.sp)
                            }
                        }

                        // Product Category Badge
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Title
                        Text(
                            text = name,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Price & Stock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${product.price}",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (product.isOutOfStock || product.stockCount <= 0) {
                                Text(
                                    text = "આઉટ ઓફ સ્ટોક / Out Of Stock",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = "સ્ટોક: ${product.stockCount} / Stock: ${product.stockCount}",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Description
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add to Cart Control panel inside detail view
                        if (!product.isOutOfStock && product.stockCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (qtyInCart > 0) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(product.id, qtyInCart - 1) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(36.dp)
                                    ) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    
                                    Text(
                                        text = "$qtyInCart",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            if (qtyInCart < product.stockCount) {
                                                viewModel.updateCartQuantity(product.id, qtyInCart + 1)
                                            } else {
                                                Toast.makeText(context, "મર્યાદિત સ્ટોક! / Out of stock limit!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(36.dp)
                                    ) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.updateCartQuantity(product.id, 1) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("કાર્ટમાં ઉમેરો / Add to Cart", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // --- REAL CHECKOUT DIALOG PLACED OUTSIDE THE SCROLLABLE TABS ---
        if (showCheckoutDialog) {
            var totalCartPriceCalculated = 0.0
            viewModel.cartItems.forEach { (productId, qty) ->
                val prod = viewModel.productsList.find { it.id == productId }
                if (prod != null) {
                    totalCartPriceCalculated += prod.price * qty
                }
            }
            val gstCalculated = totalCartPriceCalculated * 0.22
            val finalPriceCalculated = totalCartPriceCalculated + gstCalculated

            AlertDialog(
                onDismissRequest = { showCheckoutDialog = false },
                title = {
                    Text(
                        "📦 ચેકઆઉટ અને વિતરણ સરનામું / Checkout",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val rawPhone = viewModel.shippingPhone.trim()
                            if (rawPhone.isNotEmpty() && !rawPhone.startsWith("+")) {
                                viewModel.shippingPhone = "${shippingCountryCode.code} $rawPhone"
                            }
                            if (viewModel.selectedPaymentMethod == "UPI") {
                                if (viewModel.shippingName.isBlank() || viewModel.shippingPhone.isBlank() || viewModel.shippingAddressLines.isBlank() || viewModel.shippingCity.isBlank() || viewModel.shippingPincode.isBlank()) {
                                    android.widget.Toast.makeText(context, "કૃપા કરીને સંપૂર્ણ વિતરણ સરનામું દાખલ કરો! / Please enter complete shipping address!", android.widget.Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                selectedOrderUpiId = viewModel.customRegistrationUpiIds.firstOrNull() ?: "earnmitra@ybl"
                                orderUtrInput = ""
                                orderUtrError = null
                                selectedOrderApp = "PhonePe"
                                showCheckoutDialog = false
                                showUpiPaymentDetailsDialog = true
                            } else {
                                viewModel.checkoutCart()
                                showCheckoutDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ઓર્ડર કન્ફર્મ કરો / Confirm Order")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCheckoutDialog = false }) {
                        Text("બંધ કરો / Close")
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "તમારા કાર્ટની વિગતો / Order Summary:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            viewModel.cartItems.forEach { (productId, qty) ->
                                val prod = viewModel.productsList.find { it.id == productId }
                                if (prod != null) {
                                    val name = if (lang == Language.GUJARATI) prod.nameGu else prod.nameEn
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${name} (x${qty})", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text("₹${prod.price * qty}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("પેટા સરવાળો / Subtotal:", fontSize = 11.sp)
                                Text("₹${String.format("%.2f", totalCartPriceCalculated)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("૨૨% જીએસટી / 22% GST:", fontSize = 11.sp, color = Color(0xFFC62828))
                                Text("₹${String.format("%.2f", gstCalculated)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("કુલ ચૂકવણી / Grand Total:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%.2f", finalPriceCalculated)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                "વિતરણ વિગતો / Delivery Address Details:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = viewModel.shippingName,
                                onValueChange = { viewModel.shippingName = it },
                                label = { Text("પૂરું નામ / Full Name", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CountryCodePicker(
                                    selectedCode = shippingCountryCode,
                                    onCodeSelected = { shippingCountryCode = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = viewModel.shippingPhone,
                                    onValueChange = { viewModel.shippingPhone = it },
                                    label = { Text("મોબાઇલ નંબર / Mobile Number", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = viewModel.shippingAddressLines,
                                onValueChange = { viewModel.shippingAddressLines = it },
                                label = { Text("ઘર નંબર, સોસાયટી, વિસ્તાર / Address details", fontSize = 11.sp) },
                                singleLine = false,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = viewModel.shippingCity,
                                onValueChange = { viewModel.shippingCity = it },
                                label = { Text("ગામ/શહેર / Town/City", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = viewModel.shippingPincode,
                                onValueChange = { viewModel.shippingPincode = it },
                                label = { Text("પિનકોડ / Pincode", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "ચુકવણી પદ્ધતિ પસંદ કરો / Choose Payment Option:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // UPI option
                                Card(
                                    onClick = { viewModel.selectedPaymentMethod = "UPI" },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (viewModel.selectedPaymentMethod == "UPI") 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = if (viewModel.selectedPaymentMethod == "UPI") 
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                    else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = viewModel.selectedPaymentMethod == "UPI",
                                            onClick = { viewModel.selectedPaymentMethod = "UPI" }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("ઓનલાઇન યુપીઆઈ / UPI (GPay/PhonePe)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("ઇન્સ્ટન્ટ સુરક્ષિત ચુકવણી / Instant Secure Payment", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                // Wallet option
                                val isWalletOk = user.walletBalance >= finalPriceCalculated
                                Card(
                                    onClick = { if (isWalletOk) viewModel.selectedPaymentMethod = "WALLET" },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (viewModel.selectedPaymentMethod == "WALLET") 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = if (viewModel.selectedPaymentMethod == "WALLET") 
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                    else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = isWalletOk
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = viewModel.selectedPaymentMethod == "WALLET",
                                            onClick = { if (isWalletOk) viewModel.selectedPaymentMethod = "WALLET" },
                                            enabled = isWalletOk
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("EarnMitra વોલેટ બેલેન્સ / Wallet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("ઉપલબ્ધ બેલેન્સ / Balance: ₹${String.format("%.2f", user.walletBalance)}", fontSize = 10.sp, 
                                                color = if (isWalletOk) Color.Gray else Color(0xFFC62828),
                                                fontWeight = if (isWalletOk) FontWeight.Normal else FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // COD option
                                Card(
                                    onClick = { viewModel.selectedPaymentMethod = "COD" },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (viewModel.selectedPaymentMethod == "COD") 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = if (viewModel.selectedPaymentMethod == "COD") 
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                    else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = viewModel.selectedPaymentMethod == "COD",
                                            onClick = { viewModel.selectedPaymentMethod = "COD" }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("કેશ ઓન ડિલિવરી / Cash on Delivery (COD)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("પાર્સલ મળે ત્યારે રોકડા આપો / Pay when parcel delivered", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                        )
        }

        if (showUpiPaymentDetailsDialog) {
            val totalCartPriceCalculated = viewModel.cartItems.entries.sumOf { (productId, qty) ->
                (viewModel.productsList.find { it.id == productId }?.price ?: 0.0) * qty
            }
            val gstCalculated = totalCartPriceCalculated * 0.22
            val finalPriceCalculated = totalCartPriceCalculated + gstCalculated

            AlertDialog(
                onDismissRequest = { showUpiPaymentDetailsDialog = false },
                title = {
                    Text(
                        "🔒 સુરક્ષિત UPI પેમેન્ટ / Secure UPI Payment",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmedUtr = orderUtrInput.trim()
                            if (trimmedUtr.length != 12 || !trimmedUtr.all { it.isDigit() }) {
                                orderUtrError = "મહેરબાની કરીને સાચો ૧૨-અંકનો UTR નંબર દાખલ કરો / Enter valid 12-digit UTR number"
                                return@Button
                            }
                            if (!trimmedUtr.startsWith("4") && !trimmedUtr.startsWith("5") && !trimmedUtr.startsWith("6")) {
                                orderUtrError = "અમાન્ય UTR! યુટીઆર નંબર 4, 5 કે 6 થી શરૂ થવો જોઈએ. / Invalid UTR format!"
                                return@Button
                            }
                            val sameDigits = trimmedUtr.all { it == trimmedUtr[0] }
                            val sequentialDigits = "0123456789012345".contains(trimmedUtr) || "9876543210987654".contains(trimmedUtr)
                            if (sameDigits || sequentialDigits) {
                                orderUtrError = "ખોટો UTR નંબર! પુનરાવર્તિત અંકો માન્ય નથી. / Fake UTR! Repeated digits are not allowed."
                                return@Button
                            }
                            
                            orderUtrError = null
                            viewModel.checkoutCart(trimmedUtr)
                            showUpiPaymentDetailsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("યુટીઆર સબમિટ કરો / Submit UTR", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showUpiPaymentDetailsDialog = false
                        showCheckoutDialog = true // Go back to checkout details
                    }) {
                        Text("પાછા જાઓ / Go Back")
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ચૂકવવાની રકમ / Amount to Pay:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("₹${String.format("%.2f", finalPriceCalculated)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Text("(કાર્ટ સબટોટલ + ૨૨% જીએસટી સહિત / Including GST)", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        item {
                            Text(
                                text = "૧. પેમેન્ટ મેળવવા માટે UPI ID પસંદ કરો: / Select Payment Gateway:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            viewModel.customRegistrationUpiIds.forEach { upi ->
                                val isSelected = selectedOrderUpiId == upi
                                Card(
                                    onClick = { selectedOrderUpiId = upi },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = isSelected, onClick = { selectedOrderUpiId = upi })
                                        Text(upi, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "૨. ચૂકવણી કરવા માટે તમારી યુપીઆઈ એપ પસંદ કરો: / Launch UPI App:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("PhonePe", "GooglePay", "Paytm").forEach { app ->
                                    val isSel = selectedOrderApp == app
                                    Button(
                                        onClick = { 
                                            selectedOrderApp = app
                                            try {
                                                val upiUri = "upi://pay?pa=$selectedOrderUpiId&pn=EarnMitra&am=${String.format("%.2f", finalPriceCalculated)}&cu=INR&tn=EM-Order-${user.uid}"
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    data = android.net.Uri.parse(upiUri)
                                                    when (app) {
                                                        "PhonePe" -> setPackage("com.phonepe.app")
                                                        "GooglePay" -> setPackage("com.google.android.apps.nbu.paisa.user")
                                                        "Paytm" -> setPackage("net.one97.paytm")
                                                    }
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    val upiUri = "upi://pay?pa=$selectedOrderUpiId&pn=EarnMitra&am=${String.format("%.2f", finalPriceCalculated)}&cu=INR&tn=EM-Order-${user.uid}"
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(upiUri))
                                                    context.startActivity(intent)
                                                } catch (ex: Exception) {
                                                    android.widget.Toast.makeText(context, "તમારા ફોનમાં કોઈ UPI એપ ઉપલબ્ધ નથી! / No UPI App found on this device.", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text(app, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "૩. પેમેન્ટ પછી ટ્રાન્ઝેક્શન વિગતોમાંથી ૧૨-અંકનો UTR/Ref નંબર કોપી કરો અને નીચે દાખલ કરો: / Copy & paste the 12-digit UTR/Ref number below:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            OutlinedTextField(
                                value = orderUtrInput,
                                onValueChange = { 
                                    if (it.all { char -> char.isDigit() } && it.length <= 12) {
                                        orderUtrInput = it
                                        orderUtrError = null
                                    }
                                },
                                label = { Text("૧૨-અંકનો UTR નંબર / 12-Digit UTR Number") },
                                isError = orderUtrError != null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (orderUtrError != null) {
                                Text(
                                    text = orderUtrError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ અસ્વીકરણ: કોઈપણ ખોટી કે નકલી UTR નંબર સબમિટ કરવાથી તમારું એકાઉન્ટ બ્લોક થઈ શકે છે. / Disclaimer: Submitting fake UTRs can lead to permanent account suspension.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            )
        }

        if (showCodPaymentDialogForOrder != null) {
            val orderToPay = showCodPaymentDialogForOrder!!
            var selectedPaymentOption by remember { mutableStateOf("UPI") } // "UPI", "WALLET", "CASH"
            val isWalletBalanceSufficient = user.walletBalance >= orderToPay.totalPrice

            AlertDialog(
                onDismissRequest = { showCodPaymentDialogForOrder = null },
                title = {
                    Text(
                        text = "💳 કેશ ઓન ડિલિવરી ચૂકવણી / COD Payment Collection",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedPaymentOption == "WALLET" && !isWalletBalanceSufficient) {
                                Toast.makeText(context, "વોલેટમાં અપૂરતું બેલેન્સ છે! / Insufficient Wallet Balance!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.collectCodPayment(
                                    order = orderToPay,
                                    method = selectedPaymentOption,
                                    onSuccess = {
                                        showCodPaymentDialogForOrder = null
                                        Toast.makeText(context, "ચૂકવણી સફળ! ઓર્ડર ડિલિવર થયો છે. / Payment successful! Order delivered.", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("ચૂકવણી કન્ફર્મ કરો / Confirm Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCodPaymentDialogForOrder = null }) {
                        Text("બંધ કરો / Cancel")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "આ ઓર્ડર કેશ ઓન ડિલિવરી (COD) પર રાખેલો હતો. કૃપા કરીને વિતરણ મળતી વખતે ચૂકવણી કરવા માટે નીચેનામાંથી વિકલ્પ પસંદ કરો:",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = orderToPay.productNames,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "કુલ ચૂકવવાપાત્ર રકમ / Total Payable: ₹${String.format("%.2f", orderToPay.totalPrice)}",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ચૂકવણી વિકલ્પો / Payment Options:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. UPI option
                        Card(
                            onClick = { selectedPaymentOption = "UPI" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentOption == "UPI") 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (selectedPaymentOption == "UPI") 
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentOption == "UPI",
                                    onClick = { selectedPaymentOption = "UPI" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("ઓનલાઇન યુપીઆઈ / UPI (GPay/PhonePe)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("ઇન્સ્ટન્ટ ઓનલાઇન ચૂકવણી કરો / Instant Online UPI", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // 2. Wallet option
                        Card(
                            onClick = { selectedPaymentOption = "WALLET" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentOption == "WALLET") 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (selectedPaymentOption == "WALLET") 
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentOption == "WALLET",
                                    onClick = { selectedPaymentOption = "WALLET" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("EarnMitra વોલેટ બેલેન્સ / Wallet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = "બેલેન્સ / Balance: ₹${String.format("%.2f", user.walletBalance)}", 
                                        fontSize = 10.sp, 
                                        color = if (isWalletBalanceSufficient) Color.Gray else Color(0xFFC62828),
                                        fontWeight = if (isWalletBalanceSufficient) FontWeight.Normal else FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. Cash payment option
                        Card(
                            onClick = { selectedPaymentOption = "CASH" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentOption == "CASH") 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (selectedPaymentOption == "CASH") 
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentOption == "CASH",
                                    onClick = { selectedPaymentOption = "CASH" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("રોકડ ચૂકવણી / Pay by Cash", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("ડિલિવરી એજન્ટને રોકડા રૂપિયા આપો / Cash paid to delivery person", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}// 4. Settings & Security Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(viewModel: AppViewModel, user: UserAccount) {
    val lang = viewModel.selectedLanguage
    val context = LocalContext.current

    var showBackupDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    if (showTermsDialog) {
        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
    }

    var aadharInput by remember(user.aadharNumber) { mutableStateOf(user.aadharNumber) }
    var panInput by remember(user.panNumber) { mutableStateOf(user.panNumber) }
    var bankNameInput by remember(user.bankName) { mutableStateOf(user.bankName ?: "") }
    var bankAccountNumberInput by remember(user.bankAccountNumber) { mutableStateOf(user.bankAccountNumber ?: "") }
    var bankIfscCodeInput by remember(user.bankIfscCode) { mutableStateOf(user.bankIfscCode ?: "") }
    var bankUpiIdInput by remember(user.bankUpiId) { mutableStateOf(user.bankUpiId ?: "") }
    
    var isUploadingKyc by remember { mutableStateOf(false) }
    var kycUploadProgress by remember { mutableStateOf(0f) }
    var kycDocImageUri by remember(user.kycDocImage) { mutableStateOf(user.kycDocImage) }
    
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    val isKycComplete = user.kycStatus == "APPROVED" || (user.aadharNumber.length == 12 && user.panNumber.length == 10)

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.updateProfilePhotoFromUri(uri)
        }
    }

    val kycGalleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isUploadingKyc = true
            kycUploadProgress = 0.3f
            com.example.data.FirebaseSyncService.uploadKycDocument(user.uid, uri) { downloadUrl ->
                isUploadingKyc = false
                if (downloadUrl != null) {
                    kycDocImageUri = downloadUrl
                    Toast.makeText(context, "ઓળખપત્ર સફળતાપૂર્વક અપલોડ થયું! / Document uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "અપલોડ નિષ્ફળ થયું! કૃપા કરીને ફરી પ્રયાસ કરો. / Upload failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Profile & KYC Card
        item {
            var isIdentityExpanded by remember { mutableStateOf(true) }
            var isBankExpanded by remember { mutableStateOf(true) }
            var isUploadExpanded by remember { mutableStateOf(true) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Avatar profile image selection block
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var showAvatarPicker by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { showAvatarPicker = true },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            AvatarIcon(user.profileImage, modifier = Modifier.size(72.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "પ્રોફાઇલ ફોટો બદલવા માટે અવતાર પર ટેપ કરો / Tap avatar to change photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        if (showAvatarPicker) {
                            AlertDialog(
                                onDismissRequest = { showAvatarPicker = false },
                                title = { Text("અવતાર પસંદ કરો / Select Avatar", fontWeight = FontWeight.Bold) },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showAvatarPicker = false }) { Text("બંધ કરો / Close") }
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = {
                                                galleryLauncher.launch("image/*")
                                                showAvatarPicker = false
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ગેલેરીમાંથી ફોટો લો / Select from Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 12.dp))

                                        Text("તમારી પ્રોફાઇલ માટે સુંદર અવતાર પસંદ કરો:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
                                        val avatars = listOf(
                                            Pair("astro_boy", "Astro Boy"),
                                            Pair("crypto_hustler", "Crypto Hustler"),
                                            Pair("business_woman", "Business Woman"),
                                            Pair("zen_monk", "Zen Monk"),
                                            Pair("tech_guru", "Tech Guru"),
                                            Pair("eco_warrior", "Eco Warrior")
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            for (i in avatars.indices step 3) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    for (j in i until minOf(i + 3, avatars.size)) {
                                                        val av = avatars[j]
                                                        Card(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable {
                                                                    viewModel.updateProfilePhoto(av.first)
                                                                    showAvatarPicker = false
                                                                },
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (user.profileImage == av.first) MaterialTheme.colorScheme.primaryContainer
                                                                else MaterialTheme.colorScheme.surface
                                                            ),
                                                            border = BorderStroke(
                                                                2.dp,
                                                                if (user.profileImage == av.first) MaterialTheme.colorScheme.primary
                                                                else Color.Transparent
                                                            ),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(8.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                AvatarIcon(av.first, modifier = Modifier.size(48.dp))
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(av.second, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("પ્રોફાઇલ અને કેવાયસી દસ્તાવેજો / Profile & KYC", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        if (isKycComplete) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("કેવાયસી પૂર્ણ / KYC Done", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                border = BorderStroke(1.dp, Color(0xFFC62828)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("કેવાયસી અપૂર્ણ / KYC Pending", color = Color(0xFFC62828), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // User ID
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("યુનિક આઈડી / Unique ID:", fontSize = 12.sp, color = Color.Gray)
                        Text(user.uid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    // Full Name
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("પૂરું નામ / Full Name:", fontSize = 12.sp, color = Color.Gray)
                        Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    // Mobile Number
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("મોબાઇલ નંબર / Mobile:", fontSize = 12.sp, color = Color.Gray)
                        Text(user.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.Gray.copy(alpha = 0.2f))

                    // 1. Identity section
                    CollapsibleSectionHeader(
                        title = "૧. ઓળખપત્ર વિગતો / Identity Details",
                        icon = Icons.Default.Fingerprint,
                        isExpanded = isIdentityExpanded,
                        onToggle = { isIdentityExpanded = !isIdentityExpanded }
                    )

                    if (isIdentityExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("મહેરબાની કરીને ઉપાડ (Withdrawal) સુવિધા ચાલુ કરવા માટે સાચો આધાર અને પાનકાર્ડ દાખલ કરો:", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = aadharInput,
                            onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) aadharInput = it },
                            label = { Text("આધાર કાર્ડ નંબર / Aadhaar Card Number (12 અંક)") },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = panInput,
                            onValueChange = { if (it.length <= 10) panInput = it.uppercase() },
                            label = { Text("પાનકાર્ડ નંબર / PAN Card Number (10 અંક)") },
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Bank section
                    CollapsibleSectionHeader(
                        title = "૨. બેંક ખાતાની વિગતો / Bank Account Details",
                        icon = Icons.Default.AccountBalance,
                        isExpanded = isBankExpanded,
                        onToggle = { isBankExpanded = !isBankExpanded }
                    )

                    if (isBankExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bankNameInput,
                            onValueChange = { bankNameInput = it },
                            label = { Text("બેંકનું નામ / Bank Name") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bankAccountNumberInput,
                            onValueChange = { if (it.all { char -> char.isDigit() }) bankAccountNumberInput = it },
                            label = { Text("એકાઉન્ટ નંબર / Bank Account Number") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bankIfscCodeInput,
                            onValueChange = { bankIfscCodeInput = it.uppercase() },
                            label = { Text("આઈએફએસસી કોડ / IFSC Code") },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bankUpiIdInput,
                            onValueChange = { bankUpiIdInput = it },
                            label = { Text("યુપીઆઈ આઈડી / UPI ID (e.g., name@okaxis)") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Upload section
                    CollapsibleSectionHeader(
                        title = "૩. દસ્તાવેજ ઓળખપત્ર અપલોડ / Upload ID Proof",
                        icon = Icons.Default.UploadFile,
                        isExpanded = isUploadExpanded,
                        onToggle = { isUploadExpanded = !isUploadExpanded }
                    )

                    if (isUploadExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (kycDocImageUri != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("આધાર / પાન ઓળખપત્ર અપલોડ થયું છે", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("kyc_identity_document.jpg", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    TextButton(
                                        onClick = { kycDocImageUri = null }
                                    ) {
                                        Text("દૂર કરો", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else if (isUploadingKyc) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("દસ્તાવેજ અપલોડ થઈ રહ્યો છે... / Uploading ID...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { kycUploadProgress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${(kycUploadProgress * 100).toInt()}% uploaded", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        } else {
                            // Two elegant options: Real Gallery select and demo simulation
                            val coroutineScope = rememberCoroutineScope()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Real upload
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            kycGalleryLauncher.launch("image/*")
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("ગેલેરીમાંથી અપલોડ કરો / Upload from Gallery", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("વાસ્તવિક આધાર/પાન ફોટો Firebase Storage માં સેવ થશે", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                // 2. Simulated upload
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isUploadingKyc = true
                                            kycUploadProgress = 0f
                                            coroutineScope.launch {
                                                for (progress in 1..10) {
                                                    delay(100)
                                                    kycUploadProgress = progress / 10f
                                                }
                                                kycDocImageUri = "https://earnmitra-51580.firebasestorage.app/demo/simulated_id_proof.jpg"
                                                isUploadingKyc = false
                                                Toast.makeText(context, "ડેમો ફાઇલ વેરિફિકેશન સફળ! / Demo document loaded!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("ઝડપી ડેમો ફાઇલ અપલોડ કરો / Simulate Quick Upload", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                            Text("સિગ્નલ ટેસ્ટ કરવા માટે એક ક્લિકમાં ઇમેજ સેટઅપ થશે", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (aadharInput.length == 12 && panInput.length == 10 && bankAccountNumberInput.isNotBlank() && bankIfscCodeInput.isNotBlank()) {
                                viewModel.updateUserProfileComplete(
                                    aadhar = aadharInput,
                                    pan = panInput,
                                    bankName = bankNameInput,
                                    bankAccount = bankAccountNumberInput,
                                    bankIfsc = bankIfscCodeInput,
                                    bankUpi = bankUpiIdInput,
                                    kycStatus = "APPROVED",
                                    kycDocImage = kycDocImageUri ?: "simulated_id_proof.jpg"
                                )
                                Toast.makeText(context, "પ્રોફાઇલ અને KYC સફળતાપૂર્વક અપડેટ થયા! / Profile and KYC Saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "કૃપા કરીને બધી જ જરૂરી વિગતો ભરો! / Please fill all required fields!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("પ્રોફાઇલ અને કેવાયસી સાચવો / Save Profile & KYC")
                    }
                }
            }
        }

        // Dynamic Quick Language Switcher Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("એપ્લિકેશન ભાષા / Application Language", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Language.values().forEach { l ->
                            val isSelected = viewModel.selectedLanguage == l
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(l) },
                                label = {
                                    Text(
                                        text = l.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Appearance & Display Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (viewModel.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ડાર્ક મોડ / Dark Theme", fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = viewModel.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                }
            }
        }

        // App Lock PIN & SMS OTP Security Card
        item {
            var pinLockEnabled by remember(user.isSecurityLockEnabled) { mutableStateOf(user.isSecurityLockEnabled) }
            var pinCodeInput by remember(user.securityPin) { mutableStateOf(user.securityPin ?: "1234") }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("એપ સિક્યોરિટી અને પિન લોક / Advanced Security", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(15.dp))

                    // 1. PIN Lock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("૪-અંકનો સુરક્ષા પિન લોક / 4-Digit PIN Lock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("એપ્લિકેશન ખોલતી વખતે સુરક્ષા માટે પિન લોક સક્રિય કરો.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = pinLockEnabled,
                            onCheckedChange = { pinLockEnabled = it }
                        )
                    }

                    if (pinLockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pinCodeInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinCodeInput = it },
                            label = { Text("સુરક્ષા પિન / Security PIN (4 અંક)") },
                            leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateSecuritySettings(
                                isLockEnabled = pinLockEnabled,
                                pin = pinCodeInput,
                                twilioSid = user.twilioSid ?: "",
                                twilioToken = user.twilioToken ?: "",
                                twilioFrom = user.twilioFromPhone ?: "",
                                isRealOtp = user.realOtpEnabled
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("સુરક્ષા સેટિંગ્સ સાચવો / Save Security Settings")
                    }
                }
            }
        }

        // Customizable Notifications Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        Translation.get("notif_title", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("પ્રમોશન અને પ્રચાર નોટિફિકેશન / Promotion Alerts", fontSize = 12.sp)
                        Switch(
                            checked = user.notifPromo,
                            onCheckedChange = { viewModel.updateNotificationSetting("PROMO", it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("કમિશન અને ટ્રાન્ઝેક્શન ચેતવણીઓ / Financial Alerts", fontSize = 12.sp)
                        Switch(
                            checked = user.notifTrans,
                            onCheckedChange = { viewModel.updateNotificationSetting("TRANS", it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("સિસ્ટમ અપડેટ અને સમાચાર / System Announcements", fontSize = 12.sp)
                        Switch(
                            checked = user.notifSystem,
                            onCheckedChange = { viewModel.updateNotificationSetting("SYSTEM", it) }
                        )
                    }
                }
            }
        }

        // GPS Location tracking block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Translation.get("location_tracking", lang), fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = viewModel.isTrackingLocation,
                            onCheckedChange = { viewModel.toggleLocationTracking() }
                        )
                    }
                    
                    if (viewModel.isTrackingLocation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Latitude: ${String.format(Locale.getDefault(), "%.4f", viewModel.locationLat)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Longitude: ${String.format(Locale.getDefault(), "%.4f", viewModel.locationLng)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.simulateLocationUpdate() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("સ્થાન અપડેટ કરો / Simulate GPS Move")
                        }
                    }
                }
            }
        }

        // Terms & Conditions Compliance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("નિયમો અને શરતો / Terms & Conditions", fontWeight = FontWeight.Bold)
                                Text("એપ ગવર્મેન્ટ એપ્રુવલ અને કાયદાકીય શરતો / Regulatory compliance guidelines", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Button(
                            onClick = { showTermsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("જુઓ / View", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Offline compatibility / Sync and Backup simulation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translation.get("offline_status", lang), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    Text(
                        text = "એપ્લિકેશન સંપૂર્ણપણે ઓફલાઇન કાર્યરત છે. જ્યારે ઇન્ટરનેટ કનેક્ટ થાય ત્યારે ડેટા ઓટો-સિંક થશે.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showBackupDialog = true },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(Translation.get("backup_btn", lang), fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { showSyncDialog = true },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(Translation.get("sync_btn", lang), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }

            if (showBackupDialog) {
                AlertDialog(
                    onDismissRequest = { showBackupDialog = false },
                    title = { Text("સ્થાનિક બેકઅપ") },
                    text = { Text(Translation.get("backup_success", lang)) },
                    confirmButton = {
                        TextButton(onClick = { showBackupDialog = false }) {
                            Text("ઓકે / OK")
                        }
                    }
                )
            }

            if (showSyncDialog) {
                AlertDialog(
                    onDismissRequest = { showSyncDialog = false },
                    title = { Text("ઝડપી સિંક્રોનાઇઝેશન") },
                    text = { Text(Translation.get("sync_success", lang)) },
                    confirmButton = {
                        TextButton(onClick = { showSyncDialog = false }) {
                            Text("ઓકે / OK")
                        }
                    }
                )
            }
        }

        // App Updates Card
        item {
            var testerOptionsExpanded by remember { mutableStateOf(false) }
            var editDriveUrl by remember { mutableStateOf(viewModel.googleDriveUpdateUrl) }
            var editVersionName by remember { mutableStateOf(viewModel.newVersionName) }
            var editVersionCode by remember { mutableStateOf(viewModel.newVersionCode.toString()) }
            var editReleaseNotes by remember { mutableStateOf(viewModel.updateReleaseNotes) }
            
            LaunchedEffect(viewModel.googleDriveUpdateUrl, viewModel.newVersionName, viewModel.newVersionCode, viewModel.updateReleaseNotes) {
                editDriveUrl = viewModel.googleDriveUpdateUrl
                editVersionName = viewModel.newVersionName
                editVersionCode = viewModel.newVersionCode.toString()
                editReleaseNotes = viewModel.updateReleaseNotes
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title and Current Version row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "એપ્લિકેશન અપડેટ / In-App Update",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "સંસ્કરણ / Current Version: v${viewModel.currentVersionName}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // If an update is available (newVersionCode > currentVersionCode)
                    val isUpdateAvailable = viewModel.currentVersionCode < viewModel.newVersionCode
                    
                    if (isUpdateAvailable) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "નવું વર્ઝન v${viewModel.newVersionName} ઉપલબ્ધ છે! / New version v${viewModel.newVersionName} is available!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                if (viewModel.updateReleaseNotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "નવું શું છે / What's New:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = viewModel.updateReleaseNotes,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Wide Download/Update Button at the bottom
                        Button(
                            onClick = { viewModel.startUpdateDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ડાઉનલોડ અને ઇન્સ્ટોલ કરો / Download & Install", fontSize = 13.sp)
                        }
                    } else {
                        // Up to date indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32), // Green color
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "તમે નવીનતમ સંસ્કરણ પર છો / You are on the latest version.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Center/wide Check button at the bottom of update section
                        OutlinedButton(
                            onClick = { viewModel.checkForUpdates(context, forceManualCheck = true) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("અપડેટ માટે તપાસો / Check for Updates", fontSize = 12.sp)
                        }
                    }

                    // Show Tester Controls only if user is Admin (EM10000)
                    if (user.uid == "EM10000") {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { testerOptionsExpanded = !testerOptionsExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("વધારાના ટેસ્ટર સેટિંગ્સ / Tester Controls", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                            Icon(
                                imageVector = if (testerOptionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (testerOptionsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateType = InAppUpdateType.FLEXIBLE
                                        viewModel.checkForUpdates(context, forceManualCheck = true)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Flexible ટેસ્ટ", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.triggerForceImmediateUpdate()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Immediate ટેસ્ટ", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { viewModel.resetVersionForTesting() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.1f), contentColor = Color.DarkGray),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("રીસેટ v1.0", fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "સર્વર અપડેટ કંટ્રોલ / Server Update Control",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editDriveUrl,
                                onValueChange = { editDriveUrl = it },
                                label = { Text("અપડેટ APK લિંક (GitHub Pages અથવા Drive) / APK Update Link", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editVersionName,
                                    onValueChange = { editVersionName = it },
                                    label = { Text("નવું વર્ઝન નેમ / Version Name", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = editVersionCode,
                                    onValueChange = { editVersionCode = it },
                                    label = { Text("વર્ઝન કોડ / Version Code", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editReleaseNotes,
                                onValueChange = { editReleaseNotes = it },
                                label = { Text("નવું શું છે / Release Notes (Gujarati / English)", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    val vCode = editVersionCode.toIntOrNull() ?: viewModel.newVersionCode
                                    viewModel.saveUpdateConfiguration(editVersionName, vCode, editReleaseNotes, editDriveUrl)
                                    Toast.makeText(context, "અપડેટ વિગતો સેવ થઈ ગઈ! / Update config saved!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("સેવ અને લાગુ કરો / Save & Apply Update", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Play Store Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ગૂગલ પ્લે અપડેટ કનેક્શન / Try Real Play Store API", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("જો એપ પ્લે સ્ટોર પર હોય તો જ કામ કરે / Only works if installed from Google Play", fontSize = 9.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = viewModel.usePlayStoreUpdate,
                                    onCheckedChange = { viewModel.usePlayStoreUpdate = it }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- CUSTOMER SUPPORT & FAQ HELPDESK ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ગ્રાહક સપોર્ટ અને પ્રશ્નોત્તરી / Support & FAQ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "કોઈપણ પ્રશ્ન અથવા મુશ્કેલી માટે અમારા સપોર્ટ નંબર પર વોટ્સએપ મેસેજ કરો. ૨૪ કલાકમાં જવાબ મળશે.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // FAQs list
                    val faqs = listOf(
                        Pair(
                            "૧. અર્નમિત્ર એપ્લિકેશન શું છે? / What is EarnMitra?",
                            "અર્નમિત્ર એ ૧૦-લેવલની ડિજિટલ રેફરલ અને કમિશન કમાવવાની વ્યવસ્થિત એપ્લિકેશન છે. જેમાં તમે સભ્યો જોડીને કમિશન મેળવી શકો છો."
                        ),
                        Pair(
                            "૨. હું કેટલા સભ્યોને રેફર કરી શકું? / Direct Referrals Limit?",
                            "સુરક્ષા અને ગુણવત્તા માટે, દરેક સભ્ય વધુમાં વધુ ૩ ડાયરેક્ટ સભ્યો ઉમેરી શકે છે. ત્યારબાદ લિંક શેરિંગ બંધ થઈ જશે."
                        ),
                        Pair(
                            "૩. કમાયેલા પૈસા ક્યારે ઉપાડી શકાય? / When can I withdraw?",
                            "તમારા ખાતામાં ન્યૂનતમ ₹૫૦૦ થતાં જ તમે સીધા તમારા બેંક ખાતામાં અથવા યુપીઆઈ દ્વારા ઉપાડની વિનંતી મોકલી શકો છો."
                        ),
                        Pair(
                            "૪. કેવાયસી (KYC) કેમ જરૂરી છે? / Why KYC is mandatory?",
                            "સરકારી નિયમો અનુસાર બેંક ખાતામાં કમિશન જમા કરાવવા માટે સાચું આધાર કાર્ડ અને પાનકાર્ડ હોવું ફરજિયાત છે."
                        ),
                        Pair(
                            "૫. લેવલ ૧ થી ૧૦ કમિશન કઈ રીતે મળે છે? / How does Level 1-10 commission work?",
                            "જ્યારે તમારો સીધો રેફરલ ફી ભરી જોડાય, ત્યારે તમને લેવલ ૧ નું કમિશન મળે. જો એ સભ્ય આગળ કોઈને જોડે, તો લેવલ ૨ નું મળે, એમ ૧૦ લેવલ સુધી કમિશન મળે છે."
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        faqs.forEachIndexed { idx, faq ->
                            val isExpanded = expandedFaqIndex == idx
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedFaqIndex = if (isExpanded) null else idx
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = faq.first,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = faq.second,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct WhatsApp Support button
                    Button(
                        onClick = {
                            val communityUrl = "https://chat.whatsapp.com/HnefSOtsdgiE0WKVY7FkbV"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(communityUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "વોટ્સએપ ખોલી શકાયું નથી! / WhatsApp not found!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Support,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "વોટ્સએપ કમ્યુનિટી ગ્રુપમાં જોડાઓ / Join WhatsApp Support Community",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Admin Verification Panel Card
        if (user.uid == "EM10000") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("એડમિન પેમેન્ટ વેરીફીકેશન પેનલ / Admin Payment Verification Panel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "બાકી રહેલા પેમેન્ટ્સ ચકાસો અને આઈડી એક્ટિવેટ કરો: / Verify pending payments and activate accounts:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val pendingUsers = viewModel.pendingVerificationUsers
                        if (pendingUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("કોઈ પેન્ડિંગ ચુકવણી નથી / No pending payments", fontWeight = FontWeight.Medium, color = Color.Gray)
                            }
                        } else {
                            pendingUsers.forEach { pendingUser ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(pendingUser.fullName, fontWeight = FontWeight.Bold)
                                                Text("UID: ${pendingUser.uid} | Ph: ${pendingUser.phoneNumber}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("₹૧,૦૦૦", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("UTR: ${pendingUser.pendingUtr}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("UTR", pendingUser.pendingUtr)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "UTR નકલ થઈ ગઈ! / UTR Copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UTR", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.adminRejectUser(pendingUser.uid)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.adminApproveUser(pendingUser.uid)
                                                },
                                                modifier = Modifier.weight(1.2f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Deep green for Approve
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve & Active", fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Logout Block
        item {
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translation.get("logout", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Gujarati, 1: English
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedTab == 0) "નિયમો અને શરતો" else "Terms & Conditions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Compact Lang Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Text(
                        text = "ગુજ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "EN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedTab == 0) {
                    // GUJARATI SECTION
                    Text(
                        text = "૧. સરકારી અને કાયદાકીય નિયમો",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "આ એપ્લિકેશન સ્થાનિક અને કેન્દ્રીય નિયમો હેઠળ કાર્ય કરે છે. ભવિષ્યમાં કોઈપણ સરકારી એપ્રુવલ મેળવતી વખતે કોઈ પણ અસુવિધા કે ક્ષતિ ન સર્જાય તે હેતુસર તમામ ગાઈડલાઈન્સનું પાલન કરવામાં આવ્યું છે.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "૨. કેવાયસી (KYC) ની ફરજિયાત પ્રક્રિયા",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "તમારા બેંક એકાઉન્ટ અથવા UPI દ્વારા કમિશન ઉપાડ માટે સાચો આધાર કાર્ડ નંબર (૧૨ અંક) અને પાનકાર્ડ નંબર (૧૦ અંક) આપવા ફરજિયાત છે. વિગતો ખોટી હશે તો ઉપાડ અટકાવી દેવામાં આવશે.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "૩. જોડાણ ફી અને સક્રિયકરણ (₹1,000)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "એપ્લિકેશનના ઉપયોગ માટે ₹૧,૦૦૦ ની જોડાણ ફી ફરજિયાત છે, જે નોન-રિફન્ડેબલ છે. આ રકમ એકાઉન્ટ સક્રિય કરવા અને ડિજિટલ સેવાઓની જાળવણી માટે લેવામાં આવે છે.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "૪. રેફરલ કમિશન મર્યાદા (MLM Structure)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "કોઈપણ આઈડી હેઠળ વધુમાં વધુ ૩ ડાયરેક્ટ રેફરલ્સની મર્યાદા છે. રેફરલ કરનાર વ્યક્તિને કમિશન ત્યારે જ મળશે જ્યારે રેફર કરેલ નવો યુઝર સફળતાપૂર્વક ₹૧,૦૦૦ ભરીને પોતાનું આઈડી સક્રિય કરે. કમિશન ૧૦ લેવલ સુધી વહેંચવામાં આવે છે.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "૫. વિરોધી અને છેતરપિંડી પ્રવૃત્તિ પર પ્રતિબંધ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "એક જ વ્યક્તિ દ્વારા નકલી દસ્તાવેજો કે ડુપ્લિકેટ મોબાઈલ નંબરોનો ઉપયોગ કરીને મલ્ટીપલ આઈડી બનાવવી સખત પ્રતિબંધિત છે. આવી પ્રવૃત્તિ જણાશે તો આઈડી કાયમી ધોરણે બ્લોક કરવામાં આવશે.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )
                } else {
                    // ENGLISH SECTION
                    Text(
                        text = "1. Government & Legal Compliance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "This app strictly operates in compliance with national and state fintech policies. The features are fully aligned to prevent any inconvenience during upcoming government audits or license approvals.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "2. Mandatory KYC Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "To enable payout withdrawals, providing a valid 12-digit Aadhaar Card number and a 10-digit PAN Card number is mandatory. Withdrawal options are locked until KYC is successfully saved.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "3. Activation Deposit (₹1,000)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "A one-time setup and activation deposit of ₹1,000 is required to fully activate the profile and participate in the network. This fee is non-refundable.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "4. Referral Commissions Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "Direct referrals are strictly capped at 3 per account. Commissions are credited to referrers only when their referred recruits complete their ₹1,000 deposit and activate their profiles. MLM payouts extend to a maximum of 10 structural tiers.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "5. Anti-Fraud & Account Safety",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "Creating multi-accounts with overlapping IDs or dummy data is prohibited. Accounts caught engaging in manipulation are subject to instant freeze and potential legal referral.",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text(if (selectedTab == 0) "સમજાયું / OK" else "I Agree / OK", fontSize = 11.sp)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun InAppUpdateOverlay(viewModel: AppViewModel) {
    val status = viewModel.updateStatus
    val type = viewModel.updateType
    val context = LocalContext.current
    
    if (status == UpdateStatus.CHECKING) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            dismissButton = {},
            title = { Text("તપાસી રહ્યું છે... / Checking...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("નવીનતમ સંસ્કરણ માટે તપાસ કરી રહ્યું છે... / Checking for updates...")
                }
            }
        )
    }

    // Immediate / Blocker Update Dialog
    if (status == UpdateStatus.UPDATE_AVAILABLE && type == InAppUpdateType.IMMEDIATE) {
        AlertDialog(
            onDismissRequest = {}, // Blocker: cannot be dismissed
            confirmButton = {
                Button(
                    onClick = { viewModel.startUpdateDownload() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("હમણાં અપડેટ કરો / Update Now")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("મહત્વપૂર્ણ અપડેટ ઉપલબ્ધ! / Force Update", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "નવું વર્ઝન v${viewModel.newVersionName} ઉપલબ્ધ છે. આગળ વધવા માટે તમારે આ અપડેટ કરવું આવશ્યક છે. / A critical update v${viewModel.newVersionName} is required to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                       Column(modifier = Modifier.padding(12.dp)) {
                           Text("નવું શું છે / Release Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                           Spacer(modifier = Modifier.height(4.dp))
                           Text(viewModel.updateReleaseNotes, fontSize = 11.sp, color = Color.Gray)
                       }
                    }
                }
            }
        )
    }

    // Flexible update dialog (custom bottom sheet style or clean dialog prompt)
    if (status == UpdateStatus.UPDATE_AVAILABLE && type == InAppUpdateType.FLEXIBLE) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            confirmButton = {
                Button(
                    onClick = { viewModel.startUpdateDownload() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ડાઉનલોડ કરો / Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) {
                    Text("પછીથી / Later")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("નવું અપડેટ ઉપલબ્ધ! / Update Available", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("સંસ્કરણ v${viewModel.newVersionName} ઉપલબ્ધ છે. શું તમે તેને બેકગ્રાઉન્ડમાં ડાઉનલોડ કરવા માંગો છો? / Version v${viewModel.newVersionName} is ready. Would you like to download it?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                       Column(modifier = Modifier.padding(12.dp)) {
                           Text("નવું શું છે / Release Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                           Spacer(modifier = Modifier.height(4.dp))
                           Text(viewModel.updateReleaseNotes, fontSize = 11.sp, color = Color.Gray)
                       }
                    }
                }
            }
        )
    }

    // Downloading dialog/screen
    if (status == UpdateStatus.DOWNLOADING) {
        AlertDialog(
            onDismissRequest = {}, // Cannot dismiss while downloading
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("અપડેટ ડાઉનલોડ થઈ રહ્યું છે... / Downloading...", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = viewModel.updateProgress,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ડાઉનલોડ ઝડપ / Speed: ${viewModel.updateDownloadSpeed}", fontSize = 11.sp, color = Color.Gray)
                        Text("${viewModel.updateBytesDownloaded} / ${viewModel.updateBytesTotal}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // Downloaded - Ready to Install
    if (status == UpdateStatus.DOWNLOADED) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(
                    onClick = { viewModel.completeUpdateInstall(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("લિંક ખોલો અને ઇન્સ્ટોલ કરો / Open Drive Link")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("અપડેટ તૈયાર છે! / Update Ready", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("અપડેટની વિગતો મળી ગઈ છે. હવે નવું APK ડાઉનલોડ કરવા અને તેને ઇન્સ્ટોલ કરવા માટે 'લિંક ખોલો અને ઇન્સ્ટોલ કરો' પર ક્લિક કરો. / Update is ready. Click 'Open Drive Link' to download and install the new APK from Google Drive.")
            }
        )
    }

    // Installing Dialog
    if (status == UpdateStatus.INSTALLING) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("અપડેટ ઇન્સ્ટોલ થઈ રહ્યું છે... / Installing...", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ફાઈલો અનપેક અને કન્ફિગર થઈ રહી છે... / Configuring and unpacking update files...")
                }
            }
        )
    }
}

// --- SECURE SECURITY PIN KEYPAD LOCK SCREEN ---
@Composable
fun PinLockScreen(viewModel: AppViewModel, user: UserAccount) {
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "EarnMitra સુરક્ષા લોક / App Lock",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "એપ્લિકેશન અનલૉક કરવા માટે તમારો ૪-અંકનો સિક્યોરિટી પિન દાખલ કરો. / Enter your 4-digit PIN to unlock.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Secure PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pinText.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                )
            }
        }

        if (pinError) {
            Text(
                text = "ખોટો પિન! ફરીથી પ્રયાસ કરો. / Invalid PIN! Try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Custom Numeric Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "Delete")
            )

            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    for (key in row) {
                        Button(
                            onClick = {
                                pinError = false
                                if (key == "Clear") {
                                    pinText = ""
                                } else if (key == "Delete") {
                                    if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                } else {
                                    if (pinText.length < 4) {
                                        pinText += key
                                        if (pinText.length == 4) {
                                            if (pinText == user.securityPin) {
                                                viewModel.isAppUnlocked = true
                                            } else {
                                                pinError = true
                                                pinText = ""
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "Clear" || key == "Delete") MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (key == "Delete") {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete")
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        TextButton(onClick = { viewModel.logout() }) {
            Text("બીજા ખાતામાં લોગઇન કરો / Switch Account")
        }
    }
}

// --- DYNAMIC OTP SMS ALERT OVERLAY ---
@Composable
fun SmsAlertOverlay(viewModel: AppViewModel) {
    val alert = viewModel.incomingSmsAlert
    if (alert != null) {
        val context = LocalContext.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    // Auto copy OTP code
                    val otpCode = viewModel.generatedOtp
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("OTP", otpCode)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "ઓટીપી કોપી થયો! / OTP Copied!", Toast.LENGTH_SHORT).show()
                    viewModel.incomingSmsAlert = null
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "મેસેજ (હમણાં) / Messages (Now)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.incomingSmsAlert = null }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "કોપી કરવા માટે અહીં ક્લિક કરો / Click here to copy OTP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// --- VISUAL HEADS-UP NOTIFICATION OVERLAY ---
@Composable
fun HeadsUpNotificationOverlay(viewModel: AppViewModel) {
    val notification = viewModel.headsUpNotification
    if (notification != null) {
        LaunchedEffect(notification.id) {
            kotlinx.coroutines.delay(5000)
            viewModel.headsUpNotification = null
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    viewModel.headsUpNotification = null
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "DEPOSIT" -> Icons.Default.AccountBalanceWallet
                        "COMMISSION" -> Icons.Default.CardGiftcard
                        "WITHDRAWAL" -> Icons.Default.TrendingDown
                        else -> Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// --- NOTIFICATION CENTER DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterDialog(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val notifs = viewModel.currentNotifications
    val unreadCount = notifs.count { !it.isRead }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                viewModel.markAllNotificationsRead()
                onDismiss()
            }) {
                Text("બધા વાંચેલા તરીકે માર્ક કરો / Mark all as Read")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearNotifications()
            }) {
                Text("બધા સાફ કરો / Clear All", color = MaterialTheme.colorScheme.error)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("નોટિફિકેશન સેન્ટર / Alerts", fontWeight = FontWeight.Bold)
                }
                if (unreadCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("$unreadCount", color = Color.White)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (notifs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("કોઈ નવી નોટિફિકેશન નથી / No new alerts", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifs) { notif ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (notif.isRead) Color.Transparent
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = when (notif.type) {
                                            "DEPOSIT" -> Icons.Default.AccountBalanceWallet
                                            "COMMISSION" -> Icons.Default.CardGiftcard
                                            "WITHDRAWAL" -> Icons.Default.TrendingDown
                                            else -> Icons.Default.NotificationsActive
                                        },
                                        contentDescription = null,
                                        tint = if (notif.isRead) Color.Gray else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notif.message,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

// --- UPI PAYMENT GATEWAY GATEWAY CHECKOUT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiCheckoutDialog(
    targetUid: String,
    targetName: String,
    viewModel: AppViewModel,
    onActivate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var utrNumber by remember { mutableStateOf("") }
    var utrError by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf("PhonePe") }

    val upiIds = viewModel.customRegistrationUpiIds
    var selectedUpiId by remember { mutableStateOf(upiIds.firstOrNull() ?: "earnmitra@ybl") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val cleaned = utrNumber.trim()
                    val firstChar = if (cleaned.isNotEmpty()) cleaned[0] else ' '
                    if (cleaned.length != 12 || !cleaned.all { it.isDigit() }) {
                        utrError = "મહેરબાની કરીને સાચો ૧૨-અંકનો UTR નંબર દાખલ કરો / Enter valid 12-digit UTR number"
                    } else if (firstChar != '4' && firstChar != '5' && firstChar != '6') {
                        utrError = "અમાન્ય UTR! યુટીઆર નંબર 4, 5 કે 6 થી શરૂ થવો જોઈએ. / Invalid UTR format!"
                    } else if (cleaned.all { it == firstChar }) {
                        utrError = "ખોટો UTR નંબર! પુનરાવર્તિત અંકો માન્ય નથી. / Fake UTR! Repeated digits are not allowed."
                    } else {
                        onActivate(cleaned)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("યુટીઆર સબમિટ કરો / Submit UTR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("બંધ કરો / Cancel")
            }
        },
        title = {
            Text("સુરક્ષિત પેમેન્ટ ગેટવે / Secure UPI Gateway", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "એકાઉન્ટ એક્ટિવેશન ફી: ₹૧,૦૦૦",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ખાતાધારક: $targetName ($targetUid)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(15.dp))

                // UPI ID / Gateway Server Selection Row
                Text(
                    text = "પેમેન્ટ ગેટવે ચેનલ પસંદ કરો / Select UPI Gateway:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    upiIds.forEach { upi ->
                        val isSel = selectedUpiId == upi
                        val label = when(upi) {
                            "earnmitra@ybl" -> "ચેનલ ૧ (YBL)"
                            "earnmitra@ibl" -> "ચેનલ ૨ (IBL)"
                            "earnmitra@axl" -> "ચેનલ ૩ (AXL)"
                            "earnmitra1@ybl" -> "ચેનલ ૪ (YBL-1)"
                            "earnmitra1@ibl" -> "ચેનલ ૫ (IBL-1)"
                            "earnmitra1@axl" -> "ચેનલ ૬ (AXL-1)"
                            else -> upi
                        }
                        Surface(
                            onClick = { selectedUpiId = upi },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App selection
                Text("UPI એપ પસંદ કરો / Select UPI App:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    listOf("PhonePe", "GooglePay", "Paytm").forEach { app ->
                        val isSel = selectedApp == app
                        Button(
                            onClick = { 
                                selectedApp = app
                                // Launch real Android UPI Deep Link Intent!
                                try {
                                    val upiUri = "upi://pay?pa=$selectedUpiId&pn=EarnMitra&am=1000.00&cu=INR&tn=EM-$targetUid-Activation"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse(upiUri)
                                        when (app) {
                                            "PhonePe" -> setPackage("com.phonepe.app")
                                            "GooglePay" -> setPackage("com.google.android.apps.nbu.paisa.user")
                                            "Paytm" -> setPackage("net.one97.paytm")
                                        }
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to general UPI chooser if specific package is missing
                                    try {
                                        val upiUri = "upi://pay?pa=$selectedUpiId&pn=EarnMitra&am=1000.00&cu=INR&tn=EM-$targetUid-Activation"
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(upiUri))
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        android.widget.Toast.makeText(context, "તમારા ફોનમાં કોઈ UPI એપ ઉપલબ્ધ નથી! / No UPI App found on this device.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(app, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated UPI QR Code or scan box
                Card(
                    modifier = Modifier
                        .size(160.dp)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("UPI QR Code", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(selectedUpiId, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }

                Text(
                    text = "૧. કોઈપણ UPI એપથી QR કોડ સ્કેન કરી ₹૧,૦૦૦ પે કરો.\n૨. પેમેન્ટ પછી ટ્રાન્ઝેક્શન વિગતોમાંથી ૧૨-અંકનો UTR/Ref નંબર કોપી કરો અને નીચે દાખલ કરો.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = utrNumber,
                    onValueChange = {
                        utrNumber = it
                        utrError = null
                    },
                    label = { Text("૧૨-અંકનો UTR નંબર / 12-Digit UTR Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = utrError != null
                )

                if (utrError != null) {
                    Text(
                        text = utrError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
                    )
                }
            }
        }
    )
}

// --- PROCEDURALLY DRAWN CUSTOM VECTOR AVATARS ---
@Composable
fun AvatarIcon(avatarCode: String?, modifier: Modifier = Modifier) {
    if (avatarCode != null && avatarCode.startsWith("/")) {
        val file = java.io.File(avatarCode)
        if (file.exists()) {
            coil.compose.AsyncImage(
                model = file,
                contentDescription = "Profile Photo",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = modifier.clip(CircleShape)
            )
            return
        }
    }

    val bg = when (avatarCode) {
        "astro_boy" -> Color(0xFF1E88E5)
        "crypto_hustler" -> Color(0xFFFFB300)
        "business_woman" -> Color(0xFF3F51B5)
        "zen_monk" -> Color(0xFF00B0FF)
        "tech_guru" -> Color(0xFF7B1FA2)
        "eco_warrior" -> Color(0xFF43A047)
        else -> MaterialTheme.colorScheme.primary
    }
    val iconVec = when (avatarCode) {
        "astro_boy" -> Icons.Default.Star
        "crypto_hustler" -> Icons.Default.CurrencyRupee
        "business_woman" -> Icons.Default.Business
        "zen_monk" -> Icons.Default.LocalFlorist
        "tech_guru" -> Icons.Default.Computer
        "eco_warrior" -> Icons.Default.Eco
        else -> Icons.Default.AccountCircle
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(iconVec, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.6f))
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPt = center
            val w = size.width
            
            // 1. Light green background circle
            drawCircle(
                color = Color(0xFFE8F5E9), // Light green background
                radius = w / 2f,
                center = centerPt
            )
            
            // 2. Connecting lines & outer nodes
            val lineLength = w * 0.30f
            val outerCircleRadius = w * 0.11f
            val angles = listOf(-90f, 30f, 150f)
            
            angles.forEach { angle ->
                val rad = java.lang.Math.toRadians(angle.toDouble())
                val endX = centerPt.x + (lineLength * java.lang.Math.cos(rad)).toFloat()
                val endY = centerPt.y + (lineLength * java.lang.Math.sin(rad)).toFloat()
                
                // Draw connecting line from center to outer node
                drawLine(
                    color = Color(0xFFFFB300), // Golden/yellow lines
                    start = centerPt,
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = (w * 0.04f).coerceAtLeast(1.5f)
                )
                
                // Draw outer golden circle
                drawCircle(
                    color = Color(0xFFFFB300), // Golden/yellow circle
                    radius = outerCircleRadius,
                    center = androidx.compose.ui.geometry.Offset(endX, endY)
                )
            }
            
            // 3. Center green circle (where the people are)
            val centerCircleRadius = w * 0.22f
            drawCircle(
                color = Color(0xFF4CAF50), // Main vibrant green
                radius = centerCircleRadius,
                center = centerPt
            )
        }
        
        // 4. White people silhouette icon in the center
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.32f)
        )
    }
}

@Composable
fun CommissionCalculatorComponent(lang: Language) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧮", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (lang == Language.GUJARATI) "કમિશન કેલ્ક્યુલેટર" else "Commission Calculator",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == Language.GUJARATI) "તમારી નેટવર્ક કમાણીનો હિસાબ કરો" else "Estimate your network earnings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ADVANCED MODE
            var level1Count by remember { mutableStateOf(3) }
            var duplicationRate by remember { mutableStateOf(3) }
            var depthLevels by remember { mutableStateOf(5) }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Control Row 1: Level 1 Count
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.GUJARATI) "૧. ડાયરેક્ટ રેફરલ્સ (લેવલ ૧):" else "1. Direct Referrals (L1):",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$level1Count",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Slider(
                        value = level1Count.toFloat(),
                        onValueChange = { level1Count = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }

                // Control Row 2: Duplication Rate
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.GUJARATI) "૨. સભ્ય ડુપ્લિકેશન રેટ (ટીમ સાઈઝ):" else "2. Member Duplication Rate:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$duplicationRate",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (lang == Language.GUJARATI) "દરેક સભ્ય સરેરાશ કેટલા નવા સભ્યો ઉમેરશે" else "Average members invited by each person down the line",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Slider(
                        value = duplicationRate.toFloat(),
                        onValueChange = { duplicationRate = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }

                // Control Row 3: Depth Levels
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Language.GUJARATI) "૩. ગણતરી ઊંડાઈ (લેવલ):" else "3. Team Level Depth:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Level $depthLevels",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Slider(
                        value = depthLevels.toFloat(),
                        onValueChange = { depthLevels = it.toInt() },
                        valueRange = 2f..10f,
                        steps = 7
                    )
                }

                // Calculate Team and Earnings
                val levelDetails = mutableListOf<LevelCalcs>()
                var currentCount = level1Count.toDouble()
                var totalTeamSize = 0.0
                var totalEarnings = 0.0

                for (level in 1..depthLevels) {
                    val rate = if (level == 1) 200.0 else 30.0
                    val levelEarnings = currentCount * rate
                    
                    levelDetails.add(
                        LevelCalcs(
                            level = level,
                            members = currentCount,
                            rate = rate,
                            earnings = levelEarnings
                        )
                    )
                    
                    totalTeamSize += currentCount
                    totalEarnings += levelEarnings
                    
                    // duplication for next level
                    currentCount *= duplicationRate
                }

                // Total Earnings Panel
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (lang == Language.GUJARATI) "કુલ અંદાજિત કમાણી" else "Total Estimated Commission",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", totalEarnings)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.CurrencyRupee,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Divider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (lang == Language.GUJARATI) "કુલ સંભવિત ટીમ:" else "Total Potential Team:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.0f", totalTeamSize)} " + (if (lang == Language.GUJARATI) "સભ્યો" else "Members"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Level wise detailed list
                Text(
                    text = if (lang == Language.GUJARATI) "લેવલ મુજબ વિગતવાર બ્રેકડાઉન:" else "Level-wise Detailed Breakdown:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    levelDetails.forEach { levelCalc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    if (levelCalc.level == 1) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.secondary,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${levelCalc.level}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "લેવલ ${levelCalc.level} / Level ${levelCalc.level}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.0f", levelCalc.earnings)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%,.0f", levelCalc.members)} સભ્યો / Members",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "દર / Rate: ₹${levelCalc.rate.toInt()}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                // Team ratio indicator bar
                                val ratio = if (totalTeamSize > 0) (levelCalc.members / totalTeamSize).toFloat() else 0f
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { ratio.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = if (levelCalc.level == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



data class LevelCalcs(
    val level: Int,
    val members: Double,
    val rate: Double,
    val earnings: Double
)

@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 3
    val colorScheme = MaterialTheme.colorScheme

    val pages = listOf(
        Triple(
            "૧. સભ્ય બનો / Join Now",
            "અર્નમિત્ર ડિજિટલ નેટવર્ક સાથે જોડાઓ અને ₹૧,૦૦૦ ની સભ્ય ફી ભરીને તમારું બિઝનેસ એકાઉન્ટ સક્રિય કરો. આનાથી કમિશન સિસ્ટમ અનલોક થશે.",
            Icons.Default.GroupAdd
        ),
        Triple(
            "૨. મિત્રોને રેફર કરો / Refer Friends",
            "તમારા રેફરલ કાર્ડ અને QR કોડનો ઉપયોગ કરીને ફક્ત ૩ મિત્રોને જોડો. દરેક સીધા જોડાણ પર ₹૨૦૦ અને ટીમ ગ્રોથ પર વધારાનું કમિશન મેળવો!",
            Icons.Default.Share
        ),
        Triple(
            "૩. સીધી કમાણી ઉપાડો / Direct Payouts",
            "કોઈ પણ અડચણ વગર તમારી તમામ કમાણી સેકન્ડોમાં સીધી તમારા બેંક ખાતા અથવા UPI માં ઉપાડો. ૧૦૦% ઓફલાઇન સપોર્ટેડ અને સુરક્ષિત!",
            Icons.Default.AccountBalanceWallet
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Name Header
            Text(
                text = "અર્નમિત્ર / EarnMitra",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "તમારું ડિજિટલ રેફરલ કમાણી નેટવર્ક",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Animated slide content switcher
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "onboarding_slider"
            ) { page ->
                val slide = pages[page]
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(bottom = 16.dp),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slide.third,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = slide.first,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = slide.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                for (i in 0 until totalPages) {
                    Box(
                        modifier = Modifier
                            .size(width = if (currentPage == i) 24.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (currentPage == i) colorScheme.primary else Color.Gray.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = { currentPage-- }) {
                        Text("પાછળ / Back", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                        } else {
                            viewModel.completeOnboarding()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (currentPage == totalPages - 1) "શરૂ કરો / Get Started" else "આગળ / Next",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (currentPage < totalPages - 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedQrCode(uid: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp)) {
        val size = this.size.width
        val blocks = 11
        val blockSize = size / blocks
        
        // Clear background
        drawRect(color = Color.White)
        
        // Standard finder pattern locations
        val finderIndices = listOf(
            Pair(0, 0), Pair(blocks - 3, 0), Pair(0, blocks - 3)
        )
        
        // Draw standard QR finder patterns
        for (finder in finderIndices) {
            val fx = finder.first * blockSize
            val fy = finder.second * blockSize
            // Outer 3x3 block
            drawRect(
                color = primaryColor,
                topLeft = androidx.compose.ui.geometry.Offset(fx, fy),
                size = androidx.compose.ui.geometry.Size(blockSize * 3, blockSize * 3)
            )
            // Inner 1x1 white block
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(fx + blockSize, fy + blockSize),
                size = androidx.compose.ui.geometry.Size(blockSize, blockSize)
            )
        }
        
        // Draw simulated random/deterministic QR noise
        val hash = uid.hashCode()
        for (row in 0 until blocks) {
            for (col in 0 until blocks) {
                // Skip finder patterns
                if ((row < 3 && col < 3) || (row < 3 && col >= blocks - 3) || (row >= blocks - 3 && col < 3)) {
                    continue
                }
                
                // Make it look realistic by leaving some spacing
                val bitIndex = (row * blocks + col) % 32
                val isFilled = ((hash ushr bitIndex) and 1) == 1
                if (isFilled && (row % 2 == 0 || col % 2 == 0 || (row + col) % 3 == 0)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(col * blockSize, row * blockSize),
                        size = androidx.compose.ui.geometry.Size(blockSize, blockSize)
                    )
                }
            }
        }
    }
}

// --- HELPER COMPONENTS FOR COMPACT LAYOUT & COUNTRY PICKER ---
data class CountryCode(val code: String, val name: String, val flag: String)

val countryCodes = listOf(
    CountryCode("+91", "India", "🇮🇳"),
    CountryCode("+1", "United States", "🇺🇸"),
    CountryCode("+44", "United Kingdom", "🇬🇧"),
    CountryCode("+971", "United Arab Emirates", "🇦🇪"),
    CountryCode("+86", "China", "🇨🇳"),
    CountryCode("+81", "Japan", "🇯🇵"),
    CountryCode("+49", "Germany", "🇩🇪"),
    CountryCode("+33", "France", "🇫🇷"),
    CountryCode("+7", "Russia", "🇷🇺"),
    CountryCode("+61", "Australia", "🇦🇺"),
    CountryCode("+1", "Canada", "🇨🇦"),
    CountryCode("+65", "Singapore", "🇸🇬"),
    CountryCode("+27", "South Africa", "🇿🇦"),
    CountryCode("+55", "Brazil", "🇧🇷"),
    CountryCode("+92", "Pakistan", "🇵🇰"),
    CountryCode("+880", "Bangladesh", "🇧🇩"),
    CountryCode("+94", "Sri Lanka", "🇱🇰"),
    CountryCode("+977", "Nepal", "🇳🇵"),
    CountryCode("+62", "Indonesia", "🇮🇩"),
    CountryCode("+60", "Malaysia", "🇲🇾"),
    CountryCode("+63", "Philippines", "🇵🇭"),
    CountryCode("+64", "New Zealand", "🇳🇿"),
    CountryCode("+39", "Italy", "🇮🇹"),
    CountryCode("+34", "Spain", "🇪🇸"),
    CountryCode("+966", "Saudi Arabia", "🇸🇦"),
    CountryCode("+90", "Turkey", "🇹🇷")
)

@Composable
fun CountryCodePicker(
    selectedCode: CountryCode,
    onCodeSelected: (CountryCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(selectedCode.flag, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(selectedCode.code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(2.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            countryCodes.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(country.flag, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${country.name} (${country.code})", fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        onCodeSelected(country)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CollapsibleSectionHeader(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- REAL-TIME VISUAL ORDER TRACKING STEPPER ---
@Composable
fun OrderTrackingStepper(order: Order, modifier: Modifier = Modifier) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val orderDate = sdf.format(Date(order.timestamp))
    
    // Calculate simulated timeline dates based on order.timestamp
    val shippedDate = sdf.format(Date(order.timestamp + 86400000L)) // +1 day
    val outForDeliveryDate = sdf.format(Date(order.timestamp + 259200000L)) // +3 days
    val deliveredDate = sdf.format(Date(order.timestamp + 345600000L)) // +4 days

    val stages = when (order.status) {
        "PENDING" -> listOf(
            TrackingStage("ઓર્ડર કન્ફર્મ / Confirmed", orderDate, true, "તમારો ઓર્ડર સ્વીકારવામાં આવ્યો છે"),
            TrackingStage("શિપિંગ થયું / Shipped", "અંદાજિત: ${shippedDate}", false, "વસ્તુ ટૂંક સમયમાં મોકલવામાં આવશે"),
            TrackingStage("ડિલિવરી માટે બહાર / Out for Delivery", "અંદાજિત: ${outForDeliveryDate}", false, "સ્થાનિક હબ તરફથી રવાના"),
            TrackingStage("ડિલિવર થયેલ / Delivered", "અંદાજિત: ${deliveredDate}", false, "૪-૫ દિવસમાં વિતરણ થશે")
        )
        "SHIPPED" -> listOf(
            TrackingStage("ઓર્ડર કન્ફર્મ / Confirmed", orderDate, true, "તમારો ઓર્ડર સ્વીકારવામાં આવ્યો છે"),
            TrackingStage("શિપિંગ થયું / Shipped", shippedDate, true, "વસ્તુ કુરિયર દ્વારા રવાના થઈ ગઈ છે"),
            TrackingStage("ડિલિવરી માટે બહાર / Out for Delivery", "અંદાજિત: ${outForDeliveryDate}", true, "આજે અથવા આવતીકાલે વિતરણ શરૂ"),
            TrackingStage("ડિલિવર થયેલ / Delivered", "અંદાજિત: ${deliveredDate}", false, "ટૂંક સમયમાં સરનામે પહોંચશે")
        )
        else -> listOf( // "DELIVERED"
            TrackingStage("ઓર્ડર કન્ફર્મ / Confirmed", orderDate, true, "તમારો ઓર્ડર સ્વીકારવામાં આવ્યો છે"),
            TrackingStage("શિપિંગ થયું / Shipped", shippedDate, true, "વસ્તુ કુરિયર દ્વારા રવાના થઈ ગઈ છે"),
            TrackingStage("ડિલિવરી માટે બહાર / Out for Delivery", outForDeliveryDate, true, "ડિલિવરી એજન્ટ સરનામે પહોંચેલ"),
            TrackingStage("ડિલિવર થયેલ / Delivered", deliveredDate, true, "વસ્તુ સફળતાપૂર્વક ડિલિવર થઈ ગઈ છે")
        )
    }

    val estimateText = when (order.status) {
        "PENDING" -> "અંદાજિત ડિલિવરી: ૪ થી ૫ દિવસમાં / Estimated Delivery: 4 to 5 Days"
        "SHIPPED" -> "અંદાજિત ડિલિવરી: ૧ થી ૨ દિવસમાં / Estimated Delivery: 1 to 2 Days"
        else -> "ડિલિવરી પૂર્ણ થઈ ગઈ છે / Delivery Completed Successfully!"
    }

    val progressColor = if (order.status == "DELIVERED") Color(0xFF2E7D32) else Color(0xFFE65100)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = progressColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "રીઅલ-ટાઇમ ઓર્ડર ટ્રેકિંગ / Order Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = progressColor
                    )
                }
                Text(
                    text = order.status,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = progressColor,
                    modifier = Modifier
                        .background(progressColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stepper vertical implementation
            stages.forEachIndexed { index, stage ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Timeline line & circle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (stage.isDone) progressColor else Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (stage.isDone) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        if (index < stages.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(30.dp)
                                    .background(if (stage.isDone && stages[index + 1].isDone) progressColor else Color.LightGray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Stage content details
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stage.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (stage.isDone) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                            Text(
                                text = stage.date,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (stage.isDone) progressColor else Color.Gray
                            )
                        }
                        Text(
                            text = stage.description,
                            fontSize = 9.sp,
                            color = Color.Gray,
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // Arrival Days Highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = progressColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = estimateText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
        }
    }
}

data class TrackingStage(
    val title: String,
    val date: String,
    val isDone: Boolean,
    val description: String
)

// --- INTERACTIVE VISUAL GENEALOGY TEAM TREE DIAGRAM ---
@Composable
fun InteractiveTeamTreeCard(viewModel: AppViewModel, currentUser: UserAccount) {
    val lang = viewModel.selectedLanguage
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ટીમ ટ્રી ગ્રાફ / Interactive Genealogy Tree",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = { expandedNodes = emptySet() }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                        contentDescription = "Collapse All",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "તમારા ૧૦-લેવલના નેટવર્કને જોવા માટે સભ્યો પર ટેપ કરીને નીચે જોડાયેલા લોકોને એક્સપાન્ડ કરો. / Tap members to expand and explore your 10-level downline network dynamically.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            TreeNodeItem(
                user = currentUser,
                allUsers = viewModel.allUsersList,
                level = 0,
                expandedNodes = expandedNodes,
                onToggleExpand = { uid ->
                    expandedNodes = if (expandedNodes.contains(uid)) {
                        expandedNodes - uid
                    } else {
                        expandedNodes + uid
                    }
                }
            )
        }
    }
}

@Composable
fun TreeNodeItem(
    user: UserAccount,
    allUsers: List<UserAccount>,
    level: Int,
    expandedNodes: Set<String>,
    onToggleExpand: (String) -> Unit
) {
    val children = allUsers.filter { it.referredBy == user.uid }
    val isExpanded = expandedNodes.contains(user.uid)
    val hasChildren = children.isNotEmpty()

    Column(modifier = Modifier.padding(start = (if (level > 0) 14 else 0).dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(
                    if (level == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .clickable(enabled = hasChildren) { onToggleExpand(user.uid) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (user.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "L$level",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            if (hasChildren) {
                Icon(
                    imageVector = if (isExpanded) androidx.compose.material.icons.Icons.Default.ExpandMore else androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${user.uid} • " + (if (user.isActive) "સક્રિય / Active" else "નિષ્ક્રિય / Inactive"),
                    fontSize = 10.sp,
                    color = if (user.isActive) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
            }

            if (hasChildren) {
                Text(
                    text = "${children.size} Refs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        if (isExpanded && hasChildren) {
            children.forEach { child ->
                TreeNodeItem(
                    user = child,
                    allUsers = allUsers,
                    level = level + 1,
                    expandedNodes = expandedNodes,
                    onToggleExpand = onToggleExpand
                )
            }
        }
    }
}

// --- INTERACTIVE EARNINGS ANALYTICS CHART CARD ---
@Composable
fun EarningAnalyticsChartCard(viewModel: AppViewModel) {
    val lang = viewModel.selectedLanguage
    var chartType by remember { mutableStateOf(0) } // 0 for 7-Day Trend, 1 for Level-wise Earnings

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "કમાણી એનાલિટિક્સ / Earnings Analytics",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Chart toggle buttons
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (chartType == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { chartType = 0 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "7 Days",
                            color = if (chartType == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                if (chartType == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { chartType = 1 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "10 Levels",
                            color = if (chartType == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chartType == 0) {
                // 7-day trend line chart
                val trendData = listOf(150f, 320f, 220f, 480f, 400f, 650f, 520f)
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val primaryColor = MaterialTheme.colorScheme.primary

                Column {
                    Text(
                        text = "સાપ્તાહિક કમાણીનો આલેખ / Weekly Growth Trend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val padding = 30f
                        val graphWidth = width - 2 * padding
                        val graphHeight = height - 2 * padding
                        val maxVal = trendData.maxOrNull() ?: 1000f

                        // Draw Grid lines
                        for (i in 0..4) {
                            val y = padding + (graphHeight / 4) * i
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.4f),
                                start = androidx.compose.ui.geometry.Offset(padding, y),
                                end = androidx.compose.ui.geometry.Offset(width - padding, y),
                                strokeWidth = 2f
                            )
                        }

                        // Plot Path
                        val points = trendData.mapIndexed { idx, value ->
                            val x = padding + (graphWidth / (trendData.size - 1)) * idx
                            val y = padding + graphHeight - (value / maxVal) * graphHeight
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 5f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        // Draw Gradient fill below trend line
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, height - padding)
                            for (p in points) {
                                lineTo(p.x, p.y)
                            }
                            lineTo(points.last().x, height - padding)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                                startY = points.minOf { it.y },
                                endY = height - padding
                            )
                        )

                        // Draw data circles and labels
                        points.forEachIndexed { index, point ->
                            drawCircle(
                                color = primaryColor,
                                radius = 8f,
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = point
                            )
                        }
                    }

                    // Labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { day ->
                            Text(day, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }
            } else {
                // 10-level commission stats bar chart using real live data!
                val stats = viewModel.currentMlmStats.take(10)
                val primaryColor = MaterialTheme.colorScheme.primary

                Column {
                    Text(
                        text = "સ્તર મુજબની કમાણી વિગતો / MLM Levels Earnings (L1 - L10)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (stats.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("કોઈ કમિશન વિગતો ઉપલબ્ધ નથી / No commission statistics recorded.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val padding = 30f
                            val graphWidth = width - 2 * padding
                            val graphHeight = height - 2 * padding
                            val maxComm = stats.maxOfOrNull { it.earnedCommission }?.coerceAtLeast(100.0) ?: 1000.0

                            // Draw bars
                            val barWidth = (graphWidth / stats.size) * 0.6f
                            val gap = (graphWidth / stats.size) * 0.4f

                            stats.forEachIndexed { index, stat ->
                                val x = padding + index * (barWidth + gap) + gap / 2
                                val barHeight = (stat.earnedCommission / maxComm) * graphHeight
                                val y = padding + graphHeight - barHeight

                                drawRoundRect(
                                    color = if (stat.earnedCommission > 0) Color(0xFF4CAF50) else primaryColor.copy(alpha = 0.35f),
                                    topLeft = androidx.compose.ui.geometry.Offset(x, y.toFloat()),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight.toFloat()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                )
                            }
                        }

                        // Labels for L1 - L10
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            stats.forEach { stat ->
                                Text("L${stat.level}", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Admin Control Center Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTab(viewModel: AppViewModel, user: UserAccount) {
    val lang = viewModel.selectedLanguage
    val context = LocalContext.current
    
    // Load data initially
    LaunchedEffect(Unit) {
        viewModel.loadPendingVerificationUsers()
    }
    
    // Local configuration states
    var twilioSid by remember { mutableStateOf(viewModel.twilioSidState) }
    var twilioToken by remember { mutableStateOf(viewModel.twilioTokenState) }
    var twilioFromPhone by remember { mutableStateOf(viewModel.twilioFromPhoneState) }
    var globalRealOtp by remember { mutableStateOf(viewModel.globalRealOtpMode) }
    var adminWaNumber by remember { mutableStateOf(viewModel.adminWhatsAppNumber) }
    var referralWebUrl by remember { mutableStateOf(viewModel.referralWebsiteUrl) }
    
    // UPI list management states
    var newUpiIdInput by remember { mutableStateOf("") }
    
    // User management state
    var userSearchQuery by remember { mutableStateOf("") }
    var selectedUserForEdit by remember { mutableStateOf<UserAccount?>(null) }
    var showBalanceDialogForUser by remember { mutableStateOf<UserAccount?>(null) }
    var adjustAmountInput by remember { mutableStateOf("") }
    var isDeduction by remember { mutableStateOf(false) }
    
    // Product management state
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var prodNameEn by remember { mutableStateOf("") }
    var prodNameGu by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodCategory by remember { mutableStateOf("GROCERY") }
    var prodDescEn by remember { mutableStateOf("") }
    var prodDescGu by remember { mutableStateOf("") }
    var prodIsMandatory by remember { mutableStateOf(false) }
    var prodStockCount by remember { mutableStateOf("10") }
    var prodIsOutOfStock by remember { mutableStateOf(false) }
    var prodImageUrl by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupervisorAccount,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Column {
                        Text(
                            text = "એડમિન કંટ્રોલ સેન્ટર / Admin Control Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "બધી સુવિધાઓ એક જ સ્ક્રીનથી કંટ્રોલ કરો / Manage all configurations in one place",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        // 1. Pending payment approval section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "પેન્ડિંગ પેમેન્ટ વિનંતીઓ / Pending Payments",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val pendingList = viewModel.pendingVerificationUsers
                    if (pendingList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "કોઈ પેન્ડિંગ ચુકવણીઓ નથી! / No pending approvals",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        pendingList.forEach { pendingUser ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(pendingUser.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("UID: ${pendingUser.uid} | Ph: ${pendingUser.phoneNumber}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("₹૧,૦૦૦", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("UTR: ${pendingUser.pendingUtr}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("UTR", pendingUser.pendingUtr)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "UTR નકલ થઈ ગઈ! / UTR Copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UTR", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.adminRejectUser(pendingUser.uid) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.adminApproveUser(pendingUser.uid) },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 1b. Pending Product Order approvals
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "પેન્ડિંગ ઓર્ડર પેમેન્ટ વિનંતીઓ / Pending Order Payments",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val pendingOrdersList = viewModel.allOrdersListState.filter { it.paymentStatus == "PENDING_VERIFICATION" }
                    if (pendingOrdersList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "કોઈ પેન્ડિંગ ઓર્ડર ચુકવણીઓ નથી! / No pending order approvals",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        pendingOrdersList.forEach { pendingOrder ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ઓર્ડર ID: #${pendingOrder.id}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("કસ્ટમર UID: ${pendingOrder.uid}", fontSize = 11.sp, color = Color.Gray)
                                            Text("પ્રોડક્ટ્સ: ${pendingOrder.productNames}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("સરનામું: ${pendingOrder.shippingAddress}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text("₹${String.format("%.2f", pendingOrder.totalPrice)}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("UTR: ${pendingOrder.paymentRef}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("UTR", pendingOrder.paymentRef)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "UTR નકલ થઈ ગઈ! / UTR Copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UTR", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { 
                                                // Reject/Cancel Order Payment
                                                viewModel.adminUpdateOrderStatus(pendingOrder, "CANCELLED")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject & Cancel", fontSize = 10.sp, maxLines = 1)
                                        }
                                        Button(
                                            onClick = { 
                                                // Approve Order Payment
                                                viewModel.adminApproveOrderPayment(pendingOrder)
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve Payment", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 2. OTP & TWILIO SETTINGS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ઓટીપી ગેટવે (Twilio) કન્ફિગ્યુરેશન / Twilio Configuration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Real OTP Mode Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "રિયલ ઓટીપી મોડ / Real OTP Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "ચાલુ: Twilio વડે મોકલો | બંધ: ડેમો મોડ (કોડ '1234')",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = globalRealOtp,
                            onCheckedChange = {
                                globalRealOtp = it
                                viewModel.updateGlobalRealOtpMode(it)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = twilioSid,
                        onValueChange = { twilioSid = it },
                        label = { Text("Twilio Account SID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = twilioToken,
                        onValueChange = { twilioToken = it },
                        label = { Text("Twilio Auth Token") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = twilioFromPhone,
                        onValueChange = { twilioFromPhone = it },
                        label = { Text("Twilio Sender Number / Sender ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateTwilioSettings(twilioSid, twilioToken, twilioFromPhone)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Twilio સેટિંગ્સ સાચવો / Save Twilio Config")
                    }
                }
            }
        }
        
        // 3. ADMIN CONTACT & PAY UPI IDS CONFIG
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "એડમિન વોટ્સએપ અને યુપીઆઈ લિસ્ટ / Admin Details",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Admin WhatsApp Contact input
                    OutlinedTextField(
                        value = adminWaNumber,
                        onValueChange = { adminWaNumber = it },
                        label = { Text("એડમિન હેલ્પલાઇન વોટ્સએપ નંબર / Admin WhatsApp Link Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateAdminWhatsAppNumber(adminWaNumber)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("વોટ્સએપ નંબર સાચવો / Save Admin Number")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Referral Website URL input
                    Text(
                        text = "રેફરલ ડાઉનલોડ વેબસાઇટ લિંક સેટઅપ / Referral Web App Link:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = referralWebUrl,
                        onValueChange = { referralWebUrl = it },
                        label = { Text("રેફરલ વેબસાઇટ URL / Referral Website URL") },
                        placeholder = { Text("https://ais-pre-lssi3sfr4wtdjznoh2xcdt-1007319374021.asia-southeast1.run.app") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateReferralWebsiteUrl(referralWebUrl)
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("સાચવો / Save URL", maxLines = 1, fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                val defaultUrl = "https://ais-pre-lssi3sfr4wtdjznoh2xcdt-1007319374021.asia-southeast1.run.app"
                                referralWebUrl = defaultUrl
                                viewModel.updateReferralWebsiteUrl(defaultUrl)
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("ડિફોલ્ટ કરો / Reset", maxLines = 1, fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // UPI ID List management
                    Text(
                        text = "노ંધણી પેમેન્ટ માટે ઉપલબ્ધ યુપીઆઈ આઈડી લિસ્ટ: / Manage Gateway UPI IDs:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    viewModel.customRegistrationUpiIds.forEach { upi ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(upi, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            IconButton(
                                onClick = {
                                    val currentList = viewModel.customRegistrationUpiIds.toMutableList()
                                    currentList.remove(upi)
                                    viewModel.updateCustomUpiIds(currentList)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete UPI ID", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newUpiIdInput,
                            onValueChange = { newUpiIdInput = it },
                            label = { Text("નવો UPI ID ઉમેરો") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val trimmed = newUpiIdInput.trim()
                                if (trimmed.isNotBlank() && trimmed.contains("@")) {
                                    val currentList = viewModel.customRegistrationUpiIds.toMutableList()
                                    if (!currentList.contains(trimmed)) {
                                        currentList.add(trimmed)
                                        viewModel.updateCustomUpiIds(currentList)
                                        newUpiIdInput = ""
                                    } else {
                                        Toast.makeText(context, "UPI ID પહેલાથી જ લિસ્ટમાં છે! / Already exists!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "સાચો UPI ID દાખલ કરો! / Enter valid UPI ID!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add UPI")
                        }
                    }
                }
            }
        }
        
        // 4. ALL REGISTERED USERS MANAGEMENT
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "વપરાશકર્તાઓનું સંચાલન / Users Management",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        placeholder = { Text("વપરાશકર્તા શોધો (નામ / મોબાઈલ / UID)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (userSearchQuery.isNotBlank()) {
                                IconButton(onClick = { userSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val filteredUsers = viewModel.allUsersList.filter {
                        it.fullName.contains(userSearchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(userSearchQuery) ||
                        it.uid.contains(userSearchQuery, ignoreCase = true)
                    }
                    
                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "કોઈ મેચિંગ યુઝર મળ્યા નથી / No matching users found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        filteredUsers.take(15).forEach { userItem ->
                            val isExpanded = selectedUserForEdit?.uid == userItem.uid
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedUserForEdit = if (isExpanded) null else userItem
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(userItem.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (userItem.isActive) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("Active", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                } else {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("Inactive", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("UID: ${userItem.uid} | Ph: ${userItem.phoneNumber}", fontSize = 11.sp, color = Color.Gray)
                                            Text("વોલેટ બેલેન્સ / Wallet: ₹${String.format("%.2f", userItem.walletBalance)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand/Collapse"
                                        )
                                    }
                                    
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Additional info
                                        Text("જોડાણ તારીખ / Joined: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(userItem.createdTimestamp))}", fontSize = 11.sp, color = Color.Gray)
                                        Text("દ્વારા રેફરલ / Referred By: ${userItem.referredBy ?: "None (Direct Joined)"}", fontSize = 11.sp, color = Color.Gray)
                                        Text("KYC સ્ટેટસ / KYC Status: ${userItem.kycStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (userItem.kycStatus == "APPROVED") Color(0xFF2E7D32) else if (userItem.kycStatus == "PENDING") Color(0xFFEF6C00) else Color.Red)
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Row 1 of actions: Account Activation / Deactivation
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!userItem.isActive) {
                                                Button(
                                                    onClick = {
                                                        viewModel.adminForceActivateUser(userItem.uid)
                                                        selectedUserForEdit = userItem.copy(isActive = true, paymentStatus = "APPROVED")
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("એક્ટિવેટ કરો / Activate", fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        viewModel.adminDeactivateUser(userItem.uid)
                                                        selectedUserForEdit = userItem.copy(isActive = false, paymentStatus = "NOT_PAID")
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("ડી-એક્ટિવેટ / Deactivate", fontSize = 11.sp)
                                                }
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    showBalanceDialogForUser = userItem
                                                    adjustAmountInput = ""
                                                    isDeduction = false
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("બેલેન્સ બદલો / Adjust Bal", fontSize = 11.sp)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Row 2 of actions: KYC status change
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("KYC સેટ કરો: / Set KYC:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                            
                                            listOf("PENDING", "APPROVED", "NOT_STARTED").forEach { kStatus ->
                                                val isSelected = userItem.kycStatus == kStatus
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.adminUpdateUserKycStatus(userItem.uid, kStatus)
                                                        selectedUserForEdit = userItem.copy(kycStatus = kStatus)
                                                    },
                                                    label = { Text(kStatus, fontSize = 9.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 5. PRODUCT MANAGEMENT MENU
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "પ્રોડક્ટ મેનેજમેન્ટ / Product Management",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                prodNameEn = ""
                                prodNameGu = ""
                                prodPrice = ""
                                prodCategory = "GROCERY"
                                prodDescEn = ""
                                prodDescGu = ""
                                prodIsMandatory = false
                                prodStockCount = "10"
                                prodIsOutOfStock = false
                                prodImageUrl = ""
                                showAddProductDialog = true
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Product", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (viewModel.productsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "કોઈ પ્રોડક્ટ ઉપલબ્ધ નથી / No products found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        viewModel.productsList.forEach { product ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = if (lang == Language.GUJARATI) product.nameGu else product.nameEn,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                if (product.isMandatory) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("Mandatory", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Category: ${product.category}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = if (lang == Language.GUJARATI) product.descriptionGu else product.descriptionEn,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Text(
                                                text = "₹${product.price}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        editingProduct = product
                                                        prodNameEn = product.nameEn
                                                        prodNameGu = product.nameGu
                                                        prodPrice = product.price.toString()
                                                        prodCategory = product.category
                                                        prodDescEn = product.descriptionEn
                                                        prodDescGu = product.descriptionGu
                                                        prodIsMandatory = product.isMandatory
                                                        prodStockCount = product.stockCount.toString()
                                                        prodIsOutOfStock = product.isOutOfStock
                                                        prodImageUrl = product.imageUrl
                                                    },
                                                    modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                                }
                                                
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteProduct(product.id)
                                                    },
                                                    modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Balance Adjust dialog
    if (showBalanceDialogForUser != null) {
        AlertDialog(
            onDismissRequest = { showBalanceDialogForUser = null },
            title = { Text("બેલેન્સ કંટ્રોલ / Balance Control") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("વપરાશકર્તા: ${showBalanceDialogForUser!!.fullName} (${showBalanceDialogForUser!!.uid})")
                    Text("હાલનું વોલેટ બેલેન્સ: ₹${String.format("%.2f", showBalanceDialogForUser!!.walletBalance)}", fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isDeduction,
                            onClick = { isDeduction = false },
                            label = { Text("પૈસા ઉમેરો (+) / Add") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isDeduction,
                            onClick = { isDeduction = true },
                            label = { Text("પૈસા બાદ કરો (-) / Deduct") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    OutlinedTextField(
                        value = adjustAmountInput,
                        onValueChange = { adjustAmountInput = it },
                        label = { Text("રકમ દાખલ કરો / Enter Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = adjustAmountInput.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            val sign = if (isDeduction) -1.0 else 1.0
                            viewModel.adminAdjustUserBalance(showBalanceDialogForUser!!.uid, amt * sign)
                            showBalanceDialogForUser = null
                        } else {
                            Toast.makeText(context, "મહેરબાની કરીને સાચી રકમ દાખલ કરો! / Enter valid positive amount!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("પુષ્ટિ કરો / Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialogForUser = null }) {
                    Text("રદ કરો / Cancel")
                }
            }
        )
    }

    // 1. Add Product Dialog
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("નવી પ્રોડક્ટ ઉમેરો / Add New Product") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = prodNameEn,
                        onValueChange = { prodNameEn = it },
                        label = { Text("Product Name (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = prodNameGu,
                        onValueChange = { prodNameGu = it },
                        label = { Text("પ્રોડક્ટનું નામ (ગુજરાતી)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("કિંમત / Price (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Text("Category / શ્રેણી:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("GROCERY", "CLOTHING", "ELECTRONICS").forEach { cat ->
                            FilterChip(
                                selected = prodCategory == cat,
                                onClick = { prodCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = prodDescEn,
                        onValueChange = { prodDescEn = it },
                        label = { Text("Description (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = prodDescGu,
                        onValueChange = { prodDescGu = it },
                        label = { Text("વર્ણન (ગુજરાતી)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = prodStockCount,
                        onValueChange = { prodStockCount = it },
                        label = { Text("સ્ટોક સંખ્યા / Stock Count") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = prodImageUrl,
                        onValueChange = { prodImageUrl = it },
                        label = { Text("પ્રોડક્ટ ફોટો URL / Product Image URL") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("આઉટ ઓફ સ્ટોક બતાવો / Mark as Out of Stock:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Switch(
                            checked = prodIsOutOfStock,
                            onCheckedChange = { prodIsOutOfStock = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ફરજિયાત ખરીદી / Is Mandatory:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Switch(
                            checked = prodIsMandatory,
                            onCheckedChange = { prodIsMandatory = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = prodPrice.toDoubleOrNull()
                        val sc = prodStockCount.toIntOrNull() ?: 10
                        if (prodNameEn.isNotBlank() && prodNameGu.isNotBlank() && p != null && p > 0) {
                            viewModel.addProduct(
                                nameEn = prodNameEn,
                                nameGu = prodNameGu,
                                price = p,
                                category = prodCategory,
                                descriptionEn = prodDescEn,
                                descriptionGu = prodDescGu,
                                isMandatory = prodIsMandatory,
                                stockCount = sc,
                                isOutOfStock = prodIsOutOfStock || (sc <= 0),
                                imageUrl = prodImageUrl
                            )
                            showAddProductDialog = false
                        } else {
                            Toast.makeText(context, "સાચી વિગતો ભરો! / Fill valid details!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("ઉમેરો / Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("રદ કરો / Cancel")
                }
            }
        )
    }

    // 2. Edit Product Dialog
    if (editingProduct != null) {
        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { Text("પ્રોડક્ટ સંપાદિત કરો / Edit Product") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = prodNameEn,
                        onValueChange = { prodNameEn = it },
                        label = { Text("Product Name (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = prodNameGu,
                        onValueChange = { prodNameGu = it },
                        label = { Text("પ્રોડક્ટનું નામ (ગુજરાતી)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("કિંમત / Price (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Text("Category / શ્રેણી:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("GROCERY", "CLOTHING", "ELECTRONICS").forEach { cat ->
                            FilterChip(
                                selected = prodCategory == cat,
                                onClick = { prodCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = prodDescEn,
                        onValueChange = { prodDescEn = it },
                        label = { Text("Description (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = prodDescGu,
                        onValueChange = { prodDescGu = it },
                        label = { Text("વર્ણન (ગુજરાતી)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = prodStockCount,
                        onValueChange = { prodStockCount = it },
                        label = { Text("સ્ટોક સંખ્યા / Stock Count") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = prodImageUrl,
                        onValueChange = { prodImageUrl = it },
                        label = { Text("પ્રોડક્ટ ફોટો URL / Product Image URL") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("આઉટ ઓફ સ્ટોક બતાવો / Mark as Out of Stock:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Switch(
                            checked = prodIsOutOfStock,
                            onCheckedChange = { prodIsOutOfStock = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ફરજિયાત ખરીદી / Is Mandatory:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Switch(
                            checked = prodIsMandatory,
                            onCheckedChange = { prodIsMandatory = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = prodPrice.toDoubleOrNull()
                        val sc = prodStockCount.toIntOrNull() ?: 10
                        if (prodNameEn.isNotBlank() && prodNameGu.isNotBlank() && p != null && p > 0) {
                            val updated = editingProduct!!.copy(
                                nameEn = prodNameEn,
                                nameGu = prodNameGu,
                                price = p,
                                category = prodCategory,
                                descriptionEn = prodDescEn,
                                descriptionGu = prodDescGu,
                                isMandatory = prodIsMandatory,
                                stockCount = sc,
                                isOutOfStock = prodIsOutOfStock || (sc <= 0),
                                imageUrl = prodImageUrl
                            )
                            viewModel.updateProduct(updated)
                            editingProduct = null
                        } else {
                            Toast.makeText(context, "સાચી વિગતો ભરો! / Fill valid details!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("સાચવો / Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProduct = null }) {
                    Text("રદ કરો / Cancel")
                }
            }
        )
    }
}

