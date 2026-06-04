package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.data.model.*
import com.example.ui.viewmodel.ECommerceViewModel
import com.example.ui.viewmodel.AppThemeMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountScreen(
    viewModel: ECommerceViewModel
) {
    val orders by viewModel.ordersList.collectAsState()
    val isOnlineState by viewModel.isOnline.collectAsState()
    val onlineCustomers by viewModel.onlineCustomers.collectAsState()

    val currentUsername by viewModel.username.collectAsState()
    val currentUserPhone by viewModel.userPhone.collectAsState()
    val currentUserCountryCode by viewModel.userCountryCode.collectAsState()
    val currentUserLocation by viewModel.userLocation.collectAsState()
    val isUserRegistered by viewModel.isUserRegistered.collectAsState()
    val userAvatarIndex by viewModel.userAvatarIndex.collectAsState()

    var activeSheet by remember { mutableStateOf<AccountSheet?>(null) }

    var formName by remember(activeSheet, currentUsername) { mutableStateOf(currentUsername) }
    var formPhone by remember(activeSheet, currentUserPhone) { mutableStateOf(currentUserPhone) }
    var formCountryCode by remember(activeSheet, currentUserCountryCode) { mutableStateOf(currentUserCountryCode) }
    var formLocation by remember(activeSheet, currentUserLocation) { mutableStateOf(currentUserLocation) }
    var formAvatarIdx by remember(activeSheet, userAvatarIndex) { mutableStateOf(userAvatarIndex) }
    var isLocatingGps by remember { mutableStateOf(false) }

    // Admin Authenticated State
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordError by remember { mutableStateOf(false) }

    // Admin Product Form State Variables
    var adminProductId by remember { mutableStateOf("") }
    var adminProductNameAr by remember { mutableStateOf("") }
    var adminProductNameEn by remember { mutableStateOf("") }
    var adminProductDescAr by remember { mutableStateOf("") }
    var adminProductDescEn by remember { mutableStateOf("") }
    var adminProductPrice by remember { mutableStateOf("") }
    var adminProductStock by remember { mutableStateOf("10") }
    var adminProductCategoryAr by remember { mutableStateOf("المعالجات") }
    var adminProductCategoryEn by remember { mutableStateOf("Processors") }
    var adminProductImageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400") }
    var adminProductIsDiscounted by remember { mutableStateOf(false) }
    var adminProductOriginalPrice by remember { mutableStateOf("") }

    val avatarIcon = when (userAvatarIndex) {
        0 -> Icons.Filled.Person
        1 -> Icons.Filled.Engineering
        2 -> Icons.Filled.Memory
        3 -> Icons.Filled.Bolt
        4 -> Icons.Filled.DeveloperBoard
        else -> Icons.Filled.Person
    }

    Crossfade(targetState = activeSheet, label = "account_navigation") { currentActiveSheet ->
        if (currentActiveSheet == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
        // 1. Account / Customer Profile Card (Dynamic user representation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { activeSheet = AccountSheet.ProfileSettings },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile options button (triple dot)
                IconButton(onClick = { activeSheet = AccountSheet.ProfileSettings }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Profile Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isUserRegistered) {
                                Box(
                                    modifier = Modifier
                                        .background(ShopGreenContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "مسجل سحابياً",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShopGreenDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "حساب ضيف",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = currentUsername,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUserPhone.isBlank()) "لم يتم ربط رقم هاتف بعد" else "$currentUserCountryCode $currentUserPhone",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = "Avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Demo simulation panel (A beautiful testing panel that lets anyone test offline browsing!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnlineState) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isOnlineState) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isOnlineState,
                        onCheckedChange = { viewModel.toggleDemoConnectivity() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Text(
                        text = "محاكاة حالة الاتصال بالإنترنت",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isOnlineState)
                        "الهاتف متصل بالخادوم (مزامنة فورية وتحديثات الأسعار نشطة)"
                    else
                        "وضع عطل الشبكة مفعل (تصفح كاش كلي، حفظ طلبات الـ COD لتتم مزامنتها تلقائياً عند الاتصال)",
                    fontSize = 11.sp,
                    color = if (isOnlineState) ShopGreenDark else DiscountRed,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isOnlineState) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.toggleDemoConnectivity() },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("الاتصال الآن للمزامنة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Mode Selector Card
        val themeModeState by viewModel.themeMode.collectAsState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "مظهر التطبيق (ثيم)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اختر السمة واللون المفضل لديك لتصفح مريح للقطع الإلكترونية والمتحكمات",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System theme mode button
                    ThemeOptionButton(
                        text = "تلقائي",
                        icon = Icons.Filled.Settings,
                        selected = themeModeState == AppThemeMode.System,
                        onClick = { viewModel.setThemeMode(AppThemeMode.System) },
                        modifier = Modifier.weight(1f)
                    )

                    // Light theme mode button
                    ThemeOptionButton(
                        text = "مضيء",
                        icon = Icons.Filled.LightMode,
                        selected = themeModeState == AppThemeMode.Light,
                        onClick = { viewModel.setThemeMode(AppThemeMode.Light) },
                        modifier = Modifier.weight(1f)
                    )

                    // Dark theme mode button
                    ThemeOptionButton(
                        text = "داكن",
                        icon = Icons.Filled.DarkMode,
                        selected = themeModeState == AppThemeMode.Dark,
                        onClick = { viewModel.setThemeMode(AppThemeMode.Dark) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Option lists matching screenshot aesthetics
        AccountOptionRow(
            title = "بيانات حسابي وتأكيد التسجيل",
            icon = Icons.Filled.Person,
            onClick = { activeSheet = AccountSheet.ProfileSettings }
        )

        AccountOptionRow(
            title = "طلباتي شحن COD",
            icon = Icons.Filled.LocalShipping,
            onClick = { activeSheet = AccountSheet.MyOrders }
        )

        AccountOptionRow(
            title = "إتصل بنا",
            icon = Icons.Filled.Phone,
            onClick = { activeSheet = AccountSheet.Contact }
        )

        AccountOptionRow(
            title = "سياسة الخصوصية",
            icon = Icons.Filled.PrivacyTip,
            onClick = { activeSheet = AccountSheet.Privacy }
        )

        AccountOptionRow(
            title = "الشروط والأحكام",
            icon = Icons.Filled.FactCheck,
            onClick = { activeSheet = AccountSheet.Terms }
        )

        AccountOptionRow(
            title = "لوحة تحكم إدارة مستودع المتجر (Admin)",
            icon = Icons.Filled.Settings,
            onClick = { activeSheet = AccountSheet.AdminPanel }
        )

        AccountOptionRow(
            title = "تسجيل الخروج",
            icon = Icons.Filled.Logout,
            onClick = { activeSheet = AccountSheet.Logout }
        )
    }
} else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Professional Header for the Forwarded Page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { activeSheet = null }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = when (currentActiveSheet) {
                    AccountSheet.MyOrders -> "سجل طلباتي (COD)"
                    AccountSheet.Contact -> "قنوات الاتصال بنا"
                    AccountSheet.Privacy -> "سياسة خصوصية البيانات"
                    AccountSheet.Terms -> " الشروط والأحكام العامة"
                    AccountSheet.Logout -> "تسجيل الخروج"
                    AccountSheet.ProfileSettings -> "تفاصيل ضبط الحساب"
                    AccountSheet.AdminPanel -> "لوحة تحكم إدارة المتجر (Admin)"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        // Full Page Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            when (currentActiveSheet) {
                        AccountSheet.ProfileSettings -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "قم بتعديل وتأكيد بيانات حسابك. عند الشراء، سيتم ربط هذه البيانات تلقائياً بطلبك COD.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Select Avatar Option
                                Text(
                                    text = "اختر أيقونة الحساب (اختياري)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    (0..4).forEach { idx ->
                                        val icon = when (idx) {
                                            0 -> Icons.Filled.Person
                                            1 -> Icons.Filled.Settings
                                            2 -> Icons.Filled.Memory
                                            3 -> Icons.Filled.Phone
                                            4 -> Icons.Filled.DeveloperBoard
                                            else -> Icons.Filled.Person
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (formAvatarIdx == idx) MaterialTheme.colorScheme.primaryContainer 
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    width = if (formAvatarIdx == idx) 2.dp else 1.dp,
                                                    color = if (formAvatarIdx == idx) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                                .clickable { formAvatarIdx = idx },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (formAvatarIdx == idx) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Username Input
                                Text(
                                    text = "اسم مستخدم الحساب *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = formName,
                                    onValueChange = { formName = it },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Phone Country Code & Phone Number Row
                                Text(
                                    text = "رقم الهاتف الفعال مع رمز الدولة *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = formPhone,
                                        onValueChange = { formPhone = it },
                                        singleLine = true,
                                        placeholder = { Text("78XXXXXXXX", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(56.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .clickable {
                                                formCountryCode = when (formCountryCode) {
                                                    "+964" -> "+966"
                                                    "+966" -> "+962"
                                                    "+962" -> "+971"
                                                    "+971" -> "+20"
                                                    else -> "+964"
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val flag = when (formCountryCode) {
                                            "+964" -> "🇮🇶 "
                                            "+966" -> "🇸🇦 "
                                            "+962" -> "🇯🇴 "
                                            "+971" -> "🇦🇪 "
                                            "+20" -> "🇪🇬 "
                                            else -> "🌐 "
                                        }
                                        Text(
                                            text = "$flag$formCountryCode",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "اضغط على مربع الرمز لتغيير الدولة (العراق 🇮🇶، السعودية 🇸🇦، الأردن 🇯🇴، الإمارات 🇦🇪، مصر 🇪🇬)",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Location Picker Section
                                Text(
                                    text = "عنوان وموقع سكن المستلم للأجهزة والقطع *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = formLocation,
                                    onValueChange = { formLocation = it },
                                    singleLine = false,
                                    maxLines = 3,
                                    placeholder = { Text("اكتب عنوانك بالتفصيل (مثل: بغداد، الكرادة، ساحة التحريات)", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Beautiful interactive GIS mockup for GPS Simulator
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLocatingGps) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("يرتبط بمستشعرات الأقمار الصناعية GPS...", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            } else {
                                                Column(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    if (formLocation.contains("GPS:")) {
                                                        Text("تم قراءة مستشعر GPS التلقائي بنجاح!", fontSize = 10.sp, color = ShopGreenDark, fontWeight = FontWeight.Bold)
                                                        Text(formLocation, fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                                    } else {
                                                        Text("اضغط على الزر أدناه لجلب موقعك الحالي الفوري عبر الـ GPS تلقائياً", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        ElevatedButton(
                                            onClick = {
                                                isLocatingGps = true
                                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                    isLocatingGps = false
                                                    val lat = String.format(Locale.US, "%.4f", 33.3152 + (Math.random() - 0.5) * 0.02)
                                                    val lon = String.format(Locale.US, "%.4f", 44.4443 + (Math.random() - 0.5) * 0.02)
                                                    formLocation = "GPS: خط عرض $lat، خط طول $lon، شارع الصناعة، بغداد، العراق"
                                                }, 1200)
                                            },
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = !isLocatingGps,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("جلب الموقع التلقائي (مستشعر GPS)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (formName.isBlank()) {
                                                android.widget.Toast.makeText(context, "الرجاء كتابة اسم الحساب بشكل صحيح", android.widget.Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            viewModel.updateUserProfile(
                                                name = formName,
                                                phone = formPhone,
                                                countryCode = formCountryCode,
                                                location = formLocation,
                                                isRegistered = true,
                                                avatarIdx = formAvatarIdx
                                            )
                                            activeSheet = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (isUserRegistered) "حفظ التعديلات وتحديث السحابة" else "تأكيد الحساب ومزامنة السحابة",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { activeSheet = null },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("إلغاء", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        AccountSheet.AdminPanel -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val adminProducts by viewModel.productsFlow.collectAsState(initial = emptyList())
                            
                            if (!isAdminAuthenticated) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "المصادقة الأمنية لمدير المتجر",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "قم بإدخال كلمة المرور السرية لفتح مستندات المستودع وإدارة كميات ومواصفات الأجهزة الإلكترونية لجميع العملاء.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "تنويه للمهندس المجرب (Demo Mode)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Right
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "كلمة مرور المدير الافتراضية هي: admin123",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    OutlinedTextField(
                                        value = adminPasswordInput,
                                        onValueChange = { 
                                            adminPasswordInput = it
                                            adminPasswordError = false
                                        },
                                        isError = adminPasswordError,
                                        singleLine = true,
                                        label = { Text("رمز الدخول السري *", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            errorBorderColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = null,
                                                tint = if (adminPasswordError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                    
                                    if (adminPasswordError) {
                                        Text(
                                            text = "رمز الدخول غير صحيح! يرجى كتابة admin123 للتجربة.",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(18.dp))
                                    
                                    Button(
                                        onClick = {
                                            if (adminPasswordInput.trim() == "admin123") {
                                                isAdminAuthenticated = true
                                                adminPasswordError = false
                                                if (adminProductId.isBlank()) {
                                                    adminProductId = "p_${System.currentTimeMillis()}"
                                                }
                                            } else {
                                                adminPasswordError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "تسجيل الدخول والنفاذ للمستودع",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            } else {
                                var adminTab by remember { mutableStateOf(0) }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ElevatedButton(
                                            onClick = { 
                                                adminTab = 2
                                                viewModel.loadOnlineCustomers()
                                            },
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = if (adminTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (adminTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("العملاء", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(13.dp))
                                            }
                                        }

                                        ElevatedButton(
                                            onClick = { adminTab = 1 },
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = if (adminTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (adminTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("جرد المستودع", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(13.dp))
                                            }
                                        }

                                        ElevatedButton(
                                            onClick = { adminTab = 0 },
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = if (adminTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (adminTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.3f),
                                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if (adminProductId.startsWith("p_")) "إضافة جهاز" else "تعديل جهاز", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (adminTab == 0) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (adminProductId.startsWith("p_")) MaterialTheme.colorScheme.secondaryContainer 
                                                            else ShopGreenContainer, 
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (adminProductId.startsWith("p_")) "إضافة جهاز لاسلكي أو قطعة جديدة" else "تعديل كمية وسعر المنتج القائم",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (adminProductId.startsWith("p_")) MaterialTheme.colorScheme.onSecondaryContainer else ShopGreenDark
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = "معرف: ${adminProductId.take(12)}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(
                                                text = "الاسم باللغة العربية (سلسلة الرصد والبيع) *",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            OutlinedTextField(
                                                value = adminProductNameAr,
                                                onValueChange = { adminProductNameAr = it },
                                                singleLine = true,
                                                placeholder = { Text("مثال: لوحة معالج ESP8266 مدمج", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "Product Name (English Spec) *",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            OutlinedTextField(
                                                value = adminProductNameEn,
                                                onValueChange = { adminProductNameEn = it },
                                                singleLine = true,
                                                placeholder = { Text("Example: NodeMCU ESP12E Wireless Module", textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth()) },
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Left),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "كمية المخزن المستودعي *",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    OutlinedTextField(
                                                        value = adminProductStock,
                                                        onValueChange = { newValue ->
                                                            if (newValue.all { it.isDigit() }) adminProductStock = newValue
                                                        },
                                                        singleLine = true,
                                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "سعر البيع IQD *",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    OutlinedTextField(
                                                        value = adminProductPrice,
                                                        onValueChange = { newValue ->
                                                            if (newValue.all { it.isDigit() || it == '.' }) adminProductPrice = newValue
                                                        },
                                                        singleLine = true,
                                                        suffix = { Text("ع.ع", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) },
                                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "فئة تصنيف القطعة الالكترونية *",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            val cats = listOf(
                                                Triple("المعالجات", "Processors", Icons.Filled.Memory),
                                                Triple("الذاكرة", "Memory", Icons.Filled.Storage),
                                                Triple("الملحقات", "Peripherals", Icons.Filled.DeveloperBoard),
                                                Triple("البطاريات ومستلزماتها", "Battery & Accessories", Icons.Filled.Bolt),
                                                Triple("العناصر الإلكترونية", "Components", Icons.Filled.Settings)
                                            )
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                cats.forEach { (catAr, catEn, icon) ->
                                                    val isSelected = adminProductCategoryAr == catAr
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable {
                                                                adminProductCategoryAr = catAr
                                                                adminProductCategoryEn = catEn
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Text(catAr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "الوصف والمواصفات بالعربية (شاشات العرض) *",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            OutlinedTextField(
                                                value = adminProductDescAr,
                                                onValueChange = { adminProductDescAr = it },
                                                singleLine = false,
                                                maxLines = 2,
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "Product Description & Specs (English)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            OutlinedTextField(
                                                value = adminProductDescEn,
                                                onValueChange = { adminProductDescEn = it },
                                                singleLine = false,
                                                maxLines = 2,
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Left),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "صورة المنتج (اختر صورة تجريبية أو الصق رابط مخصص) *",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = adminProductImageUrl,
                                                onValueChange = { adminProductImageUrl = it },
                                                singleLine = true,
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Left),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Spacer(modifier = Modifier.height(6.dp))

                                            val imagePresets = listOf(
                                                "https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400",
                                                "https://images.unsplash.com/photo-1517420712361-2e6d99c3b6ec?w=400",
                                                "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400",
                                                "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=400"
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                imagePresets.forEachIndexed { pIdx, url ->
                                                    val isSelected = adminProductImageUrl == url
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = 8.dp)
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { adminProductImageUrl = url },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = when(pIdx) {
                                                                0 -> Icons.Filled.Memory
                                                                1 -> Icons.Filled.DeveloperBoard
                                                                2 -> Icons.Filled.Storage
                                                                else -> Icons.Filled.Settings
                                                            },
                                                            contentDescription = null,
                                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Text("روابط سريعة: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        adminProductId = "p_${System.currentTimeMillis()}"
                                                        adminProductNameAr = ""
                                                        adminProductNameEn = ""
                                                        adminProductDescAr = ""
                                                        adminProductDescEn = ""
                                                        adminProductPrice = ""
                                                        adminProductStock = "10"
                                                        adminProductImageUrl = "https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400"
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("استمارة جديدة", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val priceDouble = adminProductPrice.toDoubleOrNull() ?: 0.0
                                                        val stockInt = adminProductStock.toIntOrNull() ?: 10
                                                        
                                                        if (adminProductNameAr.isBlank() || adminProductNameEn.isBlank() || priceDouble <= 0.0) {
                                                            android.widget.Toast.makeText(context, "الرجاء كتابة الاسم والسعر بوضوح", android.widget.Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }

                                                        viewModel.addOrUpdateProduct(
                                                            id = adminProductId,
                                                            nameAr = adminProductNameAr,
                                                            nameEn = adminProductNameEn,
                                                            descriptionAr = adminProductDescAr,
                                                            descriptionEn = adminProductDescEn,
                                                            imageUrl = adminProductImageUrl,
                                                            price = priceDouble,
                                                            stock = stockInt,
                                                            categoryAr = adminProductCategoryAr,
                                                            categoryEn = adminProductCategoryEn,
                                                            onComplete = { success ->
                                                                if (success) {
                                                                    adminProductId = "p_${System.currentTimeMillis()}"
                                                                    adminProductNameAr = ""
                                                                    adminProductNameEn = ""
                                                                    adminProductDescAr = ""
                                                                    adminProductDescEn = ""
                                                                    adminProductPrice = ""
                                                                    adminProductStock = "10"
                                                                    adminProductImageUrl = "https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400"
                                                                }
                                                            }
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("رفع وتأكيد بالتحديث السحابي", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }
                                    } else if (adminTab == 1) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "قائمة القطع والكميات المحجوزة بالمستودع",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "اضغط على أي عنصر لقراءة وملء بيانات الاستمارة من أجل تعديل سعر البيع أو تحديث كمية المخزون وسعة شحنه.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            if (adminProducts.isEmpty()) {
                                                Text("لا يوجد منتجات بالمستودع حالياً", fontSize = 11.sp, modifier = Modifier.padding(16.dp))
                                            } else {
                                                adminProducts.forEach { prod ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable {
                                                                adminProductId = prod.id
                                                                adminProductNameAr = prod.nameAr
                                                                adminProductNameEn = prod.nameEn
                                                                adminProductDescAr = prod.descriptionAr
                                                                adminProductDescEn = prod.descriptionEn
                                                                adminProductPrice = prod.price.toString()
                                                                adminProductStock = prod.stock.toString()
                                                                adminProductCategoryAr = prod.categoryAr
                                                                adminProductCategoryEn = prod.categoryEn
                                                                adminProductImageUrl = prod.imageUrl
                                                                adminTab = 0
                                                                android.widget.Toast.makeText(context, "تم نقل البيانات للاستمارة للتعديل!", android.widget.Toast.LENGTH_SHORT).show()
                                                            },
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.End,
                                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                            ) {
                                                                Text(prod.nameAr, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Right)
                                                                Text(prod.nameEn, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Left)
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                                                    val isStockLow = prod.stock <= 5
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(
                                                                                if (isStockLow) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else ShopGreenContainer, 
                                                                                RoundedCornerShape(4.dp)
                                                                            )
                                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "الكمية: ${prod.stock} قطع",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isStockLow) MaterialTheme.colorScheme.error else ShopGreenDark
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text("${prod.price.toInt()} ع.ع", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                                                }
                                                            }
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.DeveloperBoard,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (adminTab == 2) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.loadOnlineCustomers() }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = "بيانات وحسابات العملاء النشطة بالمزامنة",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "يتم مزامنة ورصد بيانات المشتركين والـ GPS المتزامن مع Firebase في الوقت الفعلي فور تفعيل الاتصال.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            if (onlineCustomers.isEmpty()) {
                                                Text(
                                                    text = "لا توجد سجلات منشطة أو Firebase غير متصل بالشبكة حالياً.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            } else {
                                                onlineCustomers.forEach { customer ->
                                                    val avatarIcon = when (customer.avatarIndex) {
                                                        0 -> Icons.Filled.Person
                                                        1 -> Icons.Filled.Settings
                                                        2 -> Icons.Filled.Memory
                                                        3 -> Icons.Filled.Phone
                                                        4 -> Icons.Filled.DeveloperBoard
                                                        else -> Icons.Filled.Person
                                                    }
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.End,
                                                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(ShopGreenContainer, RoundedCornerShape(4.dp))
                                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "سحابي متصل 🟢",
                                                                            fontSize = 8.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = ShopGreenDark
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = customer.username,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(3.dp))
                                                                Text(
                                                                    text = "الهاتف: ${customer.countryCode} ${customer.phone}",
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.End,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Text(
                                                                        text = customer.location,
                                                                        fontSize = 9.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 2,
                                                                        textAlign = TextAlign.Right,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Icon(
                                                                        imageVector = Icons.Filled.LocationOn,
                                                                        contentDescription = null,
                                                                        tint = DiscountRed,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                }
                                                            }
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = avatarIcon,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
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
                        AccountSheet.MyOrders -> {
                            if (orders.isEmpty()) {
                                Text(
                                    text = "لم تقم بإنشاء أي طلبات شراء بعد.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                orders.forEach { order ->
                                    OrderItemCard(order = order)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        AccountSheet.Contact -> {
                            Text(
                                text = "يسرنا خدمتك عبر مكتبنا الرئيسي في العراق وفي فروعنا الإلكترونية المتعددة. تواصل معنا على الرقم:\n\n📱 هاتف الدعم الفني: 07841703018\n📧 بريد المعايرة: cs@electronic-city.iq\n📍 الموقع: بغداد - شارع الصناعة - مجمع المهندسين الكهروميكانيكي",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AccountSheet.Privacy -> {
                            Text(
                                text = "حماية بياناتك الشخصية وتفاصيل طلبات الشحن COD تندرج ضمن أولويات تطبيق إلكترونك سيتي. نقوم بحفظ تفاصيل الطلبات في جهازك محلياً لضمان إمكانية العمل الكامل دون إنترنت، ويتم إرسالها فقط وبشكل مشفر ومأمون لخوادم الـ Firebase والـ Supabase المعتمدة لإيصال الطرود.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AccountSheet.Terms -> {
                            Text(
                                text = "١. جميع طلبات الشراء داخل متجر إلكترونك سيتي يتم دفع مبالغها بنظام الدفع عند الاستلام نقداً (Cash On Delivery - COD).\n٢. يحق للعميل فحص القطع والمتحكمات الإلكترونية قبل التسديد.\n٣. في حال تم طلب منتج في وضع عدم الاتصال (Offline Zone)، سيتم توقيف حالة تسليم الشحنة حتى عودة التغطية ومزامنة طردك تلقائياً.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AccountSheet.Logout -> {
                            Text(
                                text = "بالنقر على تأكيد الخروج سيتم إلغاء جلسة التصفح المحلية، ويفقد الهاتف بيانات الجلسة المؤقتة. سيتم الاحتفاظ بسجل طلبات COD وكتالوج القطع محلياً.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { activeSheet = null },
                                colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Text("تأكيد تسجيل الخروج", fontSize = 12.sp, color = Color.White)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun AccountOptionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Forward arrow icon representing navigation potential
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            // Label and Leading Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: com.example.data.model.OrderWithItems) {
    val dateString = try {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
        sdf.format(Date(order.timestamp))
    } catch (e: Exception) {
        "قيد الطلب"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sync Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (order.isSynced) ShopGreenContainer else Color(0xFFFFF3E0))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (order.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = "Sync state",
                            tint = if (order.isSynced) ShopGreenDark else Color(0xFFE65100),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (order.isSynced) "متزامن خادومياً" else "محفوظ محلياً دون اتصال",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (order.isSynced) ShopGreenDark else Color(0xFFE65100)
                        )
                    }
                }

                // Reference Id code
                Text(
                    text = "طلب #${order.id.take(6).uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "تاريخ الطلب: $dateString", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "المستلم: ${order.customerName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "الهاتف: ${order.customerPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "عنوان الشحن: ${order.customerAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Items come pre-fetched via OrderWithItems @Relation — no JSON parsing
            val itemsList = remember(order.items) {
                order.items.map { item ->
                    Triple(item.getName(isArabic = true), item.price, item.quantity)
                }
            }

            if (itemsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تفاصيل المشتريات:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    textAlign = TextAlign.Right
                )
                itemsList.forEach { (name, price, qty) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatPrice(price * qty)} د.ع",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$name (x$qty)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Beautiful Timeline Stepper
            Text(
                text = "حالة شحن وتوصيل الطلب:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                textAlign = TextAlign.Right
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentStatus = order.statusAr
                val steps = listOf("تأكيد", "تجهيز", "شحن COD", "تسليم")
                val activeStepIndex = when (currentStatus) {
                    "قيد الانتظار" -> 0
                    "قيد المعالجة" -> 1
                    "قيد الشحن" -> 2
                    "تم التوصيل" -> 3
                    else -> 0
                }

                steps.forEachIndexed { index, stepName ->
                    val isCompleted = index <= activeStepIndex
                    val stepColor = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(stepColor.copy(alpha = 0.15f))
                                .border(1.dp, stepColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Completed step",
                                    tint = stepColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(stepColor)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stepName,
                            fontSize = 10.sp,
                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (index < steps.size - 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(if (index < activeStepIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatPrice(order.totalPrice)} د.ع",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "المجموع الإجمالي المطلوب (COD):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

enum class AccountSheet {
    MyOrders, Contact, Privacy, Terms, Logout, ProfileSettings, AdminPanel
}

@Composable
fun ThemeOptionButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = if (selected) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
