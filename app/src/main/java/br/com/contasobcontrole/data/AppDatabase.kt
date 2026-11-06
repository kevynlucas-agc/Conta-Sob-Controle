package br.com.contasobcontrole.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Expense::class, MeterReading::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ExpenseDao
}
