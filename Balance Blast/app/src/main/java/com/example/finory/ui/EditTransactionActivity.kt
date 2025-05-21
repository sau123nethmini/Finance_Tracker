package com.example.finory.ui

import android.app.DatePickerDialog
import android.os.Bundle
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
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationManager: NotificationManager
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var transaction: Transaction
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        notificationManager = NotificationManager(this)

        // Get transaction data from intent
        val id = intent.getStringExtra("transaction_id") ?: ""
        val title = intent.getStringExtra("transaction_title") ?: ""
        val amount = intent.getDoubleExtra("transaction_amount", 0.0)
        val category = intent.getStringExtra("transaction_category") ?: ""
        val date = Date(intent.getLongExtra("transaction_date", System.currentTimeMillis()))
        val isExpense = intent.getBooleanExtra("transaction_is_expense", true)

        transaction = Transaction(id, title, amount, category, date, isExpense)
        calendar.time = transaction.date

        setupCategorySpinner()
        setupDatePicker()
        setupButtons()
        populateFields()
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
            showDatePicker()
        }
    }

    private fun populateFields() {
        binding.dialogTitle.text = getString(R.string.edit_transaction)
        binding.etTitle.setText(transaction.title)
        binding.etAmount.setText(transaction.amount.toString())

        val categoryPosition = Category.DEFAULT_CATEGORIES.indexOf(transaction.category)
        if (categoryPosition >= 0) {
            binding.spinnerCategory.setSelection(categoryPosition)
        }

        if (transaction.isExpense) {
            binding.radioExpense.isChecked = true
        } else {
            binding.radioIncome.isChecked = true
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

            val updatedTransaction = Transaction(
                id = transaction.id,
                title = title,
                amount = amount,
                category = category,
                date = date,
                isExpense = isExpense
            )

            coroutineScope.launch {
                transactionRepository.saveTransaction(updatedTransaction)
                notificationManager.checkBudgetAndNotify()
                Toast.makeText(
                    this@EditTransactionActivity,
                    getString(R.string.transaction_updated),
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.error_updating_transaction),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDatePicker() {
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