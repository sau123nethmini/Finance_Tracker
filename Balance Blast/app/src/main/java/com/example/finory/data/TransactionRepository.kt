package com.example.finory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.example.finory.model.Transaction
import java.util.Calendar
import java.io.File
import android.util.Log
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val isExpense: Boolean,
    val title: String
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE strftime('%m', date/1000, 'unixepoch') = :month AND strftime('%Y', date/1000, 'unixepoch') = :year")
    suspend fun getTransactionsForMonth(month: String, year: String): List<TransactionEntity>
    
    // Add a simple query to test time-based filtering
    @Query("SELECT * FROM transactions WHERE date > :startTime AND date < :endTime")
    suspend fun getTransactionsInTimeRange(startTime: Long, endTime: Long): List<TransactionEntity>
}

@Database(entities = [TransactionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

class TransactionRepository(context: Context) {
    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "transactions_database"
    ).fallbackToDestructiveMigration()
     .build()
    private val transactionDao = db.transactionDao()
    private val context = context.applicationContext

    suspend fun saveTransaction(transaction: Transaction) {
        try {
            Log.d("TransactionRepository", "Saving transaction: id=${transaction.id}, title=${transaction.title}, date=${transaction.date}")
            
            val entity = TransactionEntity(
                id = transaction.id,
                amount = transaction.amount,
                category = transaction.category,
                date = transaction.date.time,
                isExpense = transaction.isExpense,
                title = transaction.title
            )
            
            // Check if the transaction already exists
            val existingTransaction = transactionDao.getAllTransactions().find { it.id == transaction.id }
            if (existingTransaction != null) {
                Log.d("TransactionRepository", "Updating existing transaction: ${transaction.id}")
                transactionDao.update(entity)
            } else {
                Log.d("TransactionRepository", "Inserting new transaction: ${transaction.id}")
                transactionDao.insert(entity)
            }
            
            // Verify the transaction was saved
            val allTransactions = transactionDao.getAllTransactions()
            Log.d("TransactionRepository", "Total transactions after save: ${allTransactions.size}")
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error saving transaction", e)
            e.printStackTrace()
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        try {
            val transaction = transactionDao.getAllTransactions().find { it.id == transactionId }
            transaction?.let { transactionDao.delete(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAllTransactions(): List<Transaction> {
        return try {
            transactionDao.getAllTransactions().map { entity ->
                Transaction(
                    id = entity.id,
                    amount = entity.amount,
                    category = entity.category,
                    date = java.util.Date(entity.date),
                    isExpense = entity.isExpense,
                    title = entity.title
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTransactionsForMonth(month: Int, year: Int): List<Transaction> {
        return try {
            val monthStr = String.format("%02d", month + 1)
            val yearStr = year.toString()
            
            Log.d("TransactionRepository", "Getting transactions for month: $monthStr, year: $yearStr")
            
            // First check if the database has any transactions
            val allTransactions = transactionDao.getAllTransactions()
            Log.d("TransactionRepository", "Total transactions in DB: ${allTransactions.size}")
            
            // Calculate start and end timestamps for the month
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            val startOfNextMonth = calendar.timeInMillis
            
            Log.d("TransactionRepository", "Fetching transactions between ${java.util.Date(startOfMonth)} and ${java.util.Date(startOfNextMonth)}")
            
            // Use the new time-range based query
            val transactions = transactionDao.getTransactionsInTimeRange(startOfMonth, startOfNextMonth).map { entity ->
                Transaction(
                    id = entity.id,
                    amount = entity.amount,
                    category = entity.category,
                    date = java.util.Date(entity.date),
                    isExpense = entity.isExpense,
                    title = entity.title
                )
            }
            Log.d("TransactionRepository", "Found ${transactions.size} transactions for month: $monthStr, year: $yearStr")
            
            transactions
        } catch (e: Exception) {
            Log.e("TransactionRepository", "Error getting transactions", e)
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getExpensesForMonth(month: Int, year: Int): List<Transaction> {
        return getTransactionsForMonth(month, year).filter { it.isExpense }
    }

    suspend fun getIncomeForMonth(month: Int, year: Int): List<Transaction> {
        return getTransactionsForMonth(month, year).filter { !it.isExpense }
    }

    suspend fun getTotalExpensesForMonth(month: Int, year: Int): Double {
        return getExpensesForMonth(month, year).sumOf { it.amount }
    }

    suspend fun getTotalIncomeForMonth(month: Int, year: Int): Double {
        return getIncomeForMonth(month, year).sumOf { it.amount }
    }

    suspend fun getExpensesByCategory(month: Int, year: Int): Map<String, Double> {
        val expenses = getExpensesForMonth(month, year)
        return expenses.groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    suspend fun backupToUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val transactions = getAllTransactions()
            val json = com.google.gson.Gson().toJson(transactions.map { it.toMap() })
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return false

            val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
            val transactions = com.google.gson.Gson().fromJson<List<Map<String, Any>>>(json, type)
                .map { Transaction.fromMap(it) }

            // Clear existing transactions
            transactionDao.getAllTransactions().forEach { transactionDao.delete(it) }

            // Insert restored transactions
            transactions.forEach { transaction ->
                val entity = TransactionEntity(
                    id = transaction.id,
                    amount = transaction.amount,
                    category = transaction.category,
                    date = transaction.date.time,
                    isExpense = transaction.isExpense,
                    title = transaction.title
                )
                transactionDao.insert(entity)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromInternalStorage(context: Context): Boolean {
        val backupFile = File(context.filesDir, "transactions_backup.json")
        if (!backupFile.exists()) return false

        return try {
            val json = backupFile.readText()
            val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
            val transactions = com.google.gson.Gson().fromJson<List<Map<String, Any>>>(json, type)
                .map { Transaction.fromMap(it) }

            // Clear existing transactions
            transactionDao.getAllTransactions().forEach { transactionDao.delete(it) }

            // Insert restored transactions
            transactions.forEach { transaction ->
                val entity = TransactionEntity(
                    id = transaction.id,
                    amount = transaction.amount,
                    category = transaction.category,
                    date = transaction.date.time,
                    isExpense = transaction.isExpense,
                    title = transaction.title
                )
                transactionDao.insert(entity)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}