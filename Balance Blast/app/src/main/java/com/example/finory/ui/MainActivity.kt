package com.example.finory.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.finory.R
import com.example.finory.data.PreferencesManager
import com.example.finory.data.TransactionRepository
import com.example.finory.databinding.ActivityMainBinding
import com.example.finory.notification.NotificationManager
import com.example.finory.ui.adapters.RecentTransactionsAdapter
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var adapter: RecentTransactionsAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val calendar = Calendar.getInstance()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
        private const val ADD_TRANSACTION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)
        notificationManager = NotificationManager(this)

        // Request notification permission
        requestNotificationPermission()

        notificationManager.scheduleDailyReminder()

        setupBottomNavigation()
        setupMonthSelector()
        setupSummaryCards()
        setupCategoryChart()
        setupRecentTransactions()

        binding.btnAddTransaction.setOnClickListener {
            startActivityForResult(Intent(this, AddTransactionActivity::class.java), ADD_TRANSACTION_REQUEST_CODE)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can show notifications
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Notification permission denied. You won't receive budget alerts.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
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
            updateDashboard()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDashboard()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }
    }

    private fun setupSummaryCards() {
        val currency = preferencesManager.getCurrency()
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        coroutineScope.launch {
            val totalIncome = transactionRepository.getTotalIncomeForMonth(month, year)
            val totalExpenses = transactionRepository.getTotalExpensesForMonth(month, year)
            val balance = totalIncome - totalExpenses

            binding.tvIncomeAmount.text = String.format("%s %.2f", currency, totalIncome)
            binding.tvExpenseAmount.text = String.format("%s %.2f", currency, totalExpenses)
            binding.tvBalanceAmount.text = String.format("%s %.2f", currency, balance)

            // Budget progress
            val budget = preferencesManager.getBudget()
            if (budget.month == month &&
                budget.year == year &&
                budget.amount > 0) {

                val percentage = (totalExpenses / budget.amount) * 100
                binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)
                binding.tvBudgetStatus.text = String.format(
                    "%.1f%% of %s %.2f", percentage, currency, budget.amount
                )

                if (percentage >= 100) {
                    binding.tvBudgetStatus.setTextColor(Color.RED)
                } else if (percentage >= 80) {
                    binding.tvBudgetStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
                } else {
                    binding.tvBudgetStatus.setTextColor(Color.GREEN)
                }
            } else {
                binding.progressBudget.progress = 0
                binding.tvBudgetStatus.text = getString(R.string.no_budget_set)
                binding.tvBudgetStatus.setTextColor(Color.GRAY)
            }
        }
    }

    private fun setupCategoryChart() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        coroutineScope.launch {
            val expensesByCategory = transactionRepository.getExpensesByCategory(month, year)

            if (expensesByCategory.isEmpty()) {
                binding.pieChart.setNoDataText(getString(R.string.no_expenses_this_month))
                binding.pieChart.invalidate()
                return@launch
            }

            val entries = ArrayList<PieEntry>()
            val colors = ArrayList<Int>()

            expensesByCategory.forEach { (category, amount) ->
                entries.add(PieEntry(amount.toFloat(), category))
                colors.add(ColorTemplate.MATERIAL_COLORS[entries.size % ColorTemplate.MATERIAL_COLORS.size])
            }

            val dataSet = PieDataSet(entries, "Categories")
            dataSet.colors = colors
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.WHITE

            val pieData = PieData(dataSet)
            binding.pieChart.data = pieData
            binding.pieChart.description.isEnabled = false
            binding.pieChart.centerText = getString(R.string.expenses_by_category)
            binding.pieChart.setCenterTextSize(14f)
            binding.pieChart.legend.textSize = 12f
            binding.pieChart.animateY(1000)
            binding.pieChart.invalidate()
        }
    }

    private fun setupRecentTransactions() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        coroutineScope.launch {
            val transactions = transactionRepository.getTransactionsForMonth(month, year)
                .sortedByDescending { it.date }
                .take(5)

            adapter = RecentTransactionsAdapter(transactions, preferencesManager.getCurrency())
            binding.recyclerRecentTransactions.adapter = adapter

            binding.tvViewAllTransactions.setOnClickListener {
                startActivity(Intent(this@MainActivity, TransactionsActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }

    private fun updateDashboard() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val currency = preferencesManager.getCurrency()

        coroutineScope.launch {
            val totalIncome = transactionRepository.getTotalIncomeForMonth(month, year)
            val totalExpenses = transactionRepository.getTotalExpensesForMonth(month, year)
            val balance = totalIncome - totalExpenses

            binding.tvIncomeAmount.text = String.format("%s %.2f", currency, totalIncome)
            binding.tvExpenseAmount.text = String.format("%s %.2f", currency, totalExpenses)
            binding.tvBalanceAmount.text = String.format("%s %.2f", currency, balance)

            if (balance >= 0) {
                binding.tvBalanceAmount.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
            } else {
                binding.tvBalanceAmount.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
            }
        }

        setupSummaryCards()
        setupCategoryChart()
        setupRecentTransactions()
    }

    override fun onResume() {
        super.onResume()

        // Debug logging
        val budget = preferencesManager.getBudget()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        coroutineScope.launch {
            if (budget.month == currentMonth && budget.year == currentYear && budget.amount > 0) {
                val totalExpenses = transactionRepository.getTotalExpensesForMonth(currentMonth, currentYear)
                val budgetPercentage = (totalExpenses / budget.amount) * 100

                Log.d("MainActivity", "Budget: $totalExpenses / ${budget.amount} = $budgetPercentage%")
                Log.d("MainActivity", "Notifications enabled: ${preferencesManager.isNotificationEnabled()}")
            }

            updateDashboard()
            // Check budget and notify
            notificationManager.checkBudgetAndNotify()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_TRANSACTION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Refresh the dashboard when a transaction is added
            updateDashboard()
        }
    }
}