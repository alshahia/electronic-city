package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.data.model.*
import com.example.ui.components.EmptyState
import com.example.ui.locals.LocalAppActivity
import com.example.ui.locals.LocalWindowSizeClass
import com.example.ui.locals.LocaleManager
import com.example.ui.locals.LocaleResources
import com.example.ui.haptics.rememberHapticClick
import com.example.ui.viewmodel.AppViewModels
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.MessageBus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountScreen(
    vms: AppViewModels
) {
    val orders by vms.order.ordersList.collectAsState()
    val isOnlineState by vms.shop.isOnline.collectAsState()
    val onlineCustomers by vms.userProfile.onlineCustomers.collectAsState()

    val currentUsername by vms.userProfile.username.collectAsState()
    val currentUserPhone by vms.userProfile.userPhone.collectAsState()
    val currentUserCountryCode by vms.userProfile.userCountryCode.collectAsState()
    val currentUserLocation by vms.userProfile.userLocation.collectAsState()
    val isUserRegistered by vms.userProfile.isUserRegistered.collectAsState()
    val userAvatarIndex by vms.userProfile.userAvatarIndex.collectAsState()

    var activeSheet by remember { mutableStateOf<AccountSheet?>(null) }

    var formName by remember(activeSheet, currentUsername) { mutableStateOf(currentUsername) }
    var formPhone by remember(activeSheet, currentUserPhone) { mutableStateOf(currentUserPhone) }
    var formCountryCode by remember(activeSheet, currentUserCountryCode) { mutableStateOf(currentUserCountryCode) }
    var formLocation by remember(activeSheet, currentUserLocation) { mutableStateOf(currentUserLocation) }
    var formAvatarIdx by remember(activeSheet, userAvatarIndex) { mutableStateOf(userAvatarIndex) }
    var isLocatingGps by remember { mutableStateOf(false) }

    // Admin Authenticated State — D8.23 / Phase 7B-2
    // The hardcoded `admin123` check used to live here as a `remember`
    // and a literal `equals`. It now belongs to `AdminAuthViewModel`,
    // which survives `Activity.recreate()` so the gate auto-redirects
    // to the admin tabs across locale switches.
    val isAdminAuthenticated by vms.adminAuth.isAuthenticated.collectAsState()
    val adminAuthenticating by vms.adminAuth.isAuthenticating.collectAsState()
    val adminLockoutUntil by vms.adminAuth.lockoutUntil.collectAsState()
    val adminAuthError by vms.adminAuth.errorMessage.collectAsState()
    var adminPasswordInput by remember { mutableStateOf("") }

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
                        contentDescription = stringResource(R.string.cd_edit_profile),
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
                                        text = stringResource(R.string.account_badge_registered),
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
                                        text = stringResource(R.string.account_badge_guest),
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
                            text = if (currentUserPhone.isBlank()) stringResource(R.string.account_phone_unbound) else stringResource(R.string.account_phone_bound, currentUserCountryCode, currentUserPhone),
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
                            contentDescription = stringResource(R.string.cd_avatar),
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
                        onCheckedChange = { vms.order.toggleDemoConnectivity() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Text(
                        text = stringResource(R.string.account_sim_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isOnlineState)
                        stringResource(R.string.account_sim_online_desc)
                    else
                        stringResource(R.string.account_sim_offline_desc),
                    fontSize = 11.sp,
                    color = if (isOnlineState) ShopGreenDark else DiscountRed,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isOnlineState) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { vms.order.toggleDemoConnectivity() },
                        colors = ButtonDefaults.buttonColors(containerColor = DiscountRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(stringResource(R.string.account_sim_action_connect), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Mode Selector Card
        val themeModeState by vms.theme.themeMode.collectAsState()

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
                    text = stringResource(R.string.account_theme_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.account_theme_subtitle),
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
                        text = stringResource(R.string.account_theme_system),
                        icon = Icons.Filled.Settings,
                        selected = themeModeState == AppThemeMode.System,
                        onClick = { vms.theme.setThemeMode(AppThemeMode.System) },
                        modifier = Modifier.weight(1f)
                    )

                    // Light theme mode button
                    ThemeOptionButton(
                        text = stringResource(R.string.account_theme_light),
                        icon = Icons.Filled.LightMode,
                        selected = themeModeState == AppThemeMode.Light,
                        onClick = { vms.theme.setThemeMode(AppThemeMode.Light) },
                        modifier = Modifier.weight(1f)
                    )

                    // Dark theme mode button
                    ThemeOptionButton(
                        text = stringResource(R.string.account_theme_dark),
                        icon = Icons.Filled.DarkMode,
                        selected = themeModeState == AppThemeMode.Dark,
                        onClick = { vms.theme.setThemeMode(AppThemeMode.Dark) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // D8.19 — in-app language switcher. Mirrors the theme card layout
        // so the two app-level settings read as a matched pair. Reads the
        // current tag via LocaleManager (which writes the SharedPreferences
        // entry that attachBaseContext in MainActivity consults) and calls
        // MainActivity.setAppLocale to persist + recreate.
        val activity = LocalAppActivity.current
        val currentLanguageTag = remember { LocaleManager.readLanguageTag(activity) }

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
                    text = stringResource(R.string.account_language_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.account_language_subtitle),
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
                    LanguageOptionButton(
                        text = stringResource(R.string.account_language_arabic),
                        selected = currentLanguageTag.startsWith("ar"),
                        onClick = { (activity as com.example.MainActivity).setAppLocale("ar") },
                        modifier = Modifier.weight(1f)
                    )

                    LanguageOptionButton(
                        text = stringResource(R.string.account_language_english),
                        selected = currentLanguageTag.startsWith("en"),
                        onClick = { (activity as com.example.MainActivity).setAppLocale("en") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Option lists matching screenshot aesthetics
        AccountOptionRow(
            title = stringResource(R.string.account_option_profile),
            icon = Icons.Filled.Person,
            onClick = { activeSheet = AccountSheet.ProfileSettings }
        )

        AccountOptionRow(
            title = stringResource(R.string.account_option_orders),
            icon = Icons.Filled.LocalShipping,
            onClick = { activeSheet = AccountSheet.MyOrders }
        )

        AccountOptionRow(
            title = stringResource(R.string.account_option_contact),
            icon = Icons.Filled.Phone,
            onClick = { activeSheet = AccountSheet.Contact }
        )

        AccountOptionRow(
            title = stringResource(R.string.account_option_privacy),
            icon = Icons.Filled.PrivacyTip,
            onClick = { activeSheet = AccountSheet.Privacy }
        )

        AccountOptionRow(
            title = stringResource(R.string.account_option_terms),
            icon = Icons.Filled.FactCheck,
            onClick = { activeSheet = AccountSheet.Terms }
        )

        AccountOptionRow(
            title = stringResource(R.string.account_option_admin),
            icon = Icons.Filled.Settings,
            onClick = { activeSheet = AccountSheet.AdminPanel }
        )

        AccountOptionRow(
            title = stringResource(R.string.account_option_logout),
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
            IconButton(onClick = {
                if (currentActiveSheet == AccountSheet.AdminPanel && isAdminAuthenticated) {
                    vms.adminAuth.lockNow()
                }
                activeSheet = null
            }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_return),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = when (currentActiveSheet) {
                    AccountSheet.MyOrders -> stringResource(R.string.account_sheet_my_orders)
                    AccountSheet.Contact -> stringResource(R.string.account_sheet_contact)
                    AccountSheet.Privacy -> stringResource(R.string.account_sheet_privacy)
                    AccountSheet.Terms -> stringResource(R.string.account_sheet_terms)
                    AccountSheet.Logout -> stringResource(R.string.account_sheet_logout)
                    AccountSheet.ProfileSettings -> stringResource(R.string.account_sheet_profile)
                    AccountSheet.AdminPanel -> stringResource(R.string.account_sheet_admin)
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
                                    text = stringResource(R.string.profile_intro),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Select Avatar Option
                                Text(
                                    text = stringResource(R.string.profile_avatar_label),
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
                                    text = stringResource(R.string.profile_field_username),
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
                                    text = stringResource(R.string.profile_field_phone),
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
                                        placeholder = { Text(stringResource(R.string.profile_phone_placeholder), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
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
                                    text = stringResource(R.string.profile_phone_country_hint),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Location Picker Section
                                Text(
                                    text = stringResource(R.string.profile_field_location),
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
                                    placeholder = { Text(stringResource(R.string.profile_address_placeholder), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
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
                                                    Text(stringResource(R.string.profile_gps_waiting), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            } else {
                                                Column(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DiscountRed, modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    if (formLocation.contains("GPS:")) {
                                                        Text(stringResource(R.string.profile_gps_success), fontSize = 10.sp, color = ShopGreenDark, fontWeight = FontWeight.Bold)
                                                        Text(formLocation, fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                                    } else {
                                                        Text(stringResource(R.string.profile_gps_prompt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
                                                Text(stringResource(R.string.profile_gps_action), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                                android.widget.Toast.makeText(context, context.getString(R.string.error_username_required), android.widget.Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            vms.userProfile.updateUserProfile(
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
                                            text = if (isUserRegistered) stringResource(R.string.profile_action_save) else stringResource(R.string.profile_action_confirm),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { activeSheet = null },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(stringResource(R.string.profile_action_cancel), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        AccountSheet.AdminPanel -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val adminProducts by vms.shop.productsFlow.collectAsState(initial = emptyList())
                            // H2 / Phase 7B-2 — archived tab. Soft-archived
                            // products never appear in `adminProducts`
                            // (DAO filter `WHERE archivedAt IS NULL`), so
                            // we expose a separate Flow from the VM.
                            val archivedProducts by vms.shop.archivedProductsFlow.collectAsState(initial = emptyList())

                            // H2 — confirm dialog state for archive /
                            // unarchive. The dialog is hoisted out of the
                            // card body so the IconButton can show a haptic
                            // + open it in one call. The product name is
                            // captured at tap-time so the body string can
                            // show "%1$s" with the right entity.
                            var archiveConfirmProduct by remember { mutableStateOf<Product?>(null) }
                            var unarchiveConfirmProduct by remember { mutableStateOf<Product?>(null) }

                            // L5 / Phase 7B-2 — Crossfade on the
                            // auth flip. When the user successfully
                            // signs in (or the 5-min idle timer
                            // expires / Lock now is tapped), the
                            // login form and the admin tabs
                            // cross-fade. The outer
                            // Crossfade at line 101 covers
                            // sheet-level transitions
                            // (ProfileSettings <-> AdminPanel);
                            // this one covers the auth-flip
                            // inside the AdminPanel sheet.
                            Crossfade(
                                targetState = isAdminAuthenticated,
                                label = "admin_auth_flip"
                            ) { authed ->
                            if (!authed) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = stringResource(R.string.admin_auth_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.admin_auth_subtitle),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = adminPasswordInput,
                                        onValueChange = {
                                            adminPasswordInput = it
                                            vms.adminAuth.consumeError()
                                        },
                                        isError = adminAuthError != null,
                                        singleLine = true,
                                        label = { Text(stringResource(R.string.admin_password_field), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
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
                                                tint = if (adminAuthError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )

                                    // D8.23 — VM-driven error / lockout messages.
                                    // Replaces the local `adminPasswordError` flow.
                                    val lockoutActive = adminLockoutUntil > System.currentTimeMillis()
                                    if (lockoutActive) {
                                        val secondsRemaining = ((adminLockoutUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                                        Text(
                                            text = stringResource(R.string.admin_auth_locked, secondsRemaining),
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    } else if (adminAuthError != null) {
                                        Text(
                                            text = stringResource(R.string.error_admin_password),
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    val haptics = rememberHapticClick()
                                    Button(
                                        onClick = {
                                            haptics()
                                            vms.adminAuth.signInWithFirebase(adminPasswordInput)
                                        },
                                        enabled = !adminAuthenticating && !lockoutActive,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.admin_action_login),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }

                                    // D8.23 / T2.14 — `Activity.recreate()` preserves the
                                    // VM, so a locale switch keeps the user signed in.
                                    // The first time the password is submitted successfully
                                    // the VM flips `isAuthenticated`; the LaunchedEffect
                                    // back-fills the form id only when we don't already
                                    // have a loaded product in the form. It also kicks the
                                    // 5-min idle clock (T2.4).
                                    LaunchedEffect(isAdminAuthenticated) {
                                        if (isAdminAuthenticated) {
                                            vms.adminAuth.recordActivity()
                                            if (adminProductId.isBlank()) {
                                                // L1 / Phase 7B-2 — UUID over
                                                // System.currentTimeMillis()
                                                // for the new-product id seed.
                                                // Two concurrent admins on the
                                                // same device can both call
                                                // this in the same millisecond;
                                                // UUID makes that practically
                                                // impossible. The `p_` prefix
                                                // is preserved because
                                                // ShopViewModel.addOrUpdateProduct
                                                // treats it as the
                                                // "startsWith(p_) => new product"
                                                // signal.
                                                adminProductId = "p_${java.util.UUID.randomUUID()}"
                                            }
                                        }
                                    }
                                }
                            } else {
                                var adminTab by remember { mutableStateOf(0) }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    // D8.23 / T2.16 — "Lock now" affordance. Hitting
                                    // this calls `AdminAuthViewModel.lockNow()` and
                                    // closes the sheet so the next launch goes back
                                    // through the password gate.
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(
                                            onClick = {
                                                vms.adminAuth.lockNow()
                                                activeSheet = null
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = stringResource(R.string.admin_action_lock_session_cd),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
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
                                                vms.userProfile.loadOnlineCustomers()
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
                                                Text(stringResource(R.string.admin_tab_customers), fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(12.dp))
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
                                                Text(stringResource(R.string.admin_tab_inventory), fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                        }

                                        ElevatedButton(
                                            onClick = { adminTab = 3 },
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = if (adminTab == 3) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (adminTab == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(stringResource(R.string.admin_tab_archived), fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                        }

                                        ElevatedButton(
                                            onClick = { adminTab = 0 },
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = if (adminTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (adminTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.2f),
                                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if (adminProductId.startsWith("p_")) stringResource(R.string.admin_product_add) else stringResource(R.string.admin_product_edit), fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // D9.1 — at Expanded width the list tabs (1 = Inventory,
                                    // 2 = Customers) split into a draggable side-by-side view
                                    // so admins can see product stock + customer list at the
                                    // same time. The Add/Edit Product tab (0) stays full-width
                                    // because the form is too vertical to fit alongside
                                    // another panel. Compact / Medium widths keep the
                                    // single-column original layout untouched.
                                    val windowSizeClass = LocalWindowSizeClass.current
                                    val isExpandedAdmin =
                                        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                                    if (isExpandedAdmin && (adminTab == 1 || adminTab == 2)) {
                                        val inventoryLeft = adminTab == 1
                                        DraggableSplitRow(
                                            initialRatio = if (inventoryLeft) 0.6f else 0.4f,
                                             left = {
                                                if (inventoryLeft) {
                                                    AdminInventoryBody(
                                                        products = adminProducts,
                                                        onLoadProduct = { prod ->
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
                                                            // M3 / Phase 7B-2 — load the
                                                            // discount fields so the
                                                            // admin can round-trip a
                                                            // discounted product
                                                            // (otherwise the toggle
                                                            // would be silently off and
                                                            // the next upload would
                                                            // overwrite the product's
                                                            // discount state).
                                                            adminProductIsDiscounted = prod.isDiscounted
                                                            adminProductOriginalPrice = prod.originalPrice?.toString().orEmpty()
                                                            adminTab = 0
                                                            // H5 / Phase 7B-2 — admin
                                                            // "loaded for edit" feedback
                                                            // routes through MessageBus
                                                            // so the snackbar uses the
                                                            // correct locale and lives
                                                            // outside the screen.
                                                            MessageBus.publish(
                                                                LocaleResources.getString(
                                                                    R.string.toast_admin_loaded_for_edit
                                                                )
                                                            )
                                                        },
                                                        onArchive = { prod -> archiveConfirmProduct = prod }
                                                    )
                                                } else {
                                                    AdminCustomersBody(
                                                        customers = onlineCustomers,
                                                        onRefresh = { vms.userProfile.loadOnlineCustomers() }
                                                    )
                                                }
                                            },
                                            right = {
                                                if (inventoryLeft) {
                                                    AdminCustomersBody(
                                                        customers = onlineCustomers,
                                                        onRefresh = { vms.userProfile.loadOnlineCustomers() }
                                                    )
                                                } else {
                                                    AdminInventoryBody(
                                                        products = adminProducts,
                                                        onLoadProduct = { prod ->
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
                                                            // M3 / Phase 7B-2 — load the
                                                            // discount fields (round-trip
                                                            // the discount state on edit).
                                                            adminProductIsDiscounted = prod.isDiscounted
                                                            adminProductOriginalPrice = prod.originalPrice?.toString().orEmpty()
                                                            adminTab = 0
                                                            // H5 / Phase 7B-2 — admin
                                                            // "loaded for edit" feedback
                                                            // routes through MessageBus
                                                            // so the snackbar uses the
                                                            // correct locale and lives
                                                            // outside the screen.
                                                            MessageBus.publish(
                                                                LocaleResources.getString(
                                                                    R.string.toast_admin_loaded_for_edit
                                                                )
                                                            )
                                                        },
                                                        onArchive = { prod -> archiveConfirmProduct = prod }
                                                    )
                                                }
                                            }
                                        )
                                    } else if (adminTab == 0) {
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
                                                        text = if (adminProductId.startsWith("p_")) stringResource(R.string.admin_product_add_subtitle) else stringResource(R.string.admin_product_edit_subtitle),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (adminProductId.startsWith("p_")) MaterialTheme.colorScheme.onSecondaryContainer else ShopGreenDark
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = stringResource(R.string.admin_product_id_display, adminProductId.take(12)),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(
                                                text = stringResource(R.string.admin_product_field_name_ar),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            OutlinedTextField(
                                                value = adminProductNameAr,
                                                onValueChange = { adminProductNameAr = it },
                                                singleLine = true,
                                                placeholder = { Text(stringResource(R.string.admin_product_name_ar_placeholder), textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = stringResource(R.string.admin_product_field_name_en),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                             OutlinedTextField(
                                                value = adminProductNameEn,
                                                onValueChange = { adminProductNameEn = it },
                                                singleLine = true,
                                                // M2 / Phase 7B-2 — TextAlign.Left/Right
                                                // is locale-fixed; the EN field was using
                                                // Left (ltr) which is fine for English,
                                                // but Start is the locale-aware
                                                // default that mirrors Right (Start
                                                // in RTL locales). For the EN field
                                                // the visual result is the same; the
                                                // change is for code-uniformity with
                                                // the rest of the form.
                                                placeholder = { Text(stringResource(R.string.admin_product_name_en_placeholder), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) },
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
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
                                                        text = stringResource(R.string.admin_product_field_stock),
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
                                                        text = stringResource(R.string.admin_product_field_price),
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
                                                        suffix = { Text(stringResource(R.string.currency_iqd_short), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) },
                                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = stringResource(R.string.admin_product_field_category),
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
                                                text = stringResource(R.string.admin_product_field_desc_ar),
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
                                                text = stringResource(R.string.admin_product_field_desc_en),
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
                                                // M2 / Phase 7B-2 — Start over Left.
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = stringResource(R.string.admin_product_field_image),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = adminProductImageUrl,
                                                // M1 / Phase 7B-2 — light-touch
                                                // URL validation. Reject
                                                // keystrokes that would leave
                                                // the field without an `http`
                                                // prefix (or `https`); the
                                                // Coil loader in
                                                // ProductDetailScreen would
                                                // fail silently with no
                                                // indicator, and a stray
                                                // `example.com/foo.jpg`
                                                // (no scheme) renders as a
                                                // broken image. We let blank
                                                // values through (the
                                                // ShopViewModel defaults the
                                                // blank case to a placeholder
                                                // URL) and the user can also
                                                // paste via the quick-pick
                                                // grid below.
                                                onValueChange = { newValue ->
                                                    if (newValue.isBlank() ||
                                                        newValue.startsWith("http://") ||
                                                        newValue.startsWith("https://")) {
                                                        adminProductImageUrl = newValue
                                                    }
                                                },
                                                singleLine = true,
                                                // M2 / Phase 7B-2 — Start over Left.
                                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
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
                                                Text(stringResource(R.string.admin_image_presets_label), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val newFormClick = rememberHapticClick {
                                                    // L1 / Phase 7B-2 — UUID over
                                                    // System.currentTimeMillis()
                                                    // for the new-product id seed.
                                                    adminProductId = "p_${java.util.UUID.randomUUID()}"
                                                    adminProductNameAr = ""
                                                    adminProductNameEn = ""
                                                    adminProductDescAr = ""
                                                    adminProductDescEn = ""
                                                    adminProductPrice = ""
                                                    adminProductStock = "10"
                                                    adminProductImageUrl = "https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400"
                                                    // M3 / Phase 7B-2 — reset the
                                                    // discount fields so a freshly
                                                    // cleared form doesn't carry the
                                                    // previously-loaded product's
                                                    // discount state.
                                                    adminProductIsDiscounted = false
                                                    adminProductOriginalPrice = ""
                                                }
                                                OutlinedButton(
                                                    onClick = newFormClick,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(stringResource(R.string.admin_action_new_form), fontSize = 11.sp)
                                                }

                                                val uploadClick = rememberHapticClick {
                                                    val priceDouble = adminProductPrice.toDoubleOrNull() ?: 0.0
                                                    val stockInt = adminProductStock.toIntOrNull() ?: 10

                                                    if (adminProductNameAr.isBlank() || adminProductNameEn.isBlank() || priceDouble <= 0.0) {
                                                        // H5 / Phase 7B-2 — required-field
                                                        // validation feedback routes
                                                        // through MessageBus. The
                                                        // admin sees a localized snackbar
                                                        // rather than an Android Toast
                                                        // (which uses the application
                                                        // context and is locale-buggy
                                                        // per D8.21).
                                                        MessageBus.publish(
                                                            LocaleResources.getString(
                                                                R.string.error_admin_required_fields
                                                            )
                                                        )
                                                        return@rememberHapticClick
                                                    }

                                                    vms.shop.addOrUpdateProduct(
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
                                                            // M3 / Phase 7B-2 — wire the
                                                            // discount fields through.
                                                            // The two state vars
                                                            // (`adminProductIsDiscounted`
                                                            // and
                                                            // `adminProductOriginalPrice`)
                                                            // were already declared at
                                                            // lines 89-90 but not
                                                            // connected to the upload.
                                                            // Now they are.
                                                            isDiscounted = adminProductIsDiscounted,
                                                            originalPrice = adminProductOriginalPrice.toDoubleOrNull(),
                                                            onComplete = { writeResult ->
                                                                // H1 / Phase 7B-2 — reset the form only
                                                                // when the local write succeeded.
                                                                // Both `BothOk` and `LocalOnlyOffline`
                                                                // are durable; the other two cases
                                                                // keep the form so the admin can retry.
                                                                val shouldReset = when (writeResult) {
                                                                    is com.example.data.repository.ProductWriteResult.BothOk -> true
                                                                    is com.example.data.repository.ProductWriteResult.LocalOnlyOffline -> true
                                                                    is com.example.data.repository.ProductWriteResult.BothFailed -> false
                                                                    is com.example.data.repository.ProductWriteResult.LocalFailedButRemoteOk -> false
                                                                }
                                                                if (shouldReset) {
                                                                    // L1 / Phase 7B-2 — UUID
                                                                    // over System.currentTimeMillis()
                                                                    // for the new-product id seed.
                                                                    adminProductId = "p_${java.util.UUID.randomUUID()}"
                                                                    adminProductNameAr = ""
                                                                    adminProductNameEn = ""
                                                                    adminProductDescAr = ""
                                                                    adminProductDescEn = ""
                                                                    adminProductPrice = ""
                                                                    adminProductStock = "10"
                                                                    adminProductImageUrl = "https://images.unsplash.com/photo-1608564697071-ddf911d81370?w=400"
                                                                    // M3 / Phase 7B-2 — also
                                                                    // clear the discount
                                                                    // fields so the next
                                                                    // upload starts from a
                                                                    // blank draft.
                                                                    adminProductIsDiscounted = false
                                                                    adminProductOriginalPrice = ""
                                                                }
                                                            }
                                                        )
                                                }
                                                Button(
                                                    onClick = uploadClick,
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stringResource(R.string.admin_action_upload), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }
                                    } else if (adminTab == 1) {
                                        AdminInventoryBody(
                                            products = adminProducts,
                                            onLoadProduct = { prod ->
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
                                                // M3 / Phase 7B-2 — load the
                                                // discount fields (round-trip the
                                                // discount state on edit).
                                                adminProductIsDiscounted = prod.isDiscounted
                                                adminProductOriginalPrice = prod.originalPrice?.toString().orEmpty()
                                                adminTab = 0
                                                // H5 / Phase 7B-2 — admin
                                                // "loaded for edit" feedback
                                                // routes through MessageBus
                                                // so the snackbar uses the
                                                // correct locale and lives
                                                // outside the screen.
                                                MessageBus.publish(
                                                    LocaleResources.getString(
                                                        R.string.toast_admin_loaded_for_edit
                                                    )
                                                )
                                            },
                                            onArchive = { prod -> archiveConfirmProduct = prod }
                                        )
                                    } else if (adminTab == 2) {
                                        AdminCustomersBody(
                                            customers = onlineCustomers,
                                            onRefresh = { vms.userProfile.loadOnlineCustomers() }
                                        )
                                    } else if (adminTab == 3) {
                                        AdminArchivedBody(
                                            products = archivedProducts,
                                            onRestore = { prod -> unarchiveConfirmProduct = prod }
                                        )
                                    }

                                    // H2 — confirm dialogs for archive /
                                    // unarchive. Hoisted here so the bodies
                                    // can stay simple `Column`/`LazyColumn`
                                    // units and only the actions are wired.
                                    archiveConfirmProduct?.let { prod ->
                                        ConfirmDialog(
                                            title = stringResource(R.string.admin_archive_confirm_title),
                                            message = stringResource(R.string.admin_archive_confirm_body, prod.nameAr),
                                            confirmText = stringResource(R.string.profile_action_confirm),
                                            onConfirm = {
                                                vms.shop.archiveProduct(prod.id)
                                                archiveConfirmProduct = null
                                            },
                                            onDismiss = { archiveConfirmProduct = null }
                                        )
                                    }
                                    unarchiveConfirmProduct?.let { prod ->
                                        ConfirmDialog(
                                            title = stringResource(R.string.admin_unarchive_confirm_title),
                                            message = stringResource(R.string.admin_unarchive_confirm_body, prod.nameAr),
                                            confirmText = stringResource(R.string.profile_action_confirm),
                                            onConfirm = {
                                                vms.shop.unarchiveProduct(prod.id)
                                                unarchiveConfirmProduct = null
                                            },
                                            onDismiss = { unarchiveConfirmProduct = null }
                                        )
                                    }
                                }
                            }
                            }  // end Crossfade(admin_auth_flip) — L5 / Phase 7B-2
                        }
                        AccountSheet.MyOrders -> {
                            if (orders.isEmpty()) {
                                // D9.2 — shared empty-state slot. The
                                // orders sheet is scrollable so a fixed
                                // 200dp vertical breathing room is plenty.
                                EmptyState(
                                    illustration = null,
                                    title = stringResource(R.string.empty_orders_title),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
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
                                text = stringResource(R.string.contact_info_body),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AccountSheet.Privacy -> {
                            Text(
                                text = stringResource(R.string.privacy_body),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AccountSheet.Terms -> {
                            Text(
                                text = stringResource(R.string.terms_body),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AccountSheet.Logout -> {
                            Text(
                                text = stringResource(R.string.logout_body),
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
                                Text(stringResource(R.string.logout_action_confirm), fontSize = 12.sp, color = Color.White)
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
                contentDescription = stringResource(R.string.cd_navigate),
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
        sdf.format(Date(order.order.timestamp))
    } catch (e: Exception) {
        stringResource(R.string.order_date_fallback)
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
                        .background(if (order.order.isSynced) ShopGreenContainer else WarningAmberContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (order.order.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = stringResource(R.string.cd_sync_state),
                            tint = if (order.order.isSynced) ShopGreenDark else WarningAmberDeep,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (order.order.isSynced) stringResource(R.string.order_synced) else stringResource(R.string.order_local),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (order.order.isSynced) ShopGreenDark else WarningAmberDeep
                        )
                    }
                }

                // Reference Id code
                Text(
                    text = stringResource(R.string.order_id_short, order.order.id.take(6).uppercase()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = stringResource(R.string.order_date, dateString), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = stringResource(R.string.order_recipient, order.order.customerName), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.order_phone, order.order.customerPhone), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = stringResource(R.string.order_shipping_address, order.order.customerAddress), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Items come pre-fetched via OrderWithItems @Relation — no JSON parsing
            val itemsList = remember(order.items) {
                order.items.map { item ->
                    Triple(item.getName(isArabic = true), item.price, item.quantity)
                }
            }

            if (itemsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.order_items_label),
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
                            text = stringResource(R.string.price_with_currency, formatPrice(price * qty), stringResource(R.string.currency_iqd)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.order_item_line, name, qty),
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
                text = stringResource(R.string.order_status_label),
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
                val currentStatus = order.order.statusAr
                val steps = listOf(
                    stringResource(R.string.order_step_confirm),
                    stringResource(R.string.order_step_process),
                    stringResource(R.string.order_step_ship_cod),
                    stringResource(R.string.order_step_deliver)
                )
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
                                    contentDescription = stringResource(R.string.cd_completed_step),
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
                    text = stringResource(R.string.price_with_currency, formatPrice(order.order.totalPrice), stringResource(R.string.currency_iqd)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.order_total_cod),
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

// D8.19 — language picker button. Mirrors the visual treatment of
// ThemeOptionButton (ElevatedButton with selected = primary fill) but
// without an icon, since the language name itself is the identifier.
// Text is 13sp (vs 11sp in the theme button) to fill the space the icon
// would normally take.
@Composable
fun LanguageOptionButton(
    text: String,
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
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// D9.1 — admin tab body helpers, extracted so the Expanded-width
// `DraggableSplitRow` can render them as both left and right panels
// without duplicating ~250 lines of card + list markup. The bodies
// receive a single callback each so they don't need to know about
// the 13 admin form state vars living in `AccountScreen()`.

@Composable
private fun AdminInventoryBody(
    products: List<Product>,
    onLoadProduct: (Product) -> Unit,
    onArchive: (Product) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = stringResource(R.string.admin_inventory_title),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.admin_inventory_subtitle),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (products.isEmpty()) {
            // D9.2 — shared empty-state slot. Admin panel is
            // compact (11sp text elsewhere) so the title is the
            // only thing shown — no subtitle, no illustration.
            EmptyState(
                illustration = null,
                title = stringResource(R.string.empty_inventory_title),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            products.forEach { prod ->
                val cardClick = rememberHapticClick { onLoadProduct(prod) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(onClick = cardClick),
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
                            Text(prod.nameEn, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Start)
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
                                        text = stringResource(R.string.admin_inventory_stock_count, prod.stock),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStockLow) MaterialTheme.colorScheme.error else ShopGreenDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                // L4 / Phase 7B-2 — pass price as String
                                // (the format spec is %1$s now) so
                                // decimal prices (e.g. 1234.5 IQD)
                                // render without the int-truncation
                                // bug.
                                Text(stringResource(R.string.admin_inventory_price, prod.price.toString()), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        // H2 / Phase 7B-2 — soft-archive shortcut.
                        // Wrapped in `rememberHapticClick` per H6 so
                        // the tap is consistent with the rest of the
                        // admin panel. The actual archive happens
                        // after the confirm dialog is acknowledged.
                        val archiveClick = rememberHapticClick { onArchive(prod) }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = archiveClick)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Archive,
                                contentDescription = stringResource(R.string.admin_action_archive_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
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
}

@Composable
private fun AdminCustomersBody(
    customers: List<Customer>,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val refreshClick = rememberHapticClick { onRefresh() }
            IconButton(onClick = refreshClick) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.admin_customers_title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.admin_customers_subtitle),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (customers.isEmpty()) {
            // D9.2 — shared empty-state slot. Title only; the
            // message about Firebase being offline belongs in a
            // future snackbar so it can be dismissed.
            EmptyState(
                illustration = null,
                title = stringResource(R.string.empty_customers_title),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            customers.forEach { customer ->
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
                                        text = stringResource(R.string.admin_customer_badge_online),
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
                                text = stringResource(R.string.admin_customer_phone, customer.countryCode, customer.phone),
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

/**
 * H2 / Phase 7B-2 — admin "Archived" tab body. Renders
 * soft-archived products with a Restore IconButton per card.
 * Mirrors [AdminInventoryBody] but skips the load-to-edit
 * affordance (restore is the only action here).
 */
@Composable
private fun AdminArchivedBody(
    products: List<Product>,
    onRestore: (Product) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = stringResource(R.string.admin_archived_title),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.admin_archived_subtitle),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (products.isEmpty()) {
            EmptyState(
                illustration = null,
                title = stringResource(R.string.admin_archived_empty),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            products.forEach { prod ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                            Text(prod.nameEn, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Start)
                            Spacer(modifier = Modifier.height(4.dp))
                            // L4 / Phase 7B-2 — pass price as String.
                            Text(stringResource(R.string.admin_inventory_price, prod.price.toString()), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        val restoreClick = rememberHapticClick { onRestore(prod) }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = restoreClick)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = stringResource(R.string.admin_action_restore_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeveloperBoard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Phase 7B-2 — minimal Material 3 confirm dialog. Two-button
 * (Confirm / Dismiss), no icon, no title alignment fuss. Used by
 * the H2 archive / unarchive confirm flows. Kept private to
 * AccountScreen because no other screen has needed a confirm
 * pattern yet (the existing logout/profile flows use a one-button
 * inline action without a dialog).
 */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 12.sp) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(confirmText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_action_cancel), fontSize = 12.sp)
            }
        }
    )
}

/**
 * D9.1 — two-pane layout with a draggable splitter. The split
 * ratio is owned by an internal `MutableState` (not persisted
 * across recompositions) and coerced into `[minRatio, maxRatio]`
 * so neither panel can collapse. The drag handler is wired through
 * `detectDragGestures` and operates on pixel deltas — `onSizeChanged`
 * captures the row's measured width on first layout so the delta
 * can be turned back into a ratio even when the parent `Row` width
 * is not yet known at drag-start time.
 *
 * Visual treatment: a 8dp wide vertical track with a 24dp tall grip
 * pill in the middle. Alpha-tuned to match `outline.copy(0.3f)` so
 * it reads as a divider, not chrome.
 */
@Composable
fun DraggableSplitRow(
    initialRatio: Float = 0.5f,
    minRatio: Float = 0.2f,
    maxRatio: Float = 0.8f,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    var ratio by remember { mutableStateOf(initialRatio.coerceIn(minRatio, maxRatio)) }
    var containerWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val splitterWidthPx = with(density) { 8.dp.toPx() }
    val minWidthPx = with(density) { 200.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .onSizeChanged { containerWidthPx = it.width }
    ) {
        Box(
            modifier = Modifier
                .weight(ratio)
                .fillMaxHeight()
        ) { left() }
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val innerWidthPx = (containerWidthPx - splitterWidthPx).coerceAtLeast(1f)
                        val deltaRatio = dragAmount.x / innerWidthPx
                        val proposed = ratio + deltaRatio
                        // Additional safety: prevent either side from going
                        // below ~200dp regardless of ratio math.
                        val minByPxRatio = minWidthPx / innerWidthPx
                        val maxByPxRatio = 1f - minByPxRatio
                        ratio = proposed.coerceIn(
                            maxOf(minRatio, minByPxRatio),
                            minOf(maxRatio, maxByPxRatio)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f - ratio)
                .fillMaxHeight()
        ) { right() }
    }
}
