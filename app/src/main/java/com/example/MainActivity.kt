package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomerEntity
import com.example.data.TransactionEntity
import com.example.ui.AccountingViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: AccountingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                // Background Box wrapping edge-to-edge colors safely
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavContainer(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppNavContainer(viewModel: AccountingViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    // Listen for toasts
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Crossfade(targetState = currentScreen, label = "screen_navigation") { screen ->
        when (screen) {
            "login" -> AuthScreen(viewModel)
            "main" -> {
                // If logged in, check locker condition
                if (currentUser != null && !isUnlocked) {
                    LockerScreen(viewModel)
                } else {
                    MainWorkspaceScreen(viewModel)
                }
            }
            else -> AuthScreen(viewModel)
        }
    }
}

// ----------------------------------------------------
// 1. REWARD LOCK SCREEN SYSTEM (APP LOCKER)
// ----------------------------------------------------
@Composable
fun LockerScreen(viewModel: AccountingViewModel) {
    val adUrl by viewModel.adLink.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var ticking by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E17),
                        Color(0xFF1E1B2E)
                    )
                )
            )
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF251F3D)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .border(2.dp, Color(0xFF7C3AED).copy(alpha = 0.5f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Safety Shield Icon with Glowing Gradient
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "🔒",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "অ্যাপটি আনলক করুন!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "নিচের বাটনে ক্লিক করে বিজ্ঞাপনটি দেখুন এবং ৫ সেকেন্ড অপেক্ষা করুন।",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        ticking = true
                        // Start simulated reward progress
                        viewModel.startAppUnlock(
                            onProgressChanged = { progress = it },
                            onFinished = { ticking = false }
                        )
                        // Trigger ad intent opening beautifully
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Secondary fallback inside container
                            viewModel.showToast("বিজ্ঞাপন ওপেন করা হচ্ছ...")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("appUnlockBtn"),
                    enabled = !ticking,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Unlock"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (ticking) "আনলক হচ্ছে..." else "আনলক করে প্রবেশ করুন 🔓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (ticking) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "দয়া করে অপেক্ষা করুন... ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. AUTHENTICATION SYSTEM (LOGIN/SIGNUP TABS)
// ----------------------------------------------------
@Composable
fun AuthScreen(viewModel: AccountingViewModel) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Signup
    val context = LocalContext.current

    // Inputs States
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupIsAdmin by remember { mutableStateOf(false) }

    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Logo
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pro Accounting",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF7C3AED),
                            Color(0xFF4F46E5)
                        )
                    ),
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "স্মার্ট দোকানের ডিজিটাল হিসাব খাতা",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Custom Tab Controller matching Tailwind tabs
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabToggleButton(
                        text = "লগইন",
                        isSelected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("login_tab")
                    )
                    TabToggleButton(
                        text = "সাইন আপ",
                        isSelected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("signup_tab")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Forms Layouts
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (activeTab == 0) {
                        // LOGIN FORM
                        Text(
                            text = "স্বাগতম! আপনার অ্যাকাউন্টে লগইন করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("ইমেইল") },
                            placeholder = { Text("example@email.com") },
                            modifier = Modifier.fillMaxWidth().testTag("login_email"),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("পাসওয়ার্ড") },
                            modifier = Modifier.fillMaxWidth().testTag("login_password"),
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.loginUser(loginEmail, loginPassword)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED)
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("লগইন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                    } else {
                        // SIGNUP FORM
                        Text(
                            text = "স্মার্ট হিসাব খাতার সদস্য হতে ফর্মটি পূরণ করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = signupName,
                            onValueChange = { signupName = it },
                            label = { Text("পুরো নাম") },
                            placeholder = { Text("আব্দুল্লাহ রহমান") },
                            modifier = Modifier.fillMaxWidth().testTag("signup_name"),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = signupEmail,
                            onValueChange = { signupEmail = it },
                            label = { Text("ইমেইল") },
                            placeholder = { Text("example@email.com") },
                            modifier = Modifier.fillMaxWidth().testTag("signup_email"),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = signupPassword,
                            onValueChange = { signupPassword = it },
                            label = { Text("পাসওয়ার্ড") },
                            modifier = Modifier.fillMaxWidth().testTag("signup_password"),
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { signupIsAdmin = !signupIsAdmin }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = signupIsAdmin,
                                onCheckedChange = { signupIsAdmin = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C3AED)),
                                modifier = Modifier.testTag("admin_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "আমি দোকানদার (Admin)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.signupUser(signupName, signupEmail, signupPassword, signupIsAdmin)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("signup_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED)
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("অ্যাকাউন্ট তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF7C3AED) else Color.Transparent,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ----------------------------------------------------
// 3. MAIN WORKSPACE WITH INTEGRATED CONTROLLER
// ----------------------------------------------------
@Composable
fun MainWorkspaceScreen(viewModel: AccountingViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val activeTab by viewModel.adminTab.collectAsStateWithLifecycle()

    var showProfileDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Branding Logo matching Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF7C3AED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "PA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "প্রো অ্যাকাউন্টিং",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFF7C3AED)
                        )
                    }

                    // Toggles & Profile Avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.Star else Icons.Default.Refresh,
                                contentDescription = "Theme"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))
                                    )
                                )
                                .clickable { showProfileDropdown = !showProfileDropdown },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.name ?: "U").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // If Role = Admin, display Navigation tabs "ড্যাশবোর্ড", "লেনদেন", "কাস্টমার"
                if (currentUser?.role == "admin") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        workspaceTabButton(
                            label = "ড্যাশবোর্ড",
                            isActive = activeTab == "dashboard",
                            onClick = { viewModel.adminTab.value = "dashboard" },
                            modifier = Modifier.testTag("nav-dashboard")
                        )
                        workspaceTabButton(
                            label = "লেনদেন খতিয়ান",
                            isActive = activeTab == "transactions",
                            onClick = { viewModel.adminTab.value = "transactions" },
                            modifier = Modifier.testTag("nav-transactions")
                        )
                        workspaceTabButton(
                            label = "কাস্টমার তালিকা",
                            isActive = activeTab == "customers",
                            onClick = { viewModel.adminTab.value = "customers" },
                            modifier = Modifier.testTag("nav-customers")
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->

        // Overlay dialog menu simulating drop-down matching logout
        if (showProfileDropdown) {
            Dialog(onDismissRequest = { showProfileDropdown = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentUser?.name ?: "User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ভূমিকা: " + (if (currentUser?.role == "admin") "দোকানদার (Admin)" else "ক্রেতা (Customer)"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showProfileDropdown = false
                                viewModel.logout()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth().testTag("logout_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("লগআউট করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Check based on active screen and role
            if (currentUser?.role == "admin") {
                when (activeTab) {
                    "dashboard" -> AdminDashboardView(viewModel)
                    "transactions" -> TransactionsTimelineView(viewModel, isCustomerSpecific = false)
                    "customers" -> CustomersListView(viewModel)
                    "customer_detail" -> CustomerLedgerView(viewModel)
                    else -> AdminDashboardView(viewModel)
                }
            } else {
                // If standard Customer, display customer landing ledger page directly
                CustomerWorkspaceView(viewModel)
            }
        }
    }
}

@Composable
fun workspaceTabButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFF7C3AED).copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        color = if (isActive) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        fontSize = 14.sp
    )
}

