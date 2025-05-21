package com.example.finory.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finory.R
import com.example.finory.data.TransactionRepository
import com.example.finory.databinding.ActivityAddTransactionBinding
import com.example.finory.model.Category
import com.example.finory.model.Transaction
import com.example.finory.notification.NotificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationManager: NotificationManager
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        notificationManager = NotificationManager(this)

        setupCategorySpinner()
        setupDatePicker()
        setupButtons()
    }

    private fun setupCategorySpinner() {
        val categories = Category.DEFAULT_CATEGORIES.toTypedArray()
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.etDate.setText(dateFormatter.format(calendar.time))

        binding.etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    binding.etDate.setText(dateFormatter.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.etTitle.error = getString(R.string.field_required)
            isValid = false
        }

        if (binding.etAmount.text.toString().trim().isEmpty()) {
            binding.etAmount.error = getString(R.string.field_required)
            isValid = false
        }

        return isValid
    }

    private fun saveTransaction() {
        try {
            val title = binding.etTitle.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDouble()
            val category = binding.spinnerCategory.selectedItem.toString()
            val date = calendar.time
            val isExpense = binding.radioExpense.isChecked

            // Log calendar and date details for debugging
            Log.d("AddTransactionActivity", "Calendar year: ${calendar.get(Calendar.YEAR)}, month: ${calendar.get(Calendar.MONTH)}, day: ${calendar.get(Calendar.DAY_OF_MONTH)}")
            Log.d("AddTransactionActivity", "Creating transaction with date: $date, timestamp: ${date.time}")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d("AddTransactionActivity", "Formatted date: ${dateFormat.format(date)}")

            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                category = category,
                date = date,
                isExpense = isExpense
            )

            coroutineScope.launch {
                Log.d("AddTransactionActivity", "Saving transaction to repository")
                transactionRepository.saveTransaction(transaction)
                notificationManager.checkBudgetAndNotify()
                
                // Log total transactions after saving
                val allTransactions = transactionRepository.getAllTransactions()
                Log.d("AddTransactionActivity", "Total transactions after save: ${allTransactions.size}")
                
                Toast.makeText(
                    this@AddTransactionActivity,
                    getString(R.string.transaction_added),
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            }
        } catch (e: Exception) {
            Log.e("AddTransactionActivity", "Error saving transaction", e)
            Toast.makeText(
                this,
                getString(R.string.error_adding_transaction),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}