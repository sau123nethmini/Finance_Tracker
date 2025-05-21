package com.example.finory.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finory.databinding.ItemTransactionBinding
import com.example.finory.model.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val currency: String,
    private val listener: OnTransactionClickListener
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    interface OnTransactionClickListener {
        fun onTransactionClick(transaction: Transaction)
        fun onTransactionLongClick(transaction: Transaction)
        fun onTransactionEditClick(transaction: Transaction)
        fun onTransactionDeleteClick(transaction: Transaction)
    }

    fun updateTransactions(newTransactions: List<Transaction>) {
        Log.d("TransactionsAdapter", "Updating transactions: old size=${transactions.size}, new size=${newTransactions.size}")
        if (newTransactions.isNotEmpty()) {
            Log.d("TransactionsAdapter", "First new transaction: ${newTransactions[0].id}, ${newTransactions[0].title}, ${newTransactions[0].date}")
        }
        
        transactions = newTransactions
        notifyDataSetChanged()
        
        Log.d("TransactionsAdapter", "After update, adapter item count: ${itemCount}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTransactionClick(transactions[position])
                }
            }

            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTransactionLongClick(transactions[position])
                    true
                } else {
                    false
                }
            }
            
            binding.btnEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTransactionEditClick(transactions[position])
                }
            }
            
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTransactionDeleteClick(transactions[position])
                }
            }
        }

        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title
            binding.tvCategory.text = transaction.category
            binding.tvDate.text = dateFormatter.format(transaction.date)

            val amountText = if (transaction.isExpense) {
                "- $currency ${transaction.amount}"
            } else {
                "+ $currency ${transaction.amount}"
            }

            binding.tvAmount.text = amountText
            binding.tvAmount.setTextColor(
                if (transaction.isExpense) {
                    binding.root.context.getColor(android.R.color.holo_red_dark)
                } else {
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                }
            )
        }
    }
}