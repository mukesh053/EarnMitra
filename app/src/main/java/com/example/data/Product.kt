package com.example.data

data class Product(
    val id: Int,
    val nameEn: String,
    val nameGu: String,
    val price: Double,
    val isMandatory: Boolean,
    val descriptionEn: String,
    val descriptionGu: String,
    val category: String, // "ELECTRONICS", "GROCERY", "CLOTHING"
    val stockCount: Int = 10,
    val isOutOfStock: Boolean = false,
    val imageUrl: String = ""
)

val Product.commission: Double
    get() = 0.0

object ProductData {
    val items = listOf(
        Product(
            id = 1,
            nameEn = "Mandatory Welcome Kit (Tea & Spices)",
            nameGu = "ફરજિયાત સ્વાગત કીટ (ચા અને મસાલા)",
            price = 450.0,
            isMandatory = true,
            descriptionEn = "Essential daily kit of organic premium tea leaves and direct farm spices at wholesale rates.",
            descriptionGu = "જૈવિક પ્રીમિયમ ચા અને ખેતરના સીધા મસાલાની હોલસેલ ભાવે દૈનિક કીટ.",
            category = "GROCERY",
            imageUrl = "https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 2,
            nameEn = "Premium Organic Peanut Oil (5L)",
            nameGu = "પ્રીમિયમ ઓર્ગેનિક સીંગતેલ (૫ લિટર)",
            price = 950.0,
            isMandatory = true,
            descriptionEn = "Cold-pressed pure organic peanut oil, direct from local farmers.",
            descriptionGu = "સ્થાનિક ખેડૂતો પાસેથી સીધું કોલ્ડ-પ્રેસ્ડ શુદ્ધ ઓર્ગેનિક સીંગતેલ.",
            category = "GROCERY",
            imageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 6,
            nameEn = "Pure Desi Cow Ghee (1L)",
            nameGu = "શુદ્ધ દેશી ગાયનું ઘી (૧ લીટર)",
            price = 650.0,
            isMandatory = false,
            descriptionEn = "A2 premium pure hand-churned Vedic cow ghee.",
            descriptionGu = "A2 પ્રીમિયમ શુદ્ધ હાથથી વલોવેલું વૈદિક ગાયનું ઘી.",
            category = "GROCERY",
            imageUrl = "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 7,
            nameEn = "Premium Basmati Rice (5kg)",
            nameGu = "પ્રીમિયમ બાસમતી ચોખા (૫ કિલો)",
            price = 450.0,
            isMandatory = false,
            descriptionEn = "Long grain aged aromatic Basmati rice for daily use.",
            descriptionGu = "દૈનિક ઉપયોગ માટે લાંબા દાણાવાળા સુગંધિત બાસમતી ચોખા.",
            category = "GROCERY",
            imageUrl = "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 3,
            nameEn = "Cotton Khadi Kurta (Hand-woven)",
            nameGu = "કોટન ખાદી કુર્તા (હાથથી વણેલા)",
            price = 600.0,
            isMandatory = false,
            descriptionEn = "Premium handloom cotton khadi kurta for daily wear.",
            descriptionGu = "દૈનિક વસ્ત્રો માટે પ્રીમિયમ હેન્ડલૂમ કોટન ખાદી કુર્તો.",
            category = "CLOTHING",
            imageUrl = "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 8,
            nameEn = "Premium Khadi Saree (Handloom)",
            nameGu = "પ્રીમિયમ ખાદી સાડી (હેન્ડલૂમ)",
            price = 850.0,
            isMandatory = false,
            descriptionEn = "Elegant hand-woven premium traditional khadi cotton saree.",
            descriptionGu = "ભવ્ય હાથથી વણેલી પ્રીમિયમ પરંપરાગત khadi સુતરાઉ સાડી.",
            category = "CLOTHING",
            imageUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 9,
            nameEn = "Pure Cotton T-Shirt",
            nameGu = "શુદ્ધ સુતરાઉ ટી-શર્ટ (કોટન)",
            price = 299.0,
            isMandatory = false,
            descriptionEn = "Breathable 100% combed cotton comfortable classic round neck T-shirt.",
            descriptionGu = "શ્વાસ લઈ શકાય તેવું ૧૦૦% કોમ્બડ કોટન આરામદાયક ક્લાસિક રાઉન્ડ નેક ટી-શર્ટ.",
            category = "CLOTHING",
            imageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 4,
            nameEn = "Smart Fitness Band",
            nameGu = "સ્માર્ટ ફિટનેસ બેન્ડ",
            price = 1200.0,
            isMandatory = false,
            descriptionEn = "Tracks steps, sleep, and heart rate with 10 days battery life.",
            descriptionGu = "૧૦ દિવસની બેટરી લાઈફ સાથે સ્ટેપ્સ, સ્લીપ અને હાર્ટ રેટ મોનિટર કરે છે.",
            category = "ELECTRONICS",
            imageUrl = "https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 10,
            nameEn = "Dual-Port Fast Mobile Charger",
            nameGu = "ફાસ્ટ મોબાઇલ ચાર્જર (ડ્યુઅલ પોર્ટ)",
            price = 250.0,
            isMandatory = false,
            descriptionEn = "20W rapid charging adapter with short-circuit protection.",
            descriptionGu = "શોર્ટ-સર્કિટ પ્રોટેક્શન સાથે ૨૦W નું ઝડપી ચાર્જિંગ એડેપ્ટર.",
            category = "ELECTRONICS",
            imageUrl = "https://images.unsplash.com/photo-1622445262465-2481c4574875?auto=format&fit=crop&w=300&q=80"
        ),
        Product(
            id = 11,
            nameEn = "Smart LED Bulb (9W)",
            nameGu = "સ્માર્ટ એલઇડી બલ્બ (૯W)",
            price = 180.0,
            isMandatory = false,
            descriptionEn = "Energy saving long-lasting white light LED bulb.",
            descriptionGu = "ઉર્જા બચાવતો લાંબો સમય ચાલતો સફેદ પ્રકાશ આપતો એલઇડી બલ્બ.",
            category = "ELECTRONICS",
            imageUrl = "https://images.unsplash.com/photo-1550985616-10810253b84d?auto=format&fit=crop&w=300&q=80"
        )
    )
}
