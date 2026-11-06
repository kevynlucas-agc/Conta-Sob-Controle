package br.com.contasobcontrole.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.financeStore by preferencesDataStore("finance_preferences")

@Singleton
class FinanceRepository @Inject constructor(
    private val dao: ExpenseDao,
    @ApplicationContext private val context: Context
) {
    val expenses = dao.observeAll()
    val readings = dao.observeReadings()
    val monthlyBudget: Flow<Long> = context.financeStore.data.map { it[longPreferencesKey("budget")] ?: 0L }
    val income: Flow<Long> = context.financeStore.data.map { it[longPreferencesKey("income")] ?: 0L }
    val themeMode: Flow<String> = context.financeStore.data.map { it[stringPreferencesKey("theme")] ?: "system" }

    suspend fun save(expense: Expense) = dao.upsert(expense)
    suspend fun delete(expense: Expense) = dao.delete(expense)
    suspend fun pay(expense: Expense, date: String) = dao.upsert(expense.copy(paidDate = date))
    suspend fun addReading(reading: MeterReading) = dao.addReading(reading)
    suspend fun setBudget(value: Long) { context.financeStore.edit { it[longPreferencesKey("budget")] = value } }
    suspend fun setIncome(value: Long) { context.financeStore.edit { it[longPreferencesKey("income")] = value } }
    suspend fun setTheme(value: String) { context.financeStore.edit { it[stringPreferencesKey("theme")] = value } }
    suspend fun ensureRecurringForMonth(month: java.time.YearMonth) {
        val all = dao.observeAll().first()
        all.filter { it.recurringMonthly && runCatching { java.time.YearMonth.parse(it.dueDate.substring(0, 7)) < month }.getOrDefault(false) }
            .forEach { template ->
                if (dao.countInMonth(template.name, template.category, month.toString()) == 0) {
                    val day = template.dueDate.substring(8, 10).toInt()
                    val date = month.atDay(day.coerceAtMost(month.lengthOfMonth())).toString()
                    dao.upsert(template.copy(id = 0, dueDate = date, paidDate = null))
                }
            }
    }

    suspend fun replaceAll(expenses: List<Expense>, readings: List<MeterReading>) {
        dao.replaceAll(expenses, readings)
    }
}
