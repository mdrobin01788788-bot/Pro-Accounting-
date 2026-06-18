package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    // Native persistence settings keys
    private val KEY_UNLOCKED_TIME = "app_unlocked_time"
    private val KEY_AD_LINK = "ad_link"

    // Core States
    val currentUser = MutableStateFlow<UserEntity?>(null)
    val isUnlocked = MutableStateFlow<Boolean>(false)
    val adLink = MutableStateFlow("https://www.facebook.com")
    val currentScreen = MutableStateFlow("login") // login, signup, main
    val adminTab = MutableStateFlow("dashboard") // dashboard, transactions, customers, customer_detail
    
    // Loaded customer for the detail tab or history tab
    val selectedCustomer = MutableStateFlow<CustomerEntity?>(null)

    // Spinner and Notification Toasts
    val isLoading = MutableStateFlow(false)
    val toastMessage = MutableStateFlow<String?>(null)

    // Dark Mode Local Persistence State
    val isDarkMode = MutableStateFlow(true)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        
        // Load initial offline settings
        viewModelScope.launch {
            val link = repository.getSetting(KEY_AD_LINK) ?: "https://www.facebook.com"
            adLink.value = link
            
            // Check initial unlock status
            checkUnlockStatus()
        }
    }

    // Reactive streams from database
    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations for Admin Dashboard
    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == "income" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == "expense" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = transactions.map { list ->
        val inc = list.filter { it.type == "income" }.sumOf { it.amount }
        val exp = list.filter { it.type == "expense" }.sumOf { it.amount }
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Reactive stream for selected customer transactions
    val selectedCustomerTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        selectedCustomer
    ) { allTrans, activeCust ->
        if (activeCust == null) emptyList()
        else allTrans.filter { it.customerEmail == activeCust.email }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct stream for logged-in Customer transactions
    val loggedInCustomerTransactions: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        currentUser
    ) { allTrans, user ->
        if (user == null || user.role != "customer") emptyList()
        else allTrans.filter { it.customerEmail == user.email }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Check offline locker status
    private suspend fun checkUnlockStatus() {
        val unlockedTimeStr = repository.getSetting(KEY_UNLOCKED_TIME)
        if (unlockedTimeStr != null) {
            val unlockedTime = unlockedTimeStr.toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            // 24 hours lock period = 86400000ms
            if (now - unlockedTime < 86400000L) {
                isUnlocked.value = true
                return
            }
        }
        isUnlocked.value = false
    }

    // Triggered ad progress timer simulation
    fun startAppUnlock(onProgressChanged: (Float) -> Unit, onFinished: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            // Simulate 5-second progress updating
            for (i in 1..100) {
                kotlinx.coroutines.delay(50)
                onProgressChanged(i / 100f)
            }
            // Save unlock state
            val now = System.currentTimeMillis()
            repository.saveSetting(KEY_UNLOCKED_TIME, now.toString())
            isUnlocked.value = true
            isLoading.value = false
            showToast("অ্যাপ আনলক হয়েছে, স্বাগতম!")
            onFinished()
        }
    }

    fun showToast(message: String) {
        toastMessage.value = message
    }

    fun clearToast() {
        toastMessage.value = null
    }

    // Switch Dark Mode
    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // Authenticate: Local Registration
    fun signupUser(name: String, emailInput: String, pass: String, isAdmin: Boolean) {
        val email = emailInput.trim().lowercase()
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            showToast("সব তথ্য সঠিকভাবে পূরণ করুন")
            return
        }
        
        viewModelScope.launch {
            isLoading.value = true
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                showToast("এই ইমেইল দিয়ে ইতঃপূর্বে অ্যাকাউন্ট খোলা হয়েছে!")
                isLoading.value = false
                return@launch
            }

            val role = if (isAdmin) "admin" else "customer"
            val newUser = UserEntity(
                email = email,
                uid = java.util.UUID.randomUUID().toString(),
                name = name,
                password = pass,
                role = role
            )

            // Insert user
            repository.insertUser(newUser)

            // If customer, we automatically pre-create customer profile
            if (!isAdmin) {
                repository.insertCustomer(
                    CustomerEntity(
                        name = name,
                        email = email,
                        phone = ""
                    )
                )
            }

            currentUser.value = newUser
            checkUnlockStatus()
            
            // Navigate based on role
            if (role == "admin") {
                adminTab.value = "dashboard"
            } else {
                adminTab.value = "customer_landing"
            }
            currentScreen.value = "main"
            isLoading.value = false
            showToast("অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!")
        }
    }

    // Authenticate: Local Login
    fun loginUser(emailInput: String, pass: String) {
        val email = emailInput.trim().lowercase()
        if (email.isBlank() || pass.isBlank()) {
            showToast("দয়া করে ইমেইল ও পাসওয়ার্ড প্রদান করুন")
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            val user = repository.getUserByEmail(email)
            if (user == null) {
                showToast("এই ইমেইল দিয়ে কোনো অ্যাকাউন্ট খুজে পাওয়া যায়নি!")
                isLoading.value = false
                return@launch
            }

            if (user.password != pass) {
                showToast("ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।")
                isLoading.value = false
                return@launch
            }

            currentUser.value = user
            checkUnlockStatus()

            if (user.role == "admin") {
                adminTab.value = "dashboard"
            } else {
                adminTab.value = "customer_landing"
            }
            currentScreen.value = "main"
            isLoading.value = false
            showToast("লগইন সফল হয়েছে!")
        }
    }

    // App Log out
    fun logout() {
        viewModelScope.launch {
            isLoading.value = true
            currentUser.value = null
            // Also reset unlock in database as requested by HTML code
            repository.saveSetting(KEY_UNLOCKED_TIME, "0")
            isUnlocked.value = false
            currentScreen.value = "login"
            isLoading.value = false
            showToast("লগআউট করা হয়েছে!")
        }
    }

    // Add customer profile (Admin action)
    fun addCustomer(name: String, phone: String, emailInput: String, initialBalance: Double, balanceType: String) {
        val nameClean = name.trim()
        val email = emailInput.trim().lowercase()
        val phoneClean = phone.trim()
        
        if (nameClean.isBlank()) {
            showToast("কাস্টমারের নাম দেয়া আবশ্যক")
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            
            // Check if customer email already registered as customer profile
            if (email.isNotBlank()) {
                val existing = repository.getCustomerByEmail(email)
                if (existing != null) {
                    showToast("এই ইমেইল দিয়ে ইতোমধ্যেই একজন কাস্টমার রয়েছে")
                    isLoading.value = false
                    return@launch
                }
            }

            val newCustomer = CustomerEntity(
                name = nameClean,
                email = if (email.isBlank()) "${java.util.UUID.randomUUID()}@local.com" else email,
                phone = phoneClean
            )

            // Insert customer
            repository.insertCustomer(newCustomer)

            // Insert initial transaction if specified
            if (initialBalance > 0) {
                repository.insertTransaction(
                    TransactionEntity(
                        customerEmail = newCustomer.email,
                        customerName = newCustomer.name,
                        description = "প্রারম্ভিক ব্যালেন্স (${if (balanceType == "income") "জমা" else "বকেয়া"})",
                        amount = initialBalance,
                        type = balanceType
                    )
                )
            }

            showToast("কাস্টমার যুক্ত করা হয়েছে!")
            isLoading.value = false
        }
    }

    // Delete customer
    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            isLoading.value = true
            repository.deleteCustomer(customer)
            showToast("কাস্টমার মুছে ফেলা হয়েছে!")
            isLoading.value = false
        }
    }

    // Add manual standalone/linked transaction
    fun addTransaction(description: String, amountVal: Double, type: String, customerEmail: String, customerName: String) {
        if (amountVal <= 0) {
            showToast("সঠিক লেনদেন পরিমাণ প্রদান করুন")
            return
        }
        val desc = if (description.isBlank()) {
            if (type == "income") "নগদ জমা" else "পণ্য ক্রয়/বকেয়া"
        } else {
            description.trim()
        }

        viewModelScope.launch {
            isLoading.value = true
            val transaction = TransactionEntity(
                customerEmail = customerEmail,
                customerName = customerName,
                description = desc,
                amount = amountVal,
                type = type
            )
            repository.insertTransaction(transaction)
            showToast("লেনদেন সফলভাবে লিপিবদ্ধ হয়েছে!")
            isLoading.value = false
        }
    }

    // Delete Transaction
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            isLoading.value = true
            repository.deleteTransaction(transaction)
            showToast("লেনদনটি মুছে ফেলা হয়েছে!")
            isLoading.value = false
        }
    }

    // Update global ad link
    fun updateAdLink(newLink: String) {
        if (newLink.isBlank()) return
        viewModelScope.launch {
            repository.saveSetting(KEY_AD_LINK, newLink.trim())
            adLink.value = newLink.trim()
            showToast("বিজ্ঞাপন লিঙ্ক আপডেট হয়েছে!")
        }
    }
}