// ----------------------------------------------------
// 4. ADMIN TAB A: MAIN DASHBOARD VIEW
// ----------------------------------------------------
@Composable
fun AdminDashboardView(viewModel: AccountingViewModel) {
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val customersList by viewModel.customers.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCompactCard(
                title = "মোট জমা",
                amount = totalIncome,
                amountColor = Color(0xFF10B981), // Emerald 500
                modifier = Modifier.weight(1f)
            )
            StatsCompactCard(
                title = "মোট খরচ",
                amount = totalExpense,
                amountColor = Color(0xFFEF4444), // Rose 500
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        StatsCompactCard(
            title = "ব্যালেন্স পরিমাণ",
            amount = totalBalance,
            amountColor = Color(0xFF7C3AED), // Indigo Purple
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Recent customers list title
        Text(
            text = "সাম্প্রতিক কাস্টমার হিসাব",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (customersList.isEmpty()) {
            EmptyListPlaceholder("এখনো পর্যন্ত কোনো কাস্টমার যুক্ত করা হয়নি। নিচে ক্লিক করে কাস্টমার লিস্টে যোগ করুন।")
        } else {
            // Look up net balance for each customer
            val recentCustomers = customersList.take(6)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentCustomers.forEach { customer ->
                    // Calculate individual balance reactively
                    val customerTrans = transactionsList.filter { it.customerEmail == customer.email }
                    val customerPaid = customerTrans.filter { it.type == "income" }.sumOf { it.amount }
                    val customerDue = customerTrans.filter { it.type == "expense" }.sumOf { it.amount }
                    val net = customerPaid - customerDue

                    CustomerSummaryRow(
                        customer = customer,
                        netBalance = net,
                        onClick = {
                            viewModel.selectedCustomer.value = customer
                            viewModel.adminTab.value = "customer_detail"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCompactCard(
    title: String,
    amount: Double,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = formatBDT(amount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = amountColor
            )
        }
    }
}

@Composable
fun CustomerSummaryRow(
    customer: CustomerEntity,
    netBalance: Double,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED).copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (customer.phone.isBlank()) "কোনো ফোন নাম্বার নেই" else customer.phone,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Balanced Indicator (Due is positive, Advances display)
            Column(horizontalAlignment = Alignment.End) {
                if (netBalance < 0) {
                    Text(
                        text = "বকেয়া: " + formatBDT(-netBalance),
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (netBalance > 0) {
                    Text(
                        text = "জমা: " + formatBDT(netBalance),
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "পরিশোধিত",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. ADMIN TAB B: STANDALONE TRANSACTION TIMELINE VIEW
// ----------------------------------------------------
@Composable
fun TransactionsTimelineView(
    viewModel: AccountingViewModel,
    isCustomerSpecific: Boolean
) {
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val specificTransactions by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    val displayList = if (isCustomerSpecific) specificTransactions else allTransactions
    
    // Add transaction inputs
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionDesc by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("income") } // income or expense
    var selectedCustomerForTx by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCustomerSpecific) "গ্রাহকের লেনদেন তালিকা" else "সর্বমোট লেনদেন বিবরণী",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyListPlaceholder("এখনো পর্যন্ত কোনো লেনদেন যোগ করা হয়নি। নিচের বোতাম ক্লিক করে শুরু করুন।")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayList, key = { it.id }) { tx ->
                        TransactionItemCard(tx, onDelete = {
                            viewModel.deleteTransaction(tx)
                        })
                    }
                }
            }
        }

        // Floating Action Button to add any standalone transaction
        if (!isCustomerSpecific) {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    selectedCustomerForTx = customers.firstOrNull()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_standalone_tx_fab"),
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }

        // Standard financial Transaction Modal Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "নতুন লেনদেন যোগ করুন 💸",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selection Dropdown list of customers
                        Text(
                            text = "গ্রাহক নির্বাচন করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (customers.isNotEmpty()) {
                                        customerDropdownExpanded = !customerDropdownExpanded
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Text(
                                text = selectedCustomerForTx?.name ?: "সাধারণ ক্যাশ হিসাব",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (customerDropdownExpanded) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    // General row option
                                    Text(
                                        text = "সাধারণ ক্যাশ হিসাব",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCustomerForTx = null
                                                customerDropdownExpanded = false
                                            }
                                            .padding(12.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    HorizontalDivider()

                                    customers.forEach { client ->
                                        Text(
                                            text = client.name + " (" + client.phone + ")",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCustomerForTx = client
                                                    customerDropdownExpanded = false
                                                }
                                                .padding(12.dp),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Type Choice (Income / Expense) tab matching Tailwind buttons
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { transactionType = "income" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transactionType == "income") Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (transactionType == "income") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("জমা (Income)", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { transactionType = "expense" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transactionType == "expense") Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (transactionType == "expense") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("খারচ/বকেয়া (Due)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = transactionAmount,
                            onValueChange = { transactionAmount = it },
                            label = { Text("টাকা পরিমাণ") },
                            placeholder = { Text("৳ ০০০.০০") },
                            modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = transactionDesc,
                            onValueChange = { transactionDesc = it },
                            label = { Text("বিবরণ") },
                            placeholder = { Text("মালের অর্ডার, বকেয়া মেটানো...") },
                            modifier = Modifier.fillMaxWidth().testTag("tx_desc_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("বাতিল", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = transactionAmount.toDoubleOrNull() ?: 0.0
                                    if (amt <= 0) {
                                        viewModel.showToast("সঠিক পরিমাণ প্রদান করুন")
                                        return@Button
                                    }
                                    
                                    viewModel.addTransaction(
                                        description = transactionDesc,
                                        amountVal = amt,
                                        type = transactionType,
                                        customerEmail = selectedCustomerForTx?.email ?: "general",
                                        customerName = selectedCustomerForTx?.name ?: "সাধারণ ক্যাশ হিসাব"
                                    )
                                    
                                    // Reset & Reset Dialogue
                                    transactionAmount = ""
                                    transactionDesc = ""
                                    showAddDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("যোগ করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    tx: TransactionEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Circular icon depending on Income vs Expense
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (tx.type == "income") Color(0xFF10B981).copy(alpha = 0.08f)
                            else Color(0xFFEF4444).copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tx.type == "income") Icons.Default.Check else Icons.Default.ShoppingCart,
                        tint = if (tx.type == "income") Color(0xFF10B981) else Color(0xFFEF4444),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.description,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "গ্রাহক: " + tx.customerName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (tx.type == "income") "+" else "-") + formatBDT(tx.amount),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (tx.type == "income") Color(0xFF10B981) else Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. ADMIN TAB C: CUSTOMER LIST VIEW (GRID)
// ----------------------------------------------------
@Composable
fun CustomersListView(viewModel: AccountingViewModel) {
    val customersList by viewModel.customers.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Inputs States
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var initialBalanceType by remember { mutableStateOf("income") } // income (জমা) or expense (বকেয়া)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কাস্টমার খাতা তালিকা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (customersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyListPlaceholder("কোনো কাস্টমার অ্যাকাউন্ট তৈরি করা হয়নি। নিচে ক্লিক করে নতুন কাস্টমার খাতা খুলুন।")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(customersList, key = { it.id }) { customer ->
                        // Dynamically compute net balance
                        val history = transactionsList.filter { it.customerEmail == customer.email }
                        val sumPaid = history.filter { it.type == "income" }.sumOf { it.amount }
                        val sumDue = history.filter { it.type == "expense" }.sumOf { it.amount }
                        val balance = sumPaid - sumDue

                        CustomerGridCard(
                            customer = customer,
                            netBalance = balance,
                            onViewDetail = {
                                viewModel.selectedCustomer.value = customer
                                viewModel.adminTab.value = "customer_detail"
                            },
                            onDelete = {
                                viewModel.deleteCustomer(customer)
                            }
                        )
                    }
                }
            }
        }

        // FAB to add customer
        FloatingActionButton(
            onClick = { showAddCustomerDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_customer_fab"),
            containerColor = Color(0xFF7C3AED),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }

        // Add Customer Dialog Modal
        if (showAddCustomerDialog) {
            Dialog(onDismissRequest = { showAddCustomerDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "নতুন কাস্টমার যোগ করুন 🤝",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("কাস্টমারের পুরো নাম *") },
                            placeholder = { Text("যেমন: আব্দুল করিম") },
                            modifier = Modifier.fillMaxWidth().testTag("cust_name_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = clientPhone,
                            onValueChange = { clientPhone = it },
                            label = { Text("মোবাইল নম্বর") },
                            placeholder = { Text("017XXXXXXXX") },
                            modifier = Modifier.fillMaxWidth().testTag("cust_phone_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = clientEmail,
                            onValueChange = { clientEmail = it },
                            label = { Text("ইমেইল (অ্যাকাউন্ট লিঙ্ক করার জন্য)") },
                            placeholder = { Text("karim@email.com") },
                            modifier = Modifier.fillMaxWidth().testTag("cust_email_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Optional Starting Balance Choice
                        Text(
                            text = "প্রারম্ভিক বকেয়া বা জমা (ঐচ্ছিক)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = initialBalance,
                                onValueChange = { initialBalance = it },
                                label = { Text("পরিমাণ টাকা") },
                                placeholder = { Text("৳ ০.০০") },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(0.9f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { initialBalanceType = "expense" }
                                ) {
                                    RadioButton(
                                        selected = initialBalanceType == "expense",
                                        onClick = { initialBalanceType = "expense" },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color.Red)
                                    )
                                    Text("বকেয়া (Due)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { initialBalanceType = "income" }
                                ) {
                                    RadioButton(
                                        selected = initialBalanceType == "income",
                                        onClick = { initialBalanceType = "income" },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color.Green)
                                    )
                                    Text("জমা (Paid)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddCustomerDialog = false }) {
                                Text("বাতিল", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (clientName.isBlank()) {
                                        viewModel.showToast("কাস্টমার নাম দেয়া আবশ্যক")
                                        return@Button
                                    }
                                    
                                    val startAmt = initialBalance.toDoubleOrNull() ?: 0.0
                                    viewModel.addCustomer(
                                        name = clientName,
                                        phone = clientPhone,
                                        emailInput = clientEmail,
                                        initialBalance = startAmt,
                                        balanceType = initialBalanceType
                                    )
                                    
                                    // Reset & Reset Dialog
                                    clientName = ""
                                    clientPhone = ""
                                    clientEmail = ""
                                    initialBalance = ""
                                    showAddCustomerDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerGridCard(
    customer: CustomerEntity,
    netBalance: Double,
    onViewDetail: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED).copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = customer.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (customer.phone.isBlank()) "কোনো ফোন নেই" else customer.phone,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Net balance visual tag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (netBalance < 0) Color(0xFFEF4444).copy(alpha = 0.07f)
                        else if (netBalance > 0) Color(0xFF10B981).copy(alpha = 0.07f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                    .padding(vertical = 6.dp, horizontal = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (netBalance < 0) {
                        Text(
                            text = "বকেয়া পাবে",
                            fontSize = 10.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatBDT(-netBalance),
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Black
                        )
                    } else if (netBalance > 0) {
                        Text(
                            text = "জমা রয়েছে",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatBDT(netBalance),
                            fontSize = 12.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        Text(
                            text = "ব্যালেন্স পরিশোধ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "৳ ০.০০",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Call CTA
            if (customer.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.phone))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কল ডায়াল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 7. CUSTOMER LEDGER / HISTORY TAB DETAIL VIEW
// ----------------------------------------------------
@Composable
fun CustomerLedgerView(viewModel: AccountingViewModel) {
    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val transactionsList by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var transactionType by remember { mutableStateOf("income") } // income or expense
    var transactionAmount by remember { mutableStateOf("") }
    var transactionDesc by remember { mutableStateOf("") }

    val activeCustomer = customer

    if (activeCustomer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("অনুগ্রহ করে কাস্টমার তালিকা থেকে নির্বাচন করুন।")
        }
    } else {
        // Calculate dynamic figures
        val totalPaid = transactionsList.filter { it.type == "income" }.sumOf { it.amount }
        val totalDue = transactionsList.filter { it.type == "expense" }.sumOf { it.amount }
        val netBalance = totalPaid - totalDue

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Profile details showing Name, Contact & Close Actions
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.adminTab.value = "customers" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = activeCustomer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (activeCustomer.phone.isNotBlank()) {
                                    Text(
                                        text = activeCustomer.phone,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Transaction Creator Button
                        Button(
                            onClick = { showAddTxDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("হিসাব লিখুন", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // mini financial summary boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.06f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("মোট জমা", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Text(formatBDT(totalPaid), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.06f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("মোট বকেয়া/ক্রয়", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                Text(formatBDT(totalDue), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF7C3AED).copy(alpha = 0.06f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("নেট বকেয়া স্থিতি", fontSize = 10.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (netBalance < 0) "৳ " + formatValue(-netBalance) + " বকেয়া"
                                           else if (netBalance > 0) "৳ " + formatValue(netBalance) + " অগ্রিম"
                                           else "জমা পরিশোধ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (netBalance < 0) Color.Red else Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body list showing past Transactions timeline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "খাতার হিসাব খতিয়ান",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (transactionsList.isEmpty()) {
                EmptyListPlaceholder("এই কাস্টমারের বকেয়া বা জমা হিসাব বিবরণী এখনো লিখা হয়নি।")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(transactionsList, key = { it.id }) { tx ->
                        TransactionItemCard(tx, onDelete = {
                            viewModel.deleteTransaction(tx)
                        })
                    }
                }
            }
        }

        // Add Transaction Dialog Modal for Specific Client
        if (showAddTxDialog) {
            Dialog(onDismissRequest = { showAddTxDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "গ্রাহক হিসাব লিখুন 📝",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Type Tabs Layout
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { transactionType = "income" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transactionType == "income") Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (transactionType == "income") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("জমা (Paid)", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { transactionType = "expense" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transactionType == "expense") Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (transactionType == "expense") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বকেয়া (Due)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = transactionAmount,
                            onValueChange = { transactionAmount = it },
                            label = { Text("অর্থের পরিমাণ *") },
                            placeholder = { Text("৳ ৪৫০.০০") },
                            modifier = Modifier.fillMaxWidth().testTag("cust_tx_amount_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("৳", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = transactionDesc,
                            onValueChange = { transactionDesc = it },
                            label = { Text("বিবরণ") },
                            placeholder = { Text("যেমন: সপ্তাহের মাল ক্রয়") },
                            modifier = Modifier.fillMaxWidth().testTag("cust_tx_desc_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddTxDialog = false }) {
                                Text("বাতিল", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val valAmt = transactionAmount.toDoubleOrNull() ?: 0.0
                                    if (valAmt <= 0) {
                                        viewModel.showToast("সঠিক পরিমাণ লিখুন")
                                        return@Button
                                    }

                                    viewModel.addTransaction(
                                        description = transactionDesc,
                                        amountVal = valAmt,
                                        type = transactionType,
                                        customerEmail = activeCustomer.email,
                                        customerName = activeCustomer.name
                                    )

                                    transactionAmount = ""
                                    transactionDesc = ""
                                    showAddTxDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. HIGH-FIDELITY CUSTOMER WEB HUB LANDING SCREEN
// ----------------------------------------------------
@Composable
fun CustomerWorkspaceView(viewModel: AccountingViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val ledgerTransactions by viewModel.loggedInCustomerTransactions.collectAsStateWithLifecycle()

    val clientUser = currentUser

    if (clientUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("দুঃখিত, কোনো কাস্টমার আইডি পাওয়া যায়নি!")
        }
    } else {
        // Compute stats for Customer user
        val totalPaid = ledgerTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalDue = ledgerTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val netBalance = totalPaid - totalDue

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Customer Header Box Card displaying Status
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "স্বাগতম, " + clientUser.name + " 👋",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                    Text(
                        text = "স্মার্ট ডিজিটাল খাতার বর্তমান হিসাব স্থিতি নিচে দেখুন",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (netBalance < 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আপনার বকেয়া পরিমাণ: " + formatBDT(-netBalance),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                        Text(
                            text = "বকেয়া মূল্য পরিশোধে দোকানদারের সাথে যোগাযোগ করুন।",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    } else if (netBalance > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আপনার অগ্রিম জমা: " + formatBDT(netBalance),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }
                    } else {
                        Text(
                            text = "আপনার সমস্ত হিসাব পরিশোধিত রয়েছে ✅",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Timeline Section showing detailed records
            Text(
                text = "আপনার লেনদেন ইতিহাস",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (ledgerTransactions.isEmpty()) {
                EmptyListPlaceholder("এখনো পর্যন্ত আপনার কোনো হিসাব বিবরণী যুক্ত করা হয়নি। দোকানদার যুক্ত করার সাথে সাথে দেখতে পাবেন।")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ledgerTransactions) { tx ->
                        // Standard Timeline view card for client readability
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (tx.type == "income") Icons.Default.Check else Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = if (tx.type == "income") Color.Green else Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tx.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp)),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                Text(
                                    text = (if (tx.type == "income") "জমা: +" else "বকেয়া: -") + formatBDT(tx.amount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (tx.type == "income") Color.Green else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// HELPER DRAWABLE COMPOSABLES & STRING HELPERS
// ----------------------------------------------------
@Composable
fun EmptyListPlaceholder(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            lineHeight = 20.sp
        )
    }
}

fun formatBDT(amount: Double): String {
    return "৳ " + String.format(Locale.US, "%,.2f", amount)
}

fun formatValue(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}
