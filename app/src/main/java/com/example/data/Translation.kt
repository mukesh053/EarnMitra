package com.example.data

enum class Language(val displayName: String, val code: String) {
    GUJARATI("ગુજરાતી", "gu"),
    ENGLISH("English", "en"),
    HINDI("हिन्दी", "hi"),
    MARATHI("मराठी", "mr"),
    TAMIL("தமிழ்", "ta"),
    TELUGU("తెలుగు", "te"),
    BENGALI("বাংলা", "bn"),
    KANNADA("ಕನ್ನಡ", "kn"),
    PUNJABI("ਪੰਜਾਬੀ", "pa"),
    SPANISH("Español", "es"),
    FRENCH("Français", "fr"),
    GERMAN("Deutsch", "de"),
    ARABIC("العربية", "ar"),
    JAPANESE("日本語", "ja"),
    PORTUGUESE("Português", "pt")
}

object Translation {
    private val strings = mapOf(
        "app_title" to mapOf(
            "gu" to "અર્નમિત્ર",
            "en" to "EarnMitra",
            "hi" to "अर्नमित्र",
            "es" to "EarnMitra",
            "fr" to "EarnMitra"
        ),
        "tagline" to mapOf(
            "gu" to "પ્રચાર કરો અને અમર્યાદિત કમાણી કરો",
            "en" to "Promote and Earn Unlimited",
            "hi" to "प्रचार करें और असीमित कमाएं",
            "es" to "Promociona y gana ilimitado",
            "fr" to "Promouvoir et gagner de l'argent"
        ),
        "login_title" to mapOf(
            "gu" to "યુનિક આઈડી દ્વારા લોગીન",
            "en" to "Login with Unique ID",
            "hi" to "यूनीक आईडी से लॉगिन करें",
            "es" to "Iniciar sesión con ID único",
            "fr" to "Connexion avec identifiant unique"
        ),
        "register_title" to mapOf(
            "gu" to "નવું એકાઉન્ટ રજીસ્ટ્રેશન",
            "en" to "New Account Registration",
            "hi" to "नया खाता पंजीकरण",
            "es" to "Registro de nueva cuenta",
            "fr" to "Enregistrement d'un nouveau compte"
        ),
        "phone_hint" to mapOf(
            "gu" to "મોબાઇલ નંબર (૧૦ અંક)",
            "en" to "Mobile Number (10 digits)",
            "hi" to "मोबाइल नंबर (10 अंक)",
            "es" to "Número de móvil (10 dígitos)",
            "fr" to "Numéro de mobile (10 chiffres)"
        ),
        "name_hint" to mapOf(
            "gu" to "પૂરું નામ",
            "en" to "Full Name",
            "hi" to "पूरा नाम",
            "es" to "Nombre completo",
            "fr" to "Nom complet"
        ),
        "uid_hint" to mapOf(
            "gu" to "યુનિક આઈડી નંબર (દા.ત. EM10001)",
            "en" to "Unique ID Number (e.g., EM10001)",
            "hi" to "यूनीक आईडी नंबर (जैसे, EM10001)",
            "es" to "Número de ID único (ej., EM10001)",
            "fr" to "Numéro d'identifiant unique (ex., EM10001)"
        ),
        "referrer_hint" to mapOf(
            "gu" to "રેફરલ આઈડી (વૈકલ્પિક)",
            "en" to "Referral ID (Optional)",
            "hi" to "रेफरल आईडी (वैकल्पिक)",
            "es" to "ID de referencia (Opcional)",
            "fr" to "Identifiant de parrainage (Optionnel)"
        ),
        "send_otp" to mapOf(
            "gu" to "ઓટીપી મોકલો",
            "en" to "Send OTP",
            "hi" to "ओटीपी भेजें",
            "es" to "Enviar OTP",
            "fr" to "Envoyer l'OTP"
        ),
        "verify_otp" to mapOf(
            "gu" to "ઓટીપી ચકાસો",
            "en" to "Verify OTP",
            "hi" to "ओटीपी सत्यापित करें",
            "es" to "Verificar OTP",
            "fr" to "Vérifier l'OTP"
        ),
        "otp_hint" to mapOf(
            "gu" to "૪-અંકનો ઓટીપી દાખલ કરો",
            "en" to "Enter 4-digit OTP",
            "hi" to "4-अंकीय ओटीपी दर्ज करें",
            "es" to "Ingrese OTP de 4 dígitos",
            "fr" to "Entrez l'OTP à 4 chiffres"
        ),
        "register_btn" to mapOf(
            "gu" to "રજીસ્ટર કરો",
            "en" to "Register",
            "hi" to "पंजीकरण करें",
            "es" to "Registrarse",
            "fr" to "S'inscrire"
        ),
        "login_btn" to mapOf(
            "gu" to "લોગીન કરો",
            "en" to "Login",
            "hi" to "लॉगिन करें",
            "es" to "Iniciar sesión",
            "fr" to "Se connecter"
        ),
        "joining_fee_notice" to mapOf(
            "gu" to "નોંધ: આ એપ્લિકેશનનો ઉપયોગ કરવા માટે ₹૧,૦૦૦ ની જોડાણ ફી ફરજિયાત છે જે પરત મળવાપાત્ર નથી.",
            "en" to "Note: A joining fee of ₹1,000 is mandatory to use this app and is non-refundable.",
            "hi" to "नोट: इस ऐप का उपयोग करने के लिए ₹1,000 का शामिल होने का शुल्क अनिवार्य है और यह गैर-वापसी योग्य है.",
            "es" to "Nota: Se requiere una tarifa de unión de ₹1,000 para usar esta aplicación y no es reembolsable.",
            "fr" to "Remarque: Des frais d'adhésion de ₹1 000 sont obligatoires pour utiliser cette application et ne sont pas remboursables."
        ),
        "pay_fee" to mapOf(
            "gu" to "જોડાણ ફી ₹૧,૦૦૦ ચૂકવો",
            "en" to "Pay Joining Fee ₹1,000",
            "hi" to "शामिल होने का शुल्क ₹1,000 का भुगतान करें",
            "es" to "Pagar tarifa de unión ₹1,000",
            "fr" to "Payer les frais d'adhésion de ₹1 000"
        ),
        "payment_success" to mapOf(
            "gu" to "ચુકવણી સફળ! તમારું એકાઉન્ટ સક્રિય છે.",
            "en" to "Payment Successful! Your account is now active.",
            "hi" to "भुगतान सफल! आपका खाता अब सक्रिय है।",
            "es" to "¡Pago exitoso! Su cuenta ya está activa.",
            "fr" to "Paiement réussi! Votre compte est maintenant actif."
        ),
        "dashboard" to mapOf(
            "gu" to "ડેશબોર્ડ",
            "en" to "Dashboard",
            "hi" to "डैशबोर्ड",
            "es" to "Panel",
            "fr" to "Tableau de bord"
        ),
        "wallet" to mapOf(
            "gu" to "વોલેટ",
            "en" to "Wallet",
            "hi" to "वॉलेट",
            "es" to "Billetera",
            "fr" to "Portefeuille"
        ),
        "products" to mapOf(
            "gu" to "પ્રોડક્ટ્સ",
            "en" to "Products",
            "hi" to "उत्पाद",
            "es" to "Productos",
            "fr" to "Produits"
        ),
        "settings" to mapOf(
            "gu" to "સેટિંગ્સ",
            "en" to "Settings",
            "hi" to "सेटिंग्स",
            "es" to "Ajustes",
            "fr" to "Paramètres"
        ),
        "status_active" to mapOf(
            "gu" to "સક્રિય",
            "en" to "Active",
            "hi" to "सक्रिय",
            "es" to "Activo",
            "fr" to "Actif"
        ),
        "status_pending" to mapOf(
            "gu" to "બાકી ચૂકવણી",
            "en" to "Pending Activation",
            "hi" to "सक्रियता लंबित",
            "es" to "Activación pendiente",
            "fr" to "Activation en attente"
        ),
        "total_earnings" to mapOf(
            "gu" to "કુલ કમાણી",
            "en" to "Total Earnings",
            "hi" to "कुल कमाई",
            "es" to "Ganancias totales",
            "fr" to "Gains totaux"
        ),
        "referral_limit_notice" to mapOf(
            "gu" to "તમે મહત્તમ ૩ સીધા રેફરલ્સ ઉમેરી શકો છો. પ્રગતિ:",
            "en" to "You can refer up to 3 direct users. Progress:",
            "hi" to "आप अधिकतम 3 प्रत्यक्ष उपयोगकर्ताओं को संदर्भित कर सकते हैं। प्रगति:",
            "es" to "Puedes referir hasta 3 usuarios directos. Progreso:",
            "fr" to "Vous pouvez parrainer jusqu'à 3 utilisateurs directs. Progression:"
        ),
        "referral_system_locked" to mapOf(
            "gu" to "રેફરલ સિસ્ટમ આપમેળે બંધ થઈ ગઈ છે (૩/૩ પૂર્ણ)",
            "en" to "Referral system automatically locked (3/3 completed)",
            "hi" to "रेफरल प्रणाली स्वचालित रूप से बंद हो गई (3/3 पूर्ण)",
            "es" to "Sistema de referencia bloqueado automáticamente (3/3 completado)",
            "fr" to "Système de parrainage verrouillé automatiquement (3/3 complété)"
        ),
        "level_earnings" to mapOf(
            "gu" to "લેવલ મુજબ કમિશન ટ્રેકિંગ (૧૦ લેવલ)",
            "en" to "Level Commission Tracking (10 Levels)",
            "hi" to "स्तर के अनुसार कमीशन ट्रैकिंग (10 स्तर)",
            "es" to "Seguimiento de comisiones por nivel (10 niveles)",
            "fr" to "Suivi des commissions par niveau (10 niveaux)"
        ),
        "calc_title" to mapOf(
            "gu" to "કમિશન કેલ્ક્યુલેટર",
            "en" to "Commission Calculator",
            "hi" to "कमीशन कैलकुलेटर",
            "es" to "Calculadora de comisiones",
            "fr" to "Calculateur de commission"
        ),
        "add_referral" to mapOf(
            "gu" to "રેફરલ જોડો (ટેસ્ટ)",
            "en" to "Join Referral (Test)",
            "hi" to "रेफरल जोड़ें (परीक्षण)",
            "es" to "Unirse por referencia (Prueba)",
            "fr" to "Rejoindre le parrainage (Test)"
        ),
        "location_tracking" to mapOf(
            "gu" to "સ્થાન ટ્રેકિંગ (સચોટ)",
            "en" to "Location Tracking (Accurate)",
            "hi" to "स्थान ट्रैकिंग (सटीक)",
            "es" to "Rastreo de ubicación (Preciso)",
            "fr" to "Suivi de localisation (Précis)"
        ),
        "offline_status" to mapOf(
            "gu" to "ઓફલાઇન મોડ ઉપલબ્ધ",
            "en" to "Offline Mode Operational",
            "hi" to "ऑफ़लाइन मोड उपलब्ध",
            "es" to "Modo sin conexión operativo",
            "fr" to "Mode hors ligne opérationnel"
        ),
        "backup_btn" to mapOf(
            "gu" to "ડેટા બેકઅપ લો",
            "en" to "Backup Data Locally",
            "hi" to "स्थानीय रूप से डेटा बैकअप लें",
            "es" to "Copia de seguridad local",
            "fr" to "Sauvegarder les données localement"
        ),
        "backup_success" to mapOf(
            "gu" to "ડેટા બેકઅપ સફળતાપૂર્વક લેવાયો છે!",
            "en" to "Data backup successfully created!",
            "hi" to "डेटा बैकअप सफलतापूर्वक बनाया गया!",
            "es" to "¡Copia de seguridad creada con éxito!",
            "fr" to "Sauvegarde des données réussie!"
        ),
        "sync_btn" to mapOf(
            "gu" to "ડેટા સિંક્રોનાઇઝ કરો",
            "en" to "Synchronize Data Now",
            "hi" to "डेटा सिंक्रनाइज़ करें",
            "es" to "Sincronizar datos ahora",
            "fr" to "Synchroniser les données"
        ),
        "sync_success" to mapOf(
            "gu" to "ડેટા ઝડપથી સિંક્રોનાઇઝ થઈ ગયો છે!",
            "en" to "Data synchronized instantly!",
            "hi" to "डेटा तुरंत सिंक्रनाइज़ हो गया!",
            "es" to "¡Datos sincronizados al instante!",
            "fr" to "Données synchronisées instantanément!"
        ),
        "security_2fa" to mapOf(
            "gu" to "ટુ-સ્ટેપ વેરિફિકેશન (2FA)",
            "en" to "Two-Step Verification (2FA)",
            "hi" to "टू-स्टेप वेरिफिकेशन (2FA)",
            "es" to "Verificación en dos pasos (2FA)",
            "fr" to "Validation en deux étapes (2FA)"
        ),
        "security_2fa_desc" to mapOf(
            "gu" to "લોગીન સુરક્ષા માટે મોબાઇલ ઓટીપી ફરજિયાત કરો",
            "en" to "Enforce mobile OTP for secure log-ins",
            "hi" to "सुरक्षित लॉगिन के लिए मोबाइल ओटीपी लागू करें",
            "es" to "Exigir OTP móvil para inicios seguros",
            "fr" to "Exiger un OTP mobile pour les connexions sécurisées"
        ),
        "notif_title" to mapOf(
            "gu" to "કસ્ટમાઇઝ નોટિફિકેશન સેટિંગ્સ",
            "en" to "Customizable Notification Settings",
            "hi" to "अनुकूलन योग्य अधिसूचना सेटिंग्स",
            "es" to "Ajustes de notificación personalizables",
            "fr" to "Paramètres de notification personnalisables"
        ),
        "withdraw_upi" to mapOf(
            "gu" to "UPI દ્વારા વિડ્રો કરો",
            "en" to "Withdraw via UPI",
            "hi" to "UPI के माध्यम से निकालें",
            "es" to "Retirar por UPI",
            "fr" to "Retirer via UPI"
        ),
        "withdraw_bank" to mapOf(
            "gu" to "સીધા બેંક એકાઉન્ટમાં ટ્રાન્સફર",
            "en" to "Transfer directly to Bank Account",
            "hi" to "सीधे बैंक खाते में ट्रांसफर करें",
            "es" to "Transferir directo a cuenta bancaria",
            "fr" to "Transférer vers un compte bancaire"
        ),
        "phonepe_local" to mapOf(
            "gu" to "કમિશનથી સીધી ખરીદી (PhonePe વગેરે જેવી સુવિધા)",
            "en" to "Local Shopping using Commission (PhonePe-like)",
            "hi" to "कमीशन का उपयोग करके स्थानीय खरीदारी (PhonePe की तरह)",
            "es" to "Compras locales usando comisión (como PhonePe)",
            "fr" to "Achats locaux via commission (comme PhonePe)"
        ),
        "mandatory_tag" to mapOf(
            "gu" to "ખરીદવી ફરજિયાત",
            "en" to "Mandatory Purchase",
            "hi" to "अनिवार्य खरीदारी",
            "es" to "Compra obligatoria",
            "fr" to "Achat obligatoire"
        ),
        "promote_tag" to mapOf(
            "gu" to "પ્રચાર કરો",
            "en" to "Promote",
            "hi" to "प्रचार करें",
            "es" to "Promocionar",
            "fr" to "Promouvoir"
        ),
        "buy_now" to mapOf(
            "gu" to "ખરીદો",
            "en" to "Buy Now",
            "hi" to "अभी खरीदें",
            "es" to "Comprar ahora",
            "fr" to "Acheter"
        ),
        "promote_success" to mapOf(
            "gu" to "પ્રચાર લિંક કોપી થઈ ગઈ છે! શેર કરો અને કમિશન મેળવો.",
            "en" to "Promo link copied! Share to earn commission.",
            "hi" to "प्रोमो लिंक कॉपी हो गया! कमीशन कमाने के लिए साझा करें।",
            "es" to "¡Enlace promocional copiado! Comparte para ganar.",
            "fr" to "Lien promo copié! Partagez pour gagner."
        ),
        "buy_success" to mapOf(
            "gu" to "પ્રોડક્ટ ખરીદી સફળ રહી!",
            "en" to "Product purchased successfully!",
            "hi" to "उत्पाद सफलतापूर्वक खरीदा गया!",
            "es" to "¡Producto comprado con éxito!",
            "fr" to "Produit acheté avec succès!"
        ),
        "invalid_uid" to mapOf(
            "gu" to "અમાન્ય યુનિક આઈડી!",
            "en" to "Invalid Unique ID!",
            "hi" to "अमान्य यूनीक आईडी!",
            "es" to "¡ID único inválido!",
            "fr" to "Identifiant unique invalide!"
        ),
        "logout" to mapOf(
            "gu" to "લોગઆઉટ",
            "en" to "Logout",
            "hi" to "लॉगआउट",
            "es" to "Cerrar sesión",
            "fr" to "Déconnexion"
        ),
        "register_success_msg" to mapOf(
            "gu" to "રજીસ્ટ્રેશન સફળ! તમારું આઈડી સાચવો: ",
            "en" to "Registration Successful! Keep your ID safe: ",
            "hi" to "पंजीकरण सफल! अपनी आईडी सुरक्षित रखें: ",
            "es" to "¡Registro exitoso! Guarde su ID: ",
            "fr" to "Enregistrement réussi! Conservez votre ID: "
        ),
        "wallet_withdraw_amount" to mapOf(
            "gu" to "ઉપાડવાની રકમ દાખલ કરો",
            "en" to "Enter amount to withdraw",
            "hi" to "निकासी राशि दर्ज करें",
            "es" to "Ingrese monto a retirar",
            "fr" to "Entrez le montant à retirer"
        ),
        "insufficient" to mapOf(
            "gu" to "અપૂરતું બેલેન્સ!",
            "en" to "Insufficient balance!",
            "hi" to "अपर्याप्त राशि!",
            "es" to "¡Saldo insuficiente!",
            "fr" to "Solde insuffisant!"
        )
    )

    fun get(key: String, lang: Language): String {
        val map = strings[key] ?: return key
        return map[lang.code] ?: map["en"] ?: key
    }
}
