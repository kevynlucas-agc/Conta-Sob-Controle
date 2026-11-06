package br.com.contasobcontrole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.contasobcontrole.presentation.screens.AddExpenseScreen
import br.com.contasobcontrole.presentation.screens.BillsScreen
import br.com.contasobcontrole.presentation.screens.CalendarScreen
import br.com.contasobcontrole.presentation.screens.DashboardScreen
import br.com.contasobcontrole.presentation.screens.ReportsScreen
import br.com.contasobcontrole.presentation.screens.SettingsScreen
import br.com.contasobcontrole.presentation.screens.UsageScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
        ContaSobControleApp()
    }
}

}

@Composable
private fun ContaSobControleApp() {

var currentScreen by rememberSaveable {
    mutableStateOf("home")
}

BackHandler(enabled = currentScreen != "home") {
    currentScreen = when (currentScreen) {
        "add" -> "bills"
        "calendar" -> "bills"
        else -> "home"
    }
}

val showNavigationBar = currentScreen in listOf(
    "home",
    "bills",
    "usage",
    "reports",
    "settings"
)

Scaffold(
    containerColor = MaterialTheme.colorScheme.background,

    bottomBar = {
        if (showNavigationBar) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    tonalElevation = 0.dp
                ) {

                    NavigationBarItem(
                        selected = currentScreen == "home",
                        onClick = { currentScreen = "home" },
                        icon = {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Início"
                            )
                        },
                        label = { Text("Início") }
                    )

                    NavigationBarItem(
                        selected = currentScreen == "bills",
                        onClick = { currentScreen = "bills" },
                        icon = {
                            Icon(
                                Icons.Filled.ReceiptLong,
                                contentDescription = "Contas"
                            )
                        },
                        label = { Text("Contas") }
                    )

                    NavigationBarItem(
                        selected = currentScreen == "usage",
                        onClick = { currentScreen = "usage" },
                        icon = {
                            Icon(
                                Icons.Filled.WaterDrop,
                                contentDescription = "Consumo"
                            )
                        },
                        label = { Text("Consumo") }
                    )

                    NavigationBarItem(
                        selected = currentScreen == "reports",
                        onClick = { currentScreen = "reports" },
                        icon = {
                            Icon(
                                Icons.Filled.BarChart,
                                contentDescription = "Relatórios"
                            )
                        },
                        label = { Text("Relatórios") }
                    )

                    NavigationBarItem(
                        selected = currentScreen == "settings",
                        onClick = { currentScreen = "settings" },
                        icon = {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Configurações"
                            )
                        },
                        label = { Text("Ajustes") }
                    )
                }
            }
        }
    }
) { innerPadding ->

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {

        when (currentScreen) {

            "home" -> {
                DashboardScreen(
                    vm = hiltViewModel(),
                    onCalendar = {
                        currentScreen = "calendar"
                    }
                )
            }

            "bills" -> {
                BillsScreen(
                    vm = hiltViewModel(),
                    onAdd = {
                        currentScreen = "add"
                    },
                    onCalendar = {
                        currentScreen = "calendar"
                    }
                )
            }

            "add" -> {
                AddExpenseScreen(
                    vm = hiltViewModel(),
                    onDone = {
                        currentScreen = "bills"
                    }
                )
            }

            "usage" -> {
                UsageScreen(
                    vm = hiltViewModel()
                )
            }

            "reports" -> {
                ReportsScreen(
                    vm = hiltViewModel()
                )
            }

            "calendar" -> {
                CalendarScreen(
                    vm = hiltViewModel()
                )
            }

            "settings" -> {
                SettingsScreen(
                    vm = hiltViewModel()
                )
            }

            else -> {
                DashboardScreen(
                    vm = hiltViewModel(),
                    onCalendar = {
                        currentScreen = "calendar"
                    }
                )
            }
        }
    }
}

}
