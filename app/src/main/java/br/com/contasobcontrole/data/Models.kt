package br.com.contasobcontrole.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val amountCents: Long,
    val dueDate: String,
    val paidDate: String? = null,
    val description: String = "",
    val recurringMonthly: Boolean = false
) {
    val isPaid: Boolean get() = paidDate != null
}

@Entity(tableName = "readings")
data class MeterReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val reading: Double,
    val date: String,
    val billCents: Long = 0
)
