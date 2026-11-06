package br.com.contasobcontrole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.contasobcontrole.presentation.AppRoot
import br.com.contasobcontrole.presentation.AppTheme
import br.com.contasobcontrole.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 501)
        }
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val mode by vm.themeMode.collectAsState()
            AppTheme(mode) { AppRoot(vm) }
        }
    }
}
