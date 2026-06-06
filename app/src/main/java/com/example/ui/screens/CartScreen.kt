package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.components.EmptyState
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    vms: AppViewModels,
    onClose: () -> Unit
) {
    val cartItemsDetails by vms.cart.cartItemsDetails.collectAsState()
    val cartTotal by vms.cart.cartTotal.collectAsState()
    val isOnlineState by vms.shop.isOnline.collectAsState()

    var showCheckoutForm by remember { mutableStateOf(false) }
    var checkoutCompleted by remember { mutableStateOf(false) }

    val savedUsername by vms.userProfile.username.collectAsState()
    val savedPhone by vms.userProfile.userPhone.collectAsState()
    val savedLocation by vms.userProfile.userLocation.collectAsState()

    // Input States
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }

    LaunchedEffect(savedUsername, savedPhone, savedLocation) {
        if (!savedUsername.startsWith("مستخدم_")) {
            fullName = savedUsername
        } else {
            fullName = ""
        }
        phoneNumber = savedPhone
        // Strip GPS prefix from delivery address if fetched by GPS for cleaner manual typing
        deliveryAddress = if (savedLocation.startsWith("GPS:")) {
            savedLocation.removePrefix("GPS:")
        } else {
            savedLocation
        }
    }

    val deliveryFee = 5000.0 // Standard delivery in Iraq

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.cart_title, cartItemsDetails.sumOf { it.first.quantity }),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (checkoutCompleted) {
                // Success Checkout Dialog
                OrderSuccessLayout(
                    isOnline = isOnlineState,
                    onContinue = {
                        checkoutCompleted = false
                        showCheckoutForm = false
                        vms.navigation.selectTab(0) // open account history screen
                        onClose()
                    }
                )
            } else if (cartItemsDetails.isEmpty()) {
                // Empty view container
                EmptyCartLayout(onBrowse = onClose)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // List of items in cart
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cartItemsDetails) { (cartItem, product) ->
                            CartItemRow(
                                cartItem = cartItem,
                                product = product,
                                onQuantityChange = { nextQty ->
                                    vms.cart.updateQuantity(product.id, nextQty)
                                },
                                onRemoveItem = {
                                    vms.cart.removeFromCart(product.id)
                                }
                            )
                        }

                        // Checkout form collapsible row
                        if (showCheckoutForm) {
                            item {
                                CheckoutFormSection(
                                    fullName = fullName,
                                    phoneNumber = phoneNumber,
                                    deliveryAddress = deliveryAddress,
                                    onNameChange = { fullName = it },
                                    onPhoneChange = { phoneNumber = it },
                                    onAddressChange = { deliveryAddress = it }
                                )
                            }
                        }
                    }

                    // Bottom Total Cost calculation and checkout buttons
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            CostRow(label = stringResource(R.string.cart_cost_products), cost = cartTotal, currency = stringResource(R.string.currency_iqd))
                            CostRow(label = stringResource(R.string.cart_cost_delivery), cost = deliveryFee, currency = stringResource(R.string.currency_iqd))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            
                            // Net Pricing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.price_with_currency, formatPrice(cartTotal + deliveryFee), stringResource(R.string.currency_iqd)),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.cart_total_cod),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!showCheckoutForm) {
                                Button(
                                    onClick = { showCheckoutForm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.cart_action_continue_cod),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        vms.order.submitCODCheckout(
                                            name = fullName,
                                            phone = phoneNumber,
                                            address = deliveryAddress,
                                            cartTotal = cartTotal,
                                            cartItems = cartItemsDetails,
                                            onSuccess = {
                                                checkoutCompleted = true
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.cart_action_confirm_cod),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
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

@Composable
fun CartItemRow(
    cartItem: CartItem,
    product: Product,
    onQuantityChange: (Int) -> Unit,
    onRemoveItem: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete trash option
            IconButton(onClick = onRemoveItem) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_remove_item),
                    tint = DiscountRed.copy(alpha = 0.8f)
                )
            }

            // Central item adjustments & pricing
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = product.nameAr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = stringResource(R.string.price_with_currency, formatPrice(product.price), stringResource(R.string.currency_iqd)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onQuantityChange(cartItem.quantity + 1) },
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    IconButton(
                        onClick = { onQuantityChange(cartItem.quantity - 1) },
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cd_remove), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Image Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                val isImageError = remember { mutableStateOf(false) }
                if (!isImageError.value) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_product_placeholder)
                            .error(R.drawable.ic_product_error)
                            .build(),
                        contentDescription = product.nameAr,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { isImageError.value = true }
                    )
                }
                if (isImageError.value) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(ShopGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DeveloperBoard, contentDescription = stringResource(R.string.cd_tech_item), tint = ShopGreenDark, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CostRow(label: String, cost: Double, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.price_with_currency, formatPrice(cost), currency),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CheckoutFormSection(
    fullName: String,
    phoneNumber: String,
    deliveryAddress: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(R.string.cart_checkout_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Name
            Text(text = stringResource(R.string.cart_field_full_name), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone
            Text(text = stringResource(R.string.cart_field_phone), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Delivery Address
            Text(text = stringResource(R.string.cart_field_address), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = onAddressChange,
                singleLine = false,
                maxLines = 3,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Locking warning (mandatory as COD)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cart_payment_notice),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.cd_cod_payment_only), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyCartLayout(onBrowse: () -> Unit) {
    // D9.2 — the cart is the only empty-state in the app that has
    // a CTA (Browse store offerings). The illustration + title +
    // subtitle all come from `EmptyState`; the action button is
    // the optional `action` slot. Wrapping the whole thing in a
    // `Box(fillMaxSize)` keeps the layout pinned to the center
    // of the cart sheet — the old implementation did the same.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            illustration = painterResource(R.drawable.empty_cart_art),
            title = stringResource(R.string.empty_cart_title),
            subtitle = stringResource(R.string.empty_cart_subtitle),
            action = {
                Button(
                    onClick = onBrowse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cart_empty_action_browse),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )
    }
}

@Composable
fun OrderSuccessLayout(
    isOnline: Boolean,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.cd_success_checkmark),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.cart_success_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isOnline)
                    stringResource(R.string.cart_success_online)
                else
                    stringResource(R.string.cart_success_offline),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.cart_success_action_view_orders), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
