package br.com.sgsistemas.cafesg.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.sgsistemas.cafesg.data.CafeRepository
import br.com.sgsistemas.cafesg.data.Funcionario
import br.com.sgsistemas.cafesg.data.RankingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class MainViewModel(private var repository: CafeRepository) : ViewModel() {

    private val _funcionarios = MutableStateFlow<List<Funcionario>>(emptyList())
    val funcionarios: StateFlow<List<Funcionario>> = _funcionarios.asStateFlow()

    private val _ranking = MutableStateFlow<List<RankingItem>>(emptyList())
    val ranking: StateFlow<List<RankingItem>> = _ranking.asStateFlow()

    private val _selectedFuncionario = MutableStateFlow<Funcionario?>(null)
    val selectedFuncionario: StateFlow<Funcionario?> = _selectedFuncionario.asStateFlow()

    private val _consumoStatus = MutableStateFlow<UiState<String>>(UiState.Idle)
    val consumoStatus: StateFlow<UiState<String>> = _consumoStatus.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            // First sync with API to update the local DB
            repository.syncFuncionarios()
            repository.syncOfflineConsumos()
            
            // Then load the updated local data for the UI
            _funcionarios.value = repository.getFuncionarios()
            _ranking.value = repository.getRanking()
        }
    }

    fun updateRepository(newRepository: CafeRepository) {
        this.repository = newRepository
        loadInitialData()
    }

    fun selectFuncionario(funcionario: Funcionario?) {
        _selectedFuncionario.value = funcionario
        if (funcionario == null) {
            _consumoStatus.value = UiState.Idle
        }
    }

    fun registrarConsumo(valor: Double) {
        val funcionario = _selectedFuncionario.value ?: return
        viewModelScope.launch {
            _consumoStatus.value = UiState.Loading
            try {
                val response = repository.registrarConsumo(funcionario.codigo, valor)
                _consumoStatus.value = UiState.Success(response.message)
                _ranking.value = repository.getRanking()
            } catch (e: Exception) {
                _consumoStatus.value = UiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
