package com.example.data.remote.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.remote.RemoteDatabaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class FirebaseDatabaseServiceImpl(private val context: Context) : RemoteDatabaseService {

    // Toggleable network simulation state (user can toggle in App UI for demo purposes!)
    private val _isDemoOnline = MutableStateFlow(true)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // We combine the real Android system network state with the UI's simulation toggle
    override val isOnlineFlow: Flow<Boolean> = flow {
        while (true) {
            val realOnline = isDeviceOnline()
            val demoState = _isDemoOnline.value
            emit(realOnline && demoState)
            delay(3000) // update every 3s
        }
    }

    private fun isDeviceOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun setDemoOnline(online: Boolean) {
        _isDemoOnline.value = online
    }

    fun isDemoOnlineState(): Boolean = _isDemoOnline.value

    override suspend fun checkConnectionDirect(): Boolean {
        return isDeviceOnline() && _isDemoOnline.value
    }

    // High fidelity seed data representing the exact items from the screenshots provided
    private val productsDb = mutableListOf(
        Product(
            id = "p1",
            nameAr = "معالج دقيق أوتوماتيكي Arduino Uno R3 ATmega328P",
            nameEn = "Arduino Uno R3 Microcontroller Board",
            descriptionAr = "متحكم أحادي ذو كفاءة عالية للتحكم بالدوائر اللاسلكية واستشعار المحيط والبرمجة.",
            descriptionEn = "High efficiency microcontroller board perfect for electronics hobbyists and systems integration.",
            imageUrl = "https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400",
            price = 15000.0,
            originalPrice = 18000.0,
            categoryAr = "المعالجات",
            categoryEn = "Processors",
            isFeatured = true,
            isDiscounted = true,
            stock = 15
        ),
        Product(
            id = "p2",
            nameAr = "معالج مصغر لاسلكي ESP32 DevKitC WiFi Bluetooth",
            nameEn = "ESP32 DevKitC WiFi Bluetooth Processor",
            descriptionAr = "معالج دوت تيك فائق الكفاءة مزود بواي فاي مدمج مخصص لمشاريع إنترنت الأشياء والتحكم الذكي.",
            descriptionEn = "Dual-core processor with integrated Wi-Fi and Bluetooth, ideal for IoT designs.",
            imageUrl = "https://images.unsplash.com/photo-1517420712361-2e6d99c3b6ec?w=400",
            price = 9500.0,
            originalPrice = null,
            categoryAr = "المعالجات",
            categoryEn = "Processors",
            isFeatured = true,
            isDiscounted = false,
            stock = 20
        ),
        Product(
            id = "p3",
            nameAr = "شريحة تخزين مغناطيسية EEPROM AT24C256 256Kbit",
            nameEn = "AT24C256 256Kbit I2C Serial EEPROM Memory",
            descriptionAr = "ذاكرة تخزين غير متطايرة دقيقة لحفظ متغيرات تشغيل الأنظمة لوضع عدم الاتصال.",
            descriptionEn = "Compact and high-speed I2C serial EEPROM chip for configuration data storage.",
            imageUrl = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400",
            price = 2500.0,
            originalPrice = 3500.0,
            categoryAr = "الذاكرة",
            categoryEn = "Memory",
            isFeatured = false,
            isDiscounted = true,
            stock = 50
        ),
        Product(
            id = "p4",
            nameAr = "شريحة ذاكرة سريعة SPI FLASH W25Q128 128M-bit",
            nameEn = "W25Q128 128M-bit SPI Flash Memory Module",
            descriptionAr = "شريحة فلاش ميموري فائقة لزيادة مساحة تخزين الأكواد والملفات والصور للمتحكمات.",
            descriptionEn = "External high-capacity flash memory expansion board with SPI interface.",
            imageUrl = "https://images.unsplash.com/photo-1597491847832-475f49e493f0?w=400",
            price = 4500.0,
            originalPrice = null,
            categoryAr = "الذاكرة",
            categoryEn = "Memory",
            isFeatured = true,
            isDiscounted = false,
            stock = 30
        ),
        Product(
            id = "p5",
            nameAr = "شاشة عرض رسومية OLED مقاس 0.96 بوصة I2C زرقاء",
            nameEn = "0.96 inch I2C OLED Display Module Blue",
            descriptionAr = "شاشة OLED متكاملة ذات وضوح ممتاز لعرض نصوص الرصد والرسومات والعدادات الحية.",
            descriptionEn = "Highly readable monochrome organic light-emitting diode graphical display for microcontrollers.",
            imageUrl = "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=400",
            price = 6000.0,
            originalPrice = 7500.0,
            categoryAr = "الملحقات",
            categoryEn = "Peripherals",
            isFeatured = true,
            isDiscounted = true,
            stock = 25
        ),
        Product(
            id = "p6",
            nameAr = "حساس استشعار المسافة بالموجات الصوتية HC-SR04",
            nameEn = "HC-SR04 Ultrasonic Distance Sensor Module",
            descriptionAr = "حساس إلكتروني ذكي للمشاريع مخصص لقياس الأبعاد وكشف العقبات والحواجز.",
            descriptionEn = "Precise non-contact distance measurement module ranging from 2cm to 400cm.",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=400",
            price = 3500.0,
            originalPrice = null,
            categoryAr = "الملحقات",
            categoryEn = "Peripherals",
            isFeatured = false,
            isDiscounted = false,
            stock = 45
        ),
        Product(
            id = "p7",
            nameAr = "لوحة توسعة المنافذ CNC Shield v3 لطابعات ثلاثية الأبعاد",
            nameEn = "CNC Shield v3 Board for 3D Printers / CNC engraving",
            descriptionAr = "لوحة درع تحكم CNC مخصصة لدعم تشغيل محركات الخطوة وتصميم طابعات الماكنات.",
            descriptionEn = "High compatibility CNC expansion board v3 for stepper driver modules and mini router designs.",
            imageUrl = "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400",
            price = 11000.0,
            originalPrice = null,
            categoryAr = "الملحقات",
            categoryEn = "Peripherals",
            isFeatured = true,
            isDiscounted = false,
            stock = 12
        ),
        Product(
            id = "p8",
            nameAr = "لوحة كمبيوتر لوحي Orange Pi Zero 3 RAM 1.5GB",
            nameEn = "Orange Pi Zero 3 Board RAM 1.5GB",
            descriptionAr = "معالج حاسوبي رباعي النواة لتطوير التطبيقات الكبيرة واستضافة الأنظمة البرمجية المتكاملة.",
            descriptionEn = "Powerful 1.5GB DDR4 RAM single-board computer, with H618 processor, Wi-Fi, and Ethernet.",
            imageUrl = "https://images.unsplash.com/photo-1563770660941-20978e870e26?w=400",
            price = 45000.0,
            originalPrice = 50000.0,
            categoryAr = "المعالجات",
            categoryEn = "Processors",
            isFeatured = true,
            isDiscounted = true,
            stock = 8
        ),
        Product(
            id = "p9",
            nameAr = "شاحن ذكي ومراقب خلايا بطاريات الليثيوم 8-100 فولت",
            nameEn = "DC 8-100V Battery Level Monitor Battery Capacity Indicator",
            descriptionAr = "شاشة معايرة ذكية لقراءة نسبة فولتية بطاريات الليثيوم والرصاص في مشاريع الطاقة.",
            descriptionEn = "Intelligent battery capacity indicator and checker with vivid LCD digital backlighting.",
            imageUrl = "https://images.unsplash.com/photo-1590301157890-4810ed352733?w=400",
            price = 8500.0,
            originalPrice = null,
            categoryAr = "البطاريات ومستلزماتها",
            categoryEn = "Battery & Accessories",
            isFeatured = false,
            isDiscounted = false,
            stock = 18
        ),
        Product(
            id = "p10",
            nameAr = "بطارية ليثيوم Tipsun أصلية 18650 سعة 2600 مللي أمبير",
            nameEn = "Tipsun 18650 Lithium Battery cell 2600mAh",
            descriptionAr = "خلية بطارية ليثيوم أيون ذات كفاءة ممتازة ومستمرة تدعم مشاريع الأداء والراوتر والربوتات.",
            descriptionEn = "High density rechargeable lithium cell with protective circuit for continuous tech loads.",
            imageUrl = "https://images.unsplash.com/photo-1601524909162-be87252be298?w=400",
            price = 4000.0,
            originalPrice = 5000.0,
            categoryAr = "البطاريات ومستلزماتها",
            categoryEn = "Battery & Accessories",
            isFeatured = true,
            isDiscounted = true,
            stock = 60
        ),
        Product(
            id = "p11",
            nameAr = "مجهز قدرة محول تيار بور سبلاي 12 فولت 20 أمبير",
            nameEn = "12V 20A Industrial Switching Power Supply",
            descriptionAr = "بور سبلاي صناعي عالي التحمل مخصص لمعايرة وتجهيز القطع الإلكترونية والمحركات بجهد ثابت.",
            descriptionEn = "Robust stabilized industrial transformer perfect for laboratory prototyping and testing.",
            imageUrl = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400",
            price = 28000.0,
            originalPrice = null,
            categoryAr = "العناصر الإلكترونية",
            categoryEn = "Components",
            isFeatured = false,
            isDiscounted = false,
            stock = 10
        )
    )

    // Store orders uploaded to our "Firebase" backend
    private val ordersDb = mutableListOf<Order>()

    // Store registered users synced to "Firebase" backend
    private val usersDb = mutableListOf<com.example.data.model.UserProfile>(
        com.example.data.model.UserProfile(
            id = "u1",
            username = "م. حيدر المياحي",
            phone = "7701234455",
            countryCode = "+964",
            location = "العراق، بغداد، الجادرية، قرب جامعة بغداد",
            avatarIndex = 4
        ),
        com.example.data.model.UserProfile(
            id = "u2",
            username = "م. مصطفى العبيدي",
            phone = "7718882233",
            countryCode = "+964",
            location = "العراق، بغداد، الكرادة، قرب كافي شوب رضا علوان",
            avatarIndex = 1
        ),
        com.example.data.model.UserProfile(
            id = "u3",
            username = "ألاء عبد الرحمن",
            phone = "7829876543",
            countryCode = "+964",
            location = "العراق، نينوى، الموصل، حي الزهور",
            avatarIndex = 3
        ),
        com.example.data.model.UserProfile(
            id = "u4",
            username = "زين العابدين علي",
            phone = "7504443322",
            countryCode = "+964",
            location = "GPS: خط عرض 36.1912، خط طول 44.0091، أربيل، عينكاوة",
            avatarIndex = 0
        )
    )

    override suspend fun getProductsOnline(): List<Product> {
        delay(1500) // simulate real HTTP network delay
        if (!checkConnectionDirect()) throw Exception("Online sync failed: Firebase is offline!")
        return productsDb.toList()
    }

    override suspend fun uploadOrder(order: Order): Boolean {
        delay(2000) // simulate Firebase Firestore upload lag
        if (!checkConnectionDirect()) return false
        ordersDb.add(order.copy(isSynced = true))
        return true
    }

    override suspend fun uploadProductOnline(product: Product): Boolean {
        delay(1500) // simulate Firebase Firestore network lag
        if (!checkConnectionDirect()) return false
        val index = productsDb.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            productsDb[index] = product
        } else {
            productsDb.add(product)
        }
        return true
    }

    override suspend fun uploadUserProfileOnline(
        username: String,
        phone: String,
        countryCode: String,
        location: String,
        avatarIndex: Int
    ): Boolean {
        delay(1200) // simulate Firebase network delay
        if (!checkConnectionDirect()) return false
        val userId = "user_${phone.filter { it.isDigit() }.takeLast(4)}"
        val profile = com.example.data.model.UserProfile(
            id = userId,
            username = username,
            phone = phone,
            countryCode = countryCode,
            location = location,
            avatarIndex = avatarIndex
        )
        val index = usersDb.indexOfFirst { it.phone == phone && phone.isNotBlank() }
        if (index >= 0) {
            usersDb[index] = profile
        } else {
            usersDb.add(profile)
        }
        return true
    }

    override suspend fun getCustomersOnline(): List<com.example.data.model.UserProfile> {
        delay(1000) // simulate loading customers
        if (!checkConnectionDirect()) throw Exception("Online sync failed: Firebase is offline!")
        return usersDb.toList()
    }
}
