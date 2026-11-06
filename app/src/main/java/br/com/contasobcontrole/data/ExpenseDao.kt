package br.com.contasobcontrole.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dueDate DESC")
    fun observeAll(): Flow<List<Expense>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Expense): Long
    @Delete suspend fun delete(item: Expense)
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun find(id: Long): Expense?
    @Query("SELECT COUNT(*) FROM expenses WHERE name = :name AND category = :category AND substr(dueDate, 1, 7) = :month")
    suspend fun countInMonth(name: String, category: String, month: String): Int
    @Query("SELECT * FROM readings ORDER BY date ASC")
    fun observeReadings(): Flow<List<MeterReading>>
    @Insert suspend fun addReading(item: MeterReading)
    @Query("DELETE FROM expenses") suspend fun clearExpenses()
    @Query("DELETE FROM readings") suspend fun clearReadings()
    @Insert suspend fun insertExpenses(items: List<Expense>)
    @Insert suspend fun insertReadings(items: List<MeterReading>)
    @Transaction
    suspend fun replaceAll(expenses: List<Expense>, readings: List<MeterReading>) {
        clearExpenses(); clearReadings()
        insertExpenses(expenses); insertReadings(readings)
    }
}
