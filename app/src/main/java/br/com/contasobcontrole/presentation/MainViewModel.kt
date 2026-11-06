package br.com.contasobcontrole.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.contasobcontrole.data.*
import br.com.contasobcontrole.notifications.DueDateReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FinanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val readings = repository.readings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val budget = repository.monthlyBudget.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val income = repository.income.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val themeMode = repository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    init { viewModelScope.launch {
        repository.ensureRecurringForMonth(YearMonth.now())
        repository.expenses.first().filter { !it.isPaid }.forEach { DueDateReceiver.schedule(context, it.id, it.name, it.dueDate) }
    } }

    fun save(name: String, category: String, cents: Long, due: String, recurring: Boolean, description: String = "") {
        if (name.isBlank() || cents <= 0 || runCatching { LocalDate.parse(due) }.isFailure) return
        viewModelScope.launch {
            val id = repository.save(Expense(name=name.trim(), category=category, amountCents=cents, dueDate=due, recurringMonthly=recurring, description=description))
            DueDateReceiver.schedule(context, id, name, due)
        }
    }
    fun update(item: Expense) = viewModelScope.launch {
        repository.save(item); DueDateReceiver.cancel(context, item.id)
        if (!item.isPaid) DueDateReceiver.schedule(context, item.id, item.name, item.dueDate)
    }
    fun delete(item: Expense) = viewModelScope.launch { DueDateReceiver.cancel(context, item.id); repository.delete(item) }
    fun pay(item: Expense) = viewModelScope.launch { DueDateReceiver.cancel(context, item.id); repository.pay(item, LocalDate.now().toString()) }
    fun addReading(type: String, value: Double, date: String, billCents: Long) {
        if (value >= 0 && runCatching { LocalDate.parse(date) }.isSuccess)
            viewModelScope.launch { repository.addReading(MeterReading(type=type, reading=value, date=date, billCents=billCents)) }
    }
    fun setBudget(cents: Long) = viewModelScope.launch { repository.setBudget(cents.coerceAtLeast(0)) }
    fun setIncome(cents: Long) = viewModelScope.launch { repository.setIncome(cents.coerceAtLeast(0)) }
    fun setTheme(mode: String) = viewModelScope.launch { repository.setTheme(mode) }
    suspend fun backupData() = expenses.value to readings.value
    fun restore(e: List<Expense>, r: List<MeterReading>, savedIncome: Long? = null, savedBudget: Long? = null, savedTheme: String? = null) = viewModelScope.launch {
        expenses.value.forEach { DueDateReceiver.cancel(context, it.id) }
        repository.replaceAll(e, r)
        savedIncome?.let { repository.setIncome(it) }
        savedBudget?.let { repository.setBudget(it) }
        savedTheme?.let { repository.setTheme(it) }
        e.filter { !it.isPaid }.forEach { DueDateReceiver.schedule(context, it.id, it.name, it.dueDate) }
    }
}
