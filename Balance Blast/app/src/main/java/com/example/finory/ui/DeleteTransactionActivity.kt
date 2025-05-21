package com.example.finory.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finory.R
import com.example.finory.data.TransactionRepository
import com.example.finory.databinding.ActivityDeleteTransactionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DeleteTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeleteTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        
        // Get transaction ID and title from intent
        val transactionId = intent.getStringExtra("transaction_id") ?: ""
        val transactionTitle = intent.getStringExtra("transaction_title") ?: ""

        binding.tvDeleteConfirmation.text = getString(
            R.string.delete_transaction_confirmation,
            transactionTitle
        )

        binding.btnDelete.setOnClickListener {
            coroutineScope.launch {
                transactionRepository.deleteTransaction(transactionId)
                Toast.makeText(
                    this@DeleteTransactionActivity,
                    getString(R.string.transaction_deleted),
                    Toast.LENGTH_SHORT
                ).show()
                
                // Set result to indicate data was changed
                setResult(RESULT_OK)
                finish()
            }
        }

        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}