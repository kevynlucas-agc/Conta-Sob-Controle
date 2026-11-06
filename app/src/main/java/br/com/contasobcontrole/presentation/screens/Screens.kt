@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.contasobcontrole.presentation.screens

import androidx.compose.ui.Alignment
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import br.com.contasobcontrole.data.Expense
import br.com.contasobcontrole.data.MeterReading
import br.com.contasobcontrole.presentation.MainViewModel
import br.com.contasobcontrole.domain.FinanceMath
import com.google.gson.Gson
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val MainShape = RoundedCornerShape(26.dp)
private val ItemShape = RoundedCornerShape(18.dp)

private fun money(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        .format(cents / 100.0)

private fun parseCents(value: String): Long? =
    value.trim().replace(".", "").replace(",", ".")
        .toBigDecimalOrNull()?.movePointRight(2)?.toLong()

private fun monthName(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }

@Composable
private fun Page(
    title: String,
    subtitle: String = "Tudo sob controle, do seu jeito.",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    highlighted: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = MainShape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlighted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Card(
        shape = MainShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.Default.Inbox,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------
// DASHBOARD
// ---------------------------------------------------------

@Composable
fun DashboardScreen(vm: MainViewModel, onCalendar: () -> Unit = {}) {
    val expenses by vm.expenses.collectAsState()
    val income by vm.income.collectAsState()
    val budget by vm.budget.collectAsState()

    val month = YearMonth.now().toString()
    val current = expenses.filter { it.dueDate.startsWith(month) }
    val total = current.sumOf { it.amountCents }

    val previousTotal = expenses.filter {
        it.dueDate.startsWith(YearMonth.now().minusMonths(1).toString())
    }.sumOf { it.amountCents }

    val paid = current.filter { it.isPaid }.sumOf { it.amountCents }
    val pending = current.filterNot { it.isPaid }.sumOf { it.amountCents }

    val pct = if (budget > 0)
        FinanceMath.percent(total, budget).toInt().coerceAtLeast(0)
    else 0

    val next = expenses.filter {
        !it.isPaid && it.dueDate >= LocalDate.now().toString()
    }.sortedBy { it.dueDate }.take(4)

    Page(
        "Olá! Suas contas",
        "Acompanhe sua saúde financeira."
    ) {
        Card(
            shape = MainShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SEU RESUMO FINANCEIRO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    monthName(YearMonth.now()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Suas finanças organizadas em um só lugar.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        SectionTitle("Visão geral", "Resumo deste mês")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(
                "Despesas",
                money(total),
                Modifier.weight(1f),
                Icons.Default.Payments
            )
            Metric(
                "Saldo estimado",
                money(income - total),
                Modifier.weight(1f),
                Icons.Default.AccountBalanceWallet,
                true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(
                "Contas pagas",
                money(paid),
                Modifier.weight(1f),
                Icons.Default.CheckCircle
            )
            Metric(
                "Pendentes",
                money(pending),
                Modifier.weight(1f),
                Icons.Default.PendingActions
            )
        }

        Card(shape = MainShape) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Orçamento mensal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.PieChart,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (budget > 0) {
                    Text(
                        "$pct% utilizado",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = {
                            (total.toFloat() / budget).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth().height(9.dp)
                    )
                    Text(
                        "${money(total)} utilizados de ${money(budget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (total > budget) {
                        Text(
                            "Você ultrapassou seu orçamento!",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        "Defina seu orçamento nas configurações para acompanhar seus limites.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val comparison = when {
            previousTotal == 0L ->
                "Sem histórico do mês anterior para comparação."
            total > previousTotal -> {
                val p = FinanceMath.percent(
                    total - previousTotal,
                    previousTotal
                ).toPlainString()
                "Gastos $p% maiores que no mês anterior."
            }
            total < previousTotal -> {
                val p = FinanceMath.percent(
                    previousTotal - total,
                    previousTotal
                ).toPlainString()
                "Gastos $p% menores que no mês anterior."
            }
            else -> "Gastos iguais aos do mês anterior."
        }

        Text(
            comparison,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Próximos vencimentos")
            TextButton(onClick = onCalendar) {
                Text("Calendário")
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        if (next.isEmpty()) {
            EmptyHint("Nenhuma conta pendente cadastrada.")
        }

        next.forEach { item ->
            Card(shape = ItemShape) {
                ListItem(
                    headlineContent = {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("Vencimento: ${item.dueDate}")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Receipt,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Text(
                            money(item.amountCents),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------
// MINHAS CONTAS
// ---------------------------------------------------------

@Composable
fun BillsScreen(
    vm: MainViewModel,
    onAdd: () -> Unit,
    onCalendar: () -> Unit = {}
) {
    val expenses by vm.expenses.collectAsState()
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }

    Page("Minhas contas", "Gerencie despesas e acompanhe pagamentos.") {
        Card(shape = MainShape) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        expenses.size.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Lançamentos cadastrados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ReceiptLong,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCalendar,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CalendarMonth, null)
                Spacer(Modifier.width(5.dp))
                Text("Calendário")
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(5.dp))
                Text("Adicionar")
            }
        }

        if (expenses.isEmpty()) {
            EmptyHint("Você ainda não cadastrou despesas. Adicione sua primeira conta.")
        }

        expenses.sortedBy { it.dueDate }.forEach { item ->
            val status = when {
                item.isPaid -> "Paga"
                item.dueDate < LocalDate.now().toString() -> "Atrasada"
                else -> "Pendente"
            }

            val statusColor = when {
                item.isPaid -> Color(0xFF2E9D68)
                item.dueDate < LocalDate.now().toString() ->
                    MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            }

            Card(shape = MainShape) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                item.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(status, statusColor)
                    }

                    Text(
                        money(item.amountCents),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            item.dueDate,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.weight(1f))

                        IconButton(onClick = { editTarget = item }) {
                            Icon(Icons.Default.Edit, "Editar")
                        }
                        if (!item.isPaid) {
                            IconButton(onClick = { vm.pay(item) }) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "Marcar como paga",
                                    tint = Color(0xFF2E9D68)
                                )
                            }
                        }
                        IconButton(onClick = { deleteTarget = item }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text("Excluir conta?") },
            text = {
                Text("A conta ${target.name} será removida deste aparelho. Esta ação não pode ser desfeita.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target)
                    deleteTarget = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    editTarget?.let { target ->
        var editName by remember(target.id) { mutableStateOf(target.name) }
        var editAmount by remember(target.id) {
            mutableStateOf(
                "%.2f".format(Locale.US, target.amountCents / 100.0)
            )
        }
        var editDue by remember(target.id) { mutableStateOf(target.dueDate) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Editar conta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        editName,
                        { editName = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        editAmount,
                        { editAmount = it },
                        label = { Text("Valor (R$)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                    OutlinedTextField(
                        editDue,
                        { editDue = it },
                        label = { Text("Vencimento (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = parseCents(editAmount)
                    if (
                        editName.isNotBlank() &&
                        cents != null &&
                        cents > 0 &&
                        runCatching { LocalDate.parse(editDue) }.isSuccess
                    ) {
                        vm.update(
                            target.copy(
                                name = editName,
                                amountCents = cents,
                                dueDate = editDue
                            )
                        )
                        editTarget = null
                    }
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ---------------------------------------------------------
// NOVA DESPESA
// ---------------------------------------------------------

@Composable
fun AddExpenseScreen(vm: MainViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Outras despesas") }
    var amount by remember { mutableStateOf("") }
    var due by remember {
        mutableStateOf(LocalDate.now().plusDays(7).toString())
    }
    var description by remember { mutableStateOf("") }
    var recurring by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Água", "Energia elétrica", "Internet", "Aluguel",
        "Gás", "Telefone", "Condomínio", "Supermercado",
        "Transporte", "Outras despesas"
    )

    Page("Nova despesa", "Cadastre uma conta e organize seus vencimentos.") {
        Card(shape = MainShape) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                SectionTitle("Informações da conta")

                OutlinedTextField(
                    name,
                    { name = it; error = false },
                    label = { Text("Nome da conta") },
                    leadingIcon = { Icon(Icons.Default.Receipt, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded,
                    { expanded = it }
                ) {
                    OutlinedTextField(
                        category,
                        {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        leadingIcon = {
                            Icon(Icons.Default.Category, null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded,
                        { expanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    amount,
                    { amount = it; error = false },
                    label = { Text("Valor em R$") },
                    placeholder = { Text("Ex.: 125,90") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    due,
                    { due = it; error = false },
                    label = { Text("Vencimento (AAAA-MM-DD)") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Descrição (opcional)") },
                    leadingIcon = { Icon(Icons.Default.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = recurring,
                        onCheckedChange = { recurring = it }
                    )
                    Column {
                        Text(
                            "Repetir mensalmente",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Despesa recorrente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (error) {
                    Text(
                        "Informe nome, valor positivo e uma data válida.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val cents = parseCents(amount)
                        if (
                            name.isBlank() ||
                            cents == null ||
                            cents <= 0 ||
                            runCatching { LocalDate.parse(due) }.isFailure
                        ) {
                            error = true
                        } else {
                            vm.save(
                                name, category, cents, due,
                                recurring, description
                            )
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar despesa")
                }
            }
        }
    }
}

// ---------------------------------------------------------
// CONTROLE DE CONSUMO
// ---------------------------------------------------------

@Composable
fun UsageScreen(vm: MainViewModel) {
    val readings by vm.readings.collectAsState()

    var type by remember { mutableStateOf("Água") }
    var value by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var bill by remember { mutableStateOf("") }

    val selected = readings
        .filter { it.type == type }
        .sortedBy { it.date }

    val latest = selected.lastOrNull()
    val previous = selected.dropLast(1).lastOrNull()

    val used = if (latest != null && previous != null)
        latest.reading - previous.reading
    else null

    Page("Controle de consumo", "Acompanhe água e energia ao longo do tempo.") {
        Card(shape = MainShape) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTitle("Tipo de medidor")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "Água",
                        onClick = { type = "Água" },
                        label = { Text("Água") },
                        leadingIcon = {
                            Icon(Icons.Default.WaterDrop, null)
                        }
                    )
                    FilterChip(
                        selected = type == "Energia",
                        onClick = { type = "Energia" },
                        label = { Text("Energia") },
                        leadingIcon = {
                            Icon(Icons.Default.Bolt, null)
                        }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(
                "Consumo no intervalo",
                used?.let {
                    "%.1f".format(Locale("pt", "BR"), it)
                } ?: "—",
                Modifier.weight(1f),
                Icons.Default.ShowChart
            )
            Metric(
                "Registros",
                selected.size.toString(),
                Modifier.weight(1f),
                Icons.Default.History
            )
        }

        Card(shape = MainShape) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTitle("Registrar leitura")

                OutlinedTextField(
                    value,
                    { value = it },
                    label = { Text("Leitura atual do medidor") },
                    leadingIcon = { Icon(Icons.Default.Speed, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )

                OutlinedTextField(
                    date,
                    { date = it },
                    label = { Text("Data (AAAA-MM-DD)") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    bill,
                    { bill = it },
                    label = { Text("Valor da conta (R$), opcional") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )

                Button(
                    onClick = {
                        val v = value.replace(",", ".").toDoubleOrNull()
                        val cents = if (bill.isBlank()) 0L
                            else parseCents(bill)

                        if (
                            v != null &&
                            v >= 0 &&
                            cents != null &&
                            cents >= 0 &&
                            runCatching { LocalDate.parse(date) }.isSuccess &&
                            (latest == null || v >= latest.reading)
                        ) {
                            vm.addReading(type, v, date, cents)
                            value = ""
                            bill = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Registrar leitura")
                }
            }
        }

        SectionTitle("Histórico", "${selected.size} registros")

        if (selected.isEmpty()) {
            EmptyHint("Nenhuma leitura registrada para este medidor.")
        }

        selected.asReversed().forEach { item ->
            Card(shape = ItemShape) {
                ListItem(
                    headlineContent = {
                        Text(
                            "${item.reading}",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = { Text(item.date) },
                    leadingContent = {
                        Icon(
                            if (type == "Água")
                                Icons.Default.WaterDrop
                            else Icons.Default.Bolt,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Text(
                            money(item.billCents),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }

        Text(
            "As leituras são informadas por você. O consumo é calculado pela diferença entre registros consecutivos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------
// RELATÓRIOS
// ---------------------------------------------------------

@Composable
fun ReportsScreen(vm: MainViewModel) {
    val expenses by vm.expenses.collectAsState()
    var period by remember { mutableStateOf("Mês") }

    val today = LocalDate.now()
    val weekStart = today.minusDays(
        (today.dayOfWeek.value - 1).toLong()
    )

    val filtered = when (period) {
        "Dia" -> expenses.filter {
            it.dueDate == today.toString()
        }

        "Semana" -> expenses.filter {
            runCatching {
                val d = LocalDate.parse(it.dueDate)
                !d.isBefore(weekStart) &&
                    !d.isAfter(weekStart.plusDays(6))
            }.getOrDefault(false)
        }

        "Ano" -> expenses.filter {
            it.dueDate.startsWith(today.year.toString())
        }

        "Categoria" -> expenses

        else -> expenses.filter {
            it.dueDate.startsWith(YearMonth.now().toString())
        }
    }

    val groups = filtered
        .groupBy { it.category }
        .mapValues { entry ->
            entry.value.sumOf { it.amountCents }
        }
        .toList()
        .sortedByDescending { it.second }

    val months = (5 downTo 0).map {
        YearMonth.now().minusMonths(it.toLong())
    }

    val monthly = months.map { month ->
        month to expenses.filter {
            it.dueDate.startsWith(month.toString())
        }.sumOf { it.amountCents }
    }

    val max = (monthly.maxOfOrNull { it.second } ?: 1L)
        .coerceAtLeast(1L)

    Page("Relatórios", "Entenda para onde seu dinheiro está indo.") {
        Card(shape = MainShape) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTitle("Período do resumo")

                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("Dia", "Semana", "Mês", "Ano", "Categoria")
                        .forEach { choice ->
                            FilterChip(
                                selected = period == choice,
                                onClick = { period = choice },
                                label = { Text(choice) }
                            )
                        }
                }

                Text(
                    money(filtered.sumOf { it.amountCents }),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Total do período selecionado",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SectionTitle(
            "Evolução financeira",
            "Despesas dos últimos seis meses"
        )

        if (expenses.isEmpty()) {
            EmptyHint("Seus gráficos aparecerão quando houver despesas.")
        } else {
            Card(shape = MainShape) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    monthly.forEach { (month, amount) ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    month.month.getDisplayName(
                                        TextStyle.SHORT,
                                        Locale("pt", "BR")
                                    ),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    money(amount),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            LinearProgressIndicator(
                                progress = {
                                    (amount.toFloat() / max)
                                        .coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth().height(9.dp)
                            )
                        }
                    }
                }
            }
        }

        SectionTitle("Despesas por categoria")

        if (groups.isEmpty()) {
            EmptyHint("Não existem despesas neste período.")
        }

        groups.forEach { (category, amount) ->
            val maximum = (groups.maxOfOrNull { it.second } ?: 1L)
                .coerceAtLeast(1L)

            Card(shape = ItemShape) {
                Column(
                    Modifier.fillMaxWidth().padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category, fontWeight = FontWeight.SemiBold)
                        Text(
                            money(amount),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            (amount.toFloat() / maximum).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth().height(7.dp)
                    )
                }
            }
        }

        Text(
            "Os relatórios consideram os lançamentos salvos neste aparelho.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------
// CALENDÁRIO
// ---------------------------------------------------------

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val expenses by vm.expenses.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }

    val monthItems = expenses
        .filter { it.dueDate.startsWith(month.toString()) }
        .sortedBy { it.dueDate }

    Page(
        "Calendário",
        "Visualize os vencimentos e organize seus pagamentos."
    ) {
        Card(shape = MainShape) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { month = month.minusMonths(1) }
                    ) {
                        Icon(Icons.Default.ChevronLeft, "Mês anterior")
                    }

                    Text(
                        "${monthName(month)} ${month.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { month = month.plusMonths(1) }
                    ) {
                        Icon(Icons.Default.ChevronRight, "Próximo mês")
                    }
                }

                Row(Modifier.fillMaxWidth()) {
                    listOf("S", "T", "Q", "Q", "S", "S", "D")
                        .forEach { day ->
                            Text(
                                day,
                                Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                }

                val offset = month.atDay(1).dayOfWeek.value - 1
                val cells = List(offset) { 0 } +
                    (1..month.lengthOfMonth()).toList()

                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val dayItems = if (day == 0) emptyList()
                            else monthItems.filter {
                                it.dueDate.endsWith(
                                    day.toString().padStart(2, '0')
                                )
                            }

                            val isToday =
                                day != 0 &&
                                month == YearMonth.now() &&
                                day == LocalDate.now().dayOfMonth

                            val overdue = dayItems.any {
                                !it.isPaid &&
                                    it.dueDate < LocalDate.now().toString()
                            }

                            Column(
                                Modifier.weight(1f).padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = when {
                                        isToday ->
                                            MaterialTheme.colorScheme.primary
                                        overdue ->
                                            MaterialTheme.colorScheme.errorContainer
                                        dayItems.isNotEmpty() ->
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else ->
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (day == 0) "" else day.toString(),
                                            fontWeight = if (isToday)
                                                FontWeight.Bold
                                            else FontWeight.Normal,
                                            color = if (isToday)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (dayItems.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Circle,
                                        null,
                                        modifier = Modifier.size(6.dp),
                                        tint = if (dayItems.all { it.isPaid })
                                            Color(0xFF2E9D68)
                                        else MaterialTheme.colorScheme.tertiary
                                    )
                                } else {
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }

                        repeat(7 - week.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        SectionTitle(
            "Contas do mês",
            "${monthItems.size} vencimentos encontrados"
        )

        if (monthItems.isEmpty()) {
            EmptyHint("Nenhum vencimento cadastrado neste mês.")
        }

        monthItems.forEach { item ->
            val status = when {
                item.isPaid -> "Paga"
                item.dueDate < LocalDate.now().toString() -> "Vencida"
                item.dueDate == LocalDate.now().toString() -> "Vence hoje"
                else -> "Pendente"
            }

            Card(shape = ItemShape) {
                ListItem(
                    headlineContent = {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("${item.dueDate} · $status")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Event,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Text(
                            money(item.amountCents),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}

