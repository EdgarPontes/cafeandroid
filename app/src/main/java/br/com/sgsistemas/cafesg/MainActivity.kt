package br.com.sgsistemas.cafesg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.sgsistemas.cafesg.data.CafeRepository
import br.com.sgsistemas.cafesg.data.SettingsManager
import br.com.sgsistemas.cafesg.ui.CafeSGApp
import br.com.sgsistemas.cafesg.ui.MainViewModel
import br.com.sgsistemas.cafesg.ui.theme.CafeSGTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsManager: SettingsManager

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = CafeRepository(settingsManager.getBaseUrl())
                return MainViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        enableEdgeToEdge()
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
                            viewModel.updateRepository(CafeRepository(settingsManager.getBaseUrl()))
                        }
                    )
                }
            }
        }
    }
}
