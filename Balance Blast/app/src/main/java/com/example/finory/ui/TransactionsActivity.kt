package com.example.finory.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.finory.R
import com.example.finory.data.PreferencesManager
import com.example.finory.data.TransactionRepository
import com.example.finory.databinding.ActivityTransactionsBinding
import com.example.finory.model.Transaction
import com.example.finory.ui.adapters.TransactionsAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TransactionsActivity : AppCompatActivity(), TransactionsAdapter.OnTransactionClickListener {
    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adapter: TransactionsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var currentJob: Job? = null

    private val calendar = Calendar.getInstance()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)

        setupBottomNavigation()
        setupMonthSelector()
        setupFilterSpinner()
        setupTransactionsList()

        binding.fabAddTransaction.setOnClickListener {
            startActivityForResult(Intent(this, AddTransactionActivity::class.java), ADD_TRANSACTION_REQUEST_CODE)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_transactions
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_transactions -> true
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMonthSelector() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time)

        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateTransactionsList()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateTransactionsList()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("All", "Expenses", "Income")
        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, filters
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = spinnerAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = filters[position].lowercase()
                updateTransactionsList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTransactionsList() {
        // Make sure recyclerview has a layout manager
        if (binding.recyclerTransactions.layoutManager == null) {
            Log.d("TransactionsActivity", "Setting layout manager for RecyclerView")
            binding.recyclerTransactions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        }
        
        // Add item decoration for spacing
        if (binding.recyclerTransactions.itemDecorationCount == 0) {
            binding.recyclerTransactions.addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    this, 
                    androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
                )
            )
        }

        currentJob?.cancel()
        currentJob = coroutineScope.launch {
            try {
                Log.d("TransactionsActivity", "Setting up transactions list")
                val transactions = getFilteredTransactions()
                if (isFinishing) return@launch
                
                Log.d("TransactionsActivity", "Creating adapter with ${transactions.size} transactions")
                adapter = TransactionsAdapter(transactions, preferencesManager.getCurrency(), this@TransactionsActivity)
                binding.recyclerTransactions.adapter = adapter
                
                // Check if adapter is properly set
                val currentAdapter = binding.recyclerTransactions.adapter
                Log.d("TransactionsActivity", "RecyclerView adapter is set: ${currentAdapter != null}, item count: ${currentAdapter?.itemCount ?: 0}")
                
            } catch (e: Exception) {
                Log.e("TransactionsActivity", "Error setting up transactions list", e)
                e.printStackTrace()
            }
        }
    }

    private suspend fun getFilteredTransactions(): List<Transaction> {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        Log.d("TransactionsActivity", "Fetching transactions for month: $month, year: $year, filter: $currentFilter")
        
        return try {
            val transactions = when (currentFilter) {
                "expenses" -> transactionRepository.getExpensesForMonth(month, year)
                "income" -> transactionRepository.getIncomeForMonth(month, year)
                else -> transactionRepository.getTransactionsForMonth(month, year)
            }.sortedByDescending { it.date }
            
            Log.d("TransactionsActivity", "Fetched ${transactions.size} transactions")
            if (transactions.isNotEmpty()) {
                Log.d("TransactionsActivity", "First transaction: ${transactions[0].title}, ${transactions[0].amount}")
            }
            
            transactions
        } catch (e: Exception) {
            Log.e("TransactionsActivity", "Error fetching transactions", e)
            e.printStackTrace()
            emptyList()
        }
    }

    private fun updateTransactionsList() {
        currentJob?.cancel()
        currentJob = coroutineScope.launch {
            try {
                Log.d("TransactionsActivity", "Updating transactions list")
                val transactions = getFilteredTransactions()
                if (isFinishing) return@launch
                
                Log.d("TransactionsActivity", "Updating adapter with ${transactions.size} transactions")
                if (this@TransactionsActivity::adapter.isInitialized) {
                    adapter.updateTransactions(transactions)
                    Log.d("TransactionsActivity", "Adapter updated, new item count: ${adapter.itemCount}")
                } else {
                    Log.d("TransactionsActivity", "Adapter not initialized, creating new one")
                    adapter = TransactionsAdapter(transactions, preferencesManager.getCurrency(), this@TransactionsActivity)
                    binding.recyclerTransactions.adapter = adapter
                }
                
                // Show empty state if needed
                if (transactions.isEmpty()) {
                    Log.d("TransactionsActivity", "No transactions found, showing empty state")
                    binding.recyclerTransactions.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = when (currentFilter) {
                        "expenses" -> getString(R.string.no_expenses_this_month)
                        "income" -> getString(R.string.no_income_this_month)
                        else -> getString(R.string.no_transactions_this_month)
                    }
                } else {
                    Log.d("TransactionsActivity", "Transactions found, hiding empty state")
                    binding.recyclerTransactions.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                
            } catch (e: Exception) {
                Log.e("TransactionsActivity", "Error updating transactions list", e)
                e.printStackTrace()
            }
        }
    }

    override fun onTransactionClick(transaction: Transaction) {
        val intent = Intent(this, EditTransactionActivity::class.java).apply {
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_title", transaction.title)
            putExtra("transaction_amount", transaction.amount)
            putExtra("transaction_category", transaction.category)
            putExtra("transaction_date", transaction.date.time)
            putExtra("transaction_is_expense", transaction.isExpense)
        }
        startActivityForResult(intent, EDIT_TRANSACTION_REQUEST_CODE)
    }

    override fun onTransactionLongClick(transaction: Transaction) {
        val intent = Intent(this, DeleteTransactionActivity::class.java).apply {
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_title", transaction.title)
        }
        startActivityForResult(intent, DELETE_TRANSACTION_REQUEST_CODE)
    }

    override fun onTransactionEditClick(transaction: Transaction) {
        val intent = Intent(this, EditTransactionActivity::class.java).apply {
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_title", transaction.title)
            putExtra("transaction_amount", transaction.amount)
            putExtra("transaction_category", transaction.category)
            putExtra("transaction_date", transaction.date.time)
            putExtra("transaction_is_expense", transaction.isExpense)
        }
        startActivityForResult(intent, EDIT_TRANSACTION_REQUEST_CODE)
    }

    override fun onTransactionDeleteClick(transaction: Transaction) {
        val intent = Intent(this, DeleteTransactionActivity::class.java).apply {
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_title", transaction.title)
        }
        startActivityForResult(intent, DELETE_TRANSACTION_REQUEST_CODE)
    }

    override fun onResume() {
        super.onResume()
        updateTransactionsList()
        debugCheckAllTransactions()
        checkRecyclerViewVisibility()
    }

    private fun debugCheckAllTransactions() {
        coroutineScope.launch {
            try {
                val allTransactions = transactionRepository.getAllTransactions()
                Log.d("TransactionsActivity", "DEBUG - All transactions count: ${allTransactions.size}")
                
                if (allTransactions.isNotEmpty()) {
                    allTransactions.forEach { transaction ->
                        Log.d("TransactionsActivity", "Transaction: id=${transaction.id}, title=${transaction.title}, " +
                              "date=${transaction.date}, amount=${transaction.amount}, isExpense=${transaction.isExpense}")
                    }
                } else {
                    Log.d("TransactionsActivity", "DEBUG - No transactions found in the database!")
                }
            } catch (e: Exception) {
                Log.e("TransactionsActivity", "Error checking all transactions", e)
            }
        }
    }

    private fun checkRecyclerViewVisibility() {
        Log.d("TransactionsActivity", "RecyclerView visibility check:")
        Log.d("TransactionsActivity", "Visibility: ${binding.recyclerTransactions.visibility == View.VISIBLE}")
        Log.d("TransactionsActivity", "Width: ${binding.recyclerTransactions.width}, Height: ${binding.recyclerTransactions.height}")
        Log.d("TransactionsActivity", "Alpha: ${binding.recyclerTransactions.alpha}")
        
        // Make sure RecyclerView is visible
        binding.recyclerTransactions.visibility = View.VISIBLE
        binding.recyclerTransactions.alpha = 1.0f

        // Post a runnable to check after layout
        binding.recyclerTransactions.post {
            Log.d("TransactionsActivity", "After layout - Width: ${binding.recyclerTransactions.width}, Height: ${binding.recyclerTransactions.height}")
            
            if (this::adapter.isInitialized) {
                Log.d("TransactionsActivity", "Adapter item count: ${adapter.itemCount}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentJob?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                DELETE_TRANSACTION_REQUEST_CODE,
                ADD_TRANSACTION_REQUEST_CODE,
                EDIT_TRANSACTION_REQUEST_CODE -> updateTransactionsList()
            }
        }
    }

    companion object {
        private const val DELETE_TRANSACTION_REQUEST_CODE = 1001
        private const val ADD_TRANSACTION_REQUEST_CODE = 1002
        private const val EDIT_TRANSACTION_REQUEST_CODE = 1003
    }
}