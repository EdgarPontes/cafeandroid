package br.com.sgsistemas.cafesg

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.com.sgsistemas.cafesg.data.CafeRepository
import br.com.sgsistemas.cafesg.data.SettingsManager
import br.com.sgsistemas.cafesg.data.local.AppDatabase
import br.com.sgsistemas.cafesg.ui.CafeSGApp
import br.com.sgsistemas.cafesg.ui.MainViewModel
import br.com.sgsistemas.cafesg.ui.theme.CafeSGTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsManager: SettingsManager
    private lateinit var database: AppDatabase
    private lateinit var repository: CafeRepository

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                repository = CafeRepository(
                    settingsManager.getBaseUrl(),
                    database.funcionarioDao(),
                    database.consumoOfflineDao()
                )
                return MainViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        database = AppDatabase.getDatabase(this)
        
        enableEdgeToEdge()
        hideSystemBars()
        
        // Try to sync pending consumos when app starts
        lifecycleScope.launch {
            if (::repository.isInitialized) {
                repository.syncOfflineConsumos()
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            CafeSGTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CafeSGApp(
                        viewModel = viewModel,
                        currentIp = settingsManager.getBaseUrl(),
                        onIpChanged = { newIp ->
                            settingsManager.setBaseUrl(newIp)
                            val newRepo = CafeRepository(
                                settingsManager.getBaseUrl(),
                                database.funcionarioDao(),
                                database.consumoOfflineDao()
                            )
                            viewModel.updateRepository(newRepo)
                        }
                    )
                }
            }
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
