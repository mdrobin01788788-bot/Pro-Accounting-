package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String, // String email as PK since it's unique
    val uid: String,
    val name: String,
    val password: String,
    val role: String, // "admin" or "customer"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerEmail: String, // "general" or linked to customer email
    val customerName: String, // display name for quick visual check
    val description: String,
    val amount: Double,
    val type: String, // "income" (জমা / Received) or "expense" (খরচ / Paid)
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val settingKey: String,
    val value: String
)

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE email = :email LIMIT 1")
    suspend fun getCustomerByEmail(email: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerEmail = :email ORDER BY timestamp DESC")
    fun getTransactionsByCustomer(email: String): Flow<List<TransactionEntity>>

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettingEntity)

    @Query("SELECT * FROM app_settings WHERE settingKey = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): AppSettingEntity?
}

@Database(entities = [UserEntity::class, CustomerEntity::class, TransactionEntity::class, AppSettingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pro_accounting_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AppRepository(private val appDao: AppDao) {
    val allCustomers: Flow<List<CustomerEntity>> = appDao.getAllCustomers()
    val allTransactions: Flow<List<TransactionEntity>> = appDao.getAllTransactions()

    fun getTransactionsForCustomer(email: String): Flow<List<TransactionEntity>> {
        return appDao.getTransactionsByCustomer(email)
    }

    suspend fun insertUser(user: UserEntity) = appDao.insertUser(user)
    suspend fun getUserByEmail(email: String) = appDao.getUserByEmail(email)

    suspend fun insertCustomer(customer: CustomerEntity) = appDao.insertCustomer(customer)
    suspend fun getCustomerByEmail(email: String) = appDao.getCustomerByEmail(email)
    suspend fun getCustomerById(id: Int) = appDao.getCustomerById(id)
    suspend fun deleteCustomer(customer: CustomerEntity) = appDao.deleteCustomer(customer)

    suspend fun insertTransaction(transaction: TransactionEntity) = appDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = appDao.deleteTransaction(transaction)

    suspend fun saveSetting(key: String, value: String) {
        appDao.insertSetting(AppSettingEntity(key, value))
    }

    suspend fun getSetting(key: String): String? {
        return appDao.getSettingByKey(key)?.value
    }
}
