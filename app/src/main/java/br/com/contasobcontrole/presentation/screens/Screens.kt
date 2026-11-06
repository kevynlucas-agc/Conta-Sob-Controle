package br.com.contasobcontrole.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.contasobcontrole.data.Expense
import br.com.contasobcontrole.data.MeterReading
import br.com.contasobcontrole.presentation.MainViewModel
import br.com.contasobcontrole.domain.FinanceMath
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private fun money(cents: Long) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cents / 100.0)
private fun parseCents(value: String): Long? = value.trim().replace(".", "").replace(",", ".")
    .toBigDecimalOrNull()?.movePointRight(2)?.toLong()

@Composable private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        content()
    }
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp)); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    } }
}
@Composable private fun EmptyHint(text: String) {
    Card(shape = RoundedCornerShape(18.dp)) { Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
fun DashboardScreen(vm: MainViewModel, onCalendar: () -> Unit = {}) {
    val expenses by vm.expenses.collectAsState()
    val income by vm.income.collectAsState()
    val budget by vm.budget.collectAsState()
    val month = YearMonth.now().toString()
    val current = expenses.filter { it.dueDate.startsWith(month) }
    val total = current.sumOf { it.amountCents }
    val previousMonthTotal = expenses.filter { it.dueDate.startsWith(YearMonth.now().minusMonths(1).toString()) }.sumOf { it.amountCents }
    val paid = current.filter { it.isPaid }.sumOf { it.amountCents }
    val pending = current.filterNot { it.isPaid }.sumOf { it.amountCents }
    val pct = FinanceMath.percent(total, budget).toInt().coerceAtLeast(0)
    val next = expenses.filter { !it.isPaid && it.dueDate >= LocalDate.now().toString() }.sortedBy { it.dueDate }.take(4)
    Page("Olá! Suas contas") {
        Text("Resumo de ${YearMonth.now().month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }}")
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Metric("Despesas do mês", money(total), Modifier.weight(1f))
            Metric("Saldo estimado", money(income - total), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Metric("Contas pagas", money(paid), Modifier.weight(1f))
            Metric("Pendentes", money(pending), Modifier.weight(1f))
        }
        Text(
            if (previousMonthTotal == 0L) "Sem histórico do mês anterior para comparação."
            else {
                val delta = total - previousMonthTotal
                val pctChange = FinanceMath.percent(kotlin.math.abs(delta), previousMonthTotal).toPlainString()
                if (delta > 0) "Gastos $pctChange% maiores que no mês anterior."
                else if (delta < 0) "Gastos $pctChange% menores que no mês anterior."
                else "Gastos iguais aos do mês anterior."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(17.dp)) {
            Text("Orçamento mensal", fontWeight = FontWeight.SemiBold)
            Text(if (budget > 0) "$pct% utilizado" else "Defina seu orçamento em Configurações")
            LinearProgressIndicator(progress = { if (budget > 0) (total.toFloat()/budget).coerceIn(0f,1f) else 0f }, modifier = Modifier.fillMaxWidth().padding(vertical=9.dp))
            Text(if (budget > 0) "Você já utilizou $pct% do seu orçamento." else "Comece definindo seu limite mensal.", style = MaterialTheme.typography.bodySmall)
        } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Próximos vencimentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onCalendar) { Text("Ver calendário") }
        }
        if (next.isEmpty()) EmptyHint("Nenhuma conta pendente cadastrada.")
        next.forEach { Text("• ${it.name} — ${money(it.amountCents)} · ${it.dueDate}") }
    }
}

@Composable
fun BillsScreen(vm: MainViewModel, onAdd: () -> Unit, onCalendar: () -> Unit = {}) {
    val expenses by vm.expenses.collectAsState()
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }
    Page("Minhas contas") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${expenses.size} lançamentos")
            Row {
                TextButton(onClick = onCalendar) { Icon(Icons.Default.CalendarMonth, null); Text("Calendário") }
                TextButton(onClick = onAdd) { Icon(Icons.Default.Add, null); Text("Adicionar") }
            }
        }
        if (expenses.isEmpty()) EmptyHint("Você ainda não cadastrou despesas. Toque em Adicionar para começar.")
        expenses.forEach { item ->
            Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text("${item.category} · vence ${item.dueDate}", style = MaterialTheme.typography.bodySmall)
                    Text(money(item.amountCents), color = MaterialTheme.colorScheme.primary)
                    Text(when { item.isPaid -> "Paga"; item.dueDate < LocalDate.now().toString() -> "Atrasada"; else -> "Pendente" })
                }
                Column {
                    IconButton(onClick = { editTarget = item }) { Icon(Icons.Default.Edit, "Editar") }
                    if (!item.isPaid) IconButton(onClick = { vm.pay(item) }) { Icon(Icons.Default.CheckCircle, "Marcar como paga", tint = Color(0xFF2E9D68)) }
                    IconButton(onClick = { deleteTarget = item }) { Icon(Icons.Default.Delete, "Excluir") }
                }
            } }
        }
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir conta?") },
            text = { Text("A conta ${target.name} será removida deste aparelho. Esta ação não pode ser desfeita.") },
            confirmButton = { TextButton(onClick = { vm.delete(target); deleteTarget = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
    editTarget?.let { target ->
        var editName by remember(target.id) { mutableStateOf(target.name) }
        var editAmount by remember(target.id) { mutableStateOf("%.2f".format(Locale.US, target.amountCents / 100.0)) }
        var editDue by remember(target.id) { mutableStateOf(target.dueDate) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Editar conta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editName, { editName = it }, label = { Text("Nome") })
                    OutlinedTextField(editAmount, { editAmount = it }, label = { Text("Valor (R$)") })
                    OutlinedTextField(editDue, { editDue = it }, label = { Text("Vencimento (AAAA-MM-DD)") })
                }
            },
            confirmButton = { TextButton(onClick = {
                val cents = parseCents(editAmount)
                if (editName.isNotBlank() && cents != null && cents > 0 && runCatching { LocalDate.parse(editDue) }.isSuccess) {
                    vm.update(target.copy(name = editName, amountCents = cents, dueDate = editDue)); editTarget = null
                }
            }) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun AddExpenseScreen(vm: MainViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Outras despesas") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var description by remember { mutableStateOf("") }
    var recurring by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    Page("Nova despesa") {
        OutlinedTextField(name, { name = it }, label = { Text("Nome da conta") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        val categories = listOf("Água", "Energia elétrica", "Internet", "Aluguel", "Gás", "Telefone", "Condomínio", "Supermercado", "Transporte", "Outras despesas")
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded, { expanded = it }) {
            OutlinedTextField(category, {}, readOnly = true, label = { Text("Categoria") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            ExposedDropdownMenu(expanded, { expanded = false }) { categories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; expanded = false }) } }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("Valor em R$ (ex.: 125,90)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(due, { due = it }, label = { Text("Vencimento (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(recurring, { recurring = it }); Text("Repetir mensalmente") }
        if (error) Text("Informe nome, valor positivo e data válida.", color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val cents = parseCents(amount)
            if (name.isBlank() || cents == null || cents <= 0 || runCatching { LocalDate.parse(due) }.isFailure) error = true
            else { vm.save(name, category, cents, due, recurring, description); onDone() }
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar despesa") }
    }
}

@Composable
fun UsageScreen(vm: MainViewModel) {
    val readings by vm.readings.collectAsState()
    var type by remember { mutableStateOf("Água") }
    var value by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var bill by remember { mutableStateOf("") }
    val selected = readings.filter { it.type == type }.sortedBy { it.date }
    val latest = selected.lastOrNull()
    val previous = selected.dropLast(1).lastOrNull()
    val used = if (latest != null && previous != null) latest.reading - previous.reading else null
    Page("Controle de consumo") {
        Text("Registre leituras dos medidores. Leituras são dados reais informados por você; projeções são estimativas.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == "Água", { type = "Água" }, label = { Text("Água") })
            FilterChip(type == "Energia", { type = "Energia" }, label = { Text("Energia") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Consumo no intervalo", used?.let { "%.1f".format(Locale("pt","BR"), it) } ?: "—", Modifier.weight(1f))
            Metric("Registros", selected.size.toString(), Modifier.weight(1f))
        }
        OutlinedTextField(value, { value = it }, label = { Text("Leitura atual do medidor") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(date, { date = it }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bill, { bill = it }, label = { Text("Valor da conta em R$ (opcional)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val v = value.replace(",", ".").toDoubleOrNull()
            val cents = parseCents(bill) ?: 0L
            if (v != null && v >= 0 && runCatching { LocalDate.parse(date) }.isSuccess) { vm.addReading(type, v, date, cents); value = ""; bill = "" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Registrar leitura") }
        Text("Histórico", style = MaterialTheme.typography.titleMedium)
        if (selected.isEmpty()) EmptyHint("Sem leituras registradas para este medidor.")
        selected.asReversed().forEach { Text("${it.date}: ${it.reading} · ${money(it.billCents)}") }
    }
}

@Composable
fun ReportsScreen(vm: MainViewModel) {
    val expenses by vm.expenses.collectAsState()
    var period by remember { mutableStateOf("Mês") }
    val today = LocalDate.now()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val filtered = when (period) {
        "Dia" -> expenses.filter { it.dueDate == today.toString() }
        "Semana" -> expenses.filter { runCatching { val d = LocalDate.parse(it.dueDate); !d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6)) }.getOrDefault(false) }
        "Ano" -> expenses.filter { it.dueDate.startsWith(today.year.toString()) }
        "Categoria" -> expenses
        else -> expenses.filter { it.dueDate.startsWith(YearMonth.now().toString()) }
    }
    val groups = filtered.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amountCents } }.toList().sortedByDescending { it.second }
    val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
    val monthly = months.map { m -> m to expenses.filter { it.dueDate.startsWith(m.toString()) }.sumOf { it.amountCents } }
    val max = (monthly.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    Page("Relatórios") {
        Text("Período do resumo", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Dia", "Semana", "Mês", "Ano", "Categoria").forEach { choice ->
                FilterChip(selected = period == choice, onClick = { period = choice }, label = { Text(choice) })
            }
        }
        Text("Total do período: ${money(filtered.sumOf { it.amountCents })}", fontWeight = FontWeight.Bold)
        Text("Evolução dos últimos seis meses", style = MaterialTheme.typography.titleMedium)
        if (expenses.isEmpty()) EmptyHint("Seus gráficos aparecerão quando houver despesas cadastradas.")
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                monthly.forEach { (month, value) ->
                    Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(money(value).replace("R$", ""), style = MaterialTheme.typography.labelSmall)
                        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(horizontal=5.dp)) {
                            val h = size.height * (value.toFloat()/max).coerceIn(0f,1f)
                            drawRoundRect(color = Color(0xFF2E9D68), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height-h), size = androidx.compose.ui.geometry.Size(size.width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f,10f))
                        }
                        Text(month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("pt","BR")), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text("Despesas por categoria", style = MaterialTheme.typography.titleMedium)
        groups.forEach { (category, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(category); Text(money(value)) }
            LinearProgressIndicator(progress = { value.toFloat()/(groups.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L) }, modifier = Modifier.fillMaxWidth())
        }
        Text("Total no período selecionado: ${money(filtered.sumOf { it.amountCents })}", fontWeight = FontWeight.Bold)
        Text("Os relatórios consideram os lançamentos salvos neste aparelho.")
    }
}

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val expenses by vm.expenses.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val monthItems = expenses.filter { it.dueDate.startsWith(month.toString()) }.sortedBy { it.dueDate }
    Page("Calendário de vencimentos") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Mês anterior") }
            Text(month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt","BR")).replaceFirstChar { it.uppercase() } + " ${month.year}", fontWeight = FontWeight.Bold)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Próximo mês") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("S","T","Q","Q","S","S","D").forEach { Text(it, fontWeight = FontWeight.Bold) }
        }
        val offset = (month.atDay(1).dayOfWeek.value - 1)
        val cells = List(offset) { 0 } + (1..month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    val dayItems = if (day == 0) emptyList() else monthItems.filter { it.dueDate.endsWith(day.toString().padStart(2,'0')) }
                    val isToday = day != 0 && month == YearMonth.now() && day == LocalDate.now().dayOfMonth
                    Column(Modifier.weight(1f).padding(2.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(10.dp), color = when { isToday -> MaterialTheme.colorScheme.primaryContainer; dayItems.any { !it.isPaid && it.dueDate < LocalDate.now().toString() } -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.surfaceVariant }) {
                            Text(if (day == 0) "" else day.toString(), Modifier.padding(vertical=7.dp, horizontal=9.dp), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (dayItems.isNotEmpty()) Text("●", color = if (dayItems.all { it.isPaid }) Color(0xFF2E9D68) else MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        Text("Contas do mês", style = MaterialTheme.typography.titleMedium)
        if (monthItems.isEmpty()) EmptyHint("Nenhum vencimento cadastrado neste mês.")
        monthItems.forEach { item ->
            ListItem(
                headlineContent = { Text(item.name) },
                supportingContent = { Text("${item.dueDate} · " + when { item.isPaid -> "Paga"; item.dueDate < LocalDate.now().toString() -> "Vencida"; item.dueDate == LocalDate.now().toString() -> "Vence hoje"; else -> "Pendente" }) },
                trailingContent = { Text(money(item.amountCents)) }
            )
        }
    }
}

private data class BackupPayload(
    val version: Int = 1,
    val expenses: List<Expense>,
    val readings: List<MeterReading>,
    val incomeCents: Long = 0,
    val budgetCents: Long = 0,
    val themeMode: String = "system"
)

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val budget by vm.budget.collectAsState()
    val income by vm.income.collectAsState()
    val theme by vm.themeMode.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val readings by vm.readings.collectAsState()
    var pendingRestore by remember { mutableStateOf<BackupPayload?>(null) }
    var budgetText by remember(budget) { mutableStateOf(if (budget == 0L) "" else "%.2f".format(Locale.US, budget / 100.0)) }
    var incomeText by remember(income) { mutableStateOf(if (income == 0L) "" else "%.2f".format(Locale.US, income / 100.0)) }
    var rent by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var internet by remember { mutableStateOf("") }
    var food by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    var simIncome by remember { mutableStateOf("") }
    var backupText by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val gson = remember { Gson() }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching {
            requireNotNull(context.contentResolver.openOutputStream(uri)) { "Não foi possível abrir o destino." }.bufferedWriter().use { it.write(backupText ?: "") }
            message = "Backup salvo com sucesso."
        }.onFailure { message = "Não foi possível salvar o backup: ${it.localizedMessage}" }
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Arquivo vazio")
            val payload = gson.fromJson(json, BackupPayload::class.java) ?: error("Backup inválido")
            require(payload.version == 1) { "Versão de backup não suportada" }
            require(payload.expenses.all { it.name.isNotBlank() && it.amountCents >= 0 && runCatching { LocalDate.parse(it.dueDate) }.isSuccess }) { "O backup contém despesas inválidas." }
            require(payload.readings.all { it.reading >= 0 && it.billCents >= 0 && runCatching { LocalDate.parse(it.date) }.isSuccess }) { "O backup contém leituras inválidas." }
            pendingRestore = payload
            message = "Backup validado. Confirme a substituição dos dados locais."
        }.onFailure { message = "Falha ao restaurar: ${it.localizedMessage}" }
    }

    Page("Configurações") {
        Text("Aparência", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("system" to "Sistema", "light" to "Claro", "dark" to "Escuro").forEachIndexed { index, pair ->
                SegmentedButton(selected = theme == pair.first, onClick = { vm.setTheme(pair.first) }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(pair.second) }
            }
        }
        Text("Planejamento mensal", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(incomeText, { incomeText = it }, label = { Text("Renda mensal (R$)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(budgetText, { budgetText = it }, label = { Text("Orçamento mensal (R$)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Button(onClick = {
            parseCents(incomeText)?.let(vm::setIncome)
            parseCents(budgetText)?.let(vm::setBudget)
            message = "Planejamento salvo."
        }, modifier = Modifier.fillMaxWidth()) { Text("Salvar planejamento") }

        HorizontalDivider()
        Text("Simulador: morar sozinho", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(simIncome, { simIncome = it }, label = { Text("Renda mensal prevista (R$)") }, modifier = Modifier.fillMaxWidth())
        @Composable fun SimField(label: String, value: String, set: (String) -> Unit) {
            OutlinedTextField(value, set, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        SimField("Aluguel", rent, { rent = it }); SimField("Água", water, { water = it })
        SimField("Energia", power, { power = it }); SimField("Internet", internet, { internet = it })
        SimField("Alimentação", food, { food = it }); SimField("Transporte", transport, { transport = it })
        SimField("Outras despesas", other, { other = it })
        val values = listOf(rent, water, power, internet, food, transport, other).map { parseCents(it) }
        val simulationTotal = if (values.all { it != null }) values.filterNotNull().sum() else 0L
        val simulatedIncome = parseCents(simIncome) ?: 0L
        val remainder = simulatedIncome - simulationTotal
        Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) {
            Text("Custo mensal estimado: ${money(simulationTotal)}", fontWeight = FontWeight.Bold)
            Text("Saldo estimado: ${money(remainder)}", color = if (remainder >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Text(if (values.any { it == null } || simulatedIncome <= 0) "Preencha os campos com valores válidos." else if (remainder >= 0) "A renda cobre as despesas simuladas. Considere reservar parte do saldo para emergências." else "A renda não cobre as despesas simuladas. Revise os valores antes de assumir compromissos.")
        } }

        HorizontalDivider()
        Text("Backup e restauração", style = MaterialTheme.typography.titleMedium)
        Text("O backup contém despesas e leituras salvas neste aparelho. Guarde o arquivo em local seguro.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = {
                val payload = BackupPayload(1, expenses, readings, income, budget, theme)
                backupText = gson.toJson(payload)
                createBackup.launch("conta-sob-controle-backup.json")
            }) { Icon(Icons.Default.FileDownload, null); Text("Exportar") }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { restoreBackup.launch(arrayOf("application/json", "text/json", "*/*")) }) { Icon(Icons.Default.FileUpload, null); Text("Restaurar") }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        Text("Os dados permanecem no armazenamento local do aparelho. A restauração substitui as despesas e leituras atuais.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    pendingRestore?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar backup?") },
            text = { Text("Isso substituirá ${expenses.size} despesas e as leituras atuais pelas ${payload.expenses.size} despesas e ${payload.readings.size} leituras do arquivo. Não é possível desfazer.") },
            confirmButton = { TextButton(onClick = {
                vm.restore(payload.expenses, payload.readings, payload.incomeCents, payload.budgetCents, payload.themeMode)
                pendingRestore = null
                message = "Restauração iniciada com sucesso."
            }) { Text("Substituir dados") } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } }
        )
    }
}
