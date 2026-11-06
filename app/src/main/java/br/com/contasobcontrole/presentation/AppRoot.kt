package br.com.contasobcontrole.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import br.com.contasobcontrole.presentation.screens.*

@Composable
fun AppRoot(vm: MainViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val items = listOf("Início" to "home", "Contas" to "bills", "Consumo" to "usage", "Relatórios" to "reports", "Config." to "settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.ReceiptLong, Icons.Default.WaterDrop, Icons.Default.BarChart, Icons.Default.Settings)
    val current by nav.currentBackStackEntryAsState()
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { nav.navigate("add") }) { Icon(Icons.Default.Add, "Nova despesa") } },
        bottomBar = {
            NavigationBar { items.forEachIndexed { i, item ->
                NavigationBarItem(selected = current?.destination?.route == item.second,
                    onClick = { nav.navigate(item.second) { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } },
                    icon = { Icon(icons[i], null) }, label = { Text(item.first) })
            } }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { DashboardScreen(vm, onCalendar = { nav.navigate("calendar") }) }
            composable("bills") { BillsScreen(vm, onAdd = { nav.navigate("add") }, onCalendar = { nav.navigate("calendar") }) }
            composable("usage") { UsageScreen(vm) }
            composable("reports") { ReportsScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
            composable("calendar") { CalendarScreen(vm) }
            composable("add") { AddExpenseScreen(vm, onDone = { nav.popBackStack() }) }
        }
    }
}
