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
import retrofit2.HttpException

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

sealed class InitialDataUiState {
    object Loading : InitialDataUiState()
    object Success : InitialDataUiState()
    data class AuthError(val message: String) : InitialDataUiState()
    data class OtherError(val message: String) : InitialDataUiState()
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

    private val _initialDataStatus = MutableStateFlow<InitialDataUiState>(InitialDataUiState.Loading)
    val initialDataStatus: StateFlow<InitialDataUiState> = _initialDataStatus.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _initialDataStatus.value = InitialDataUiState.Loading
            try {
                // First sync with API to update the local DB
                repository.syncFuncionarios()
                repository.syncOfflineConsumos()
                
                // Then load the updated local data for the UI
                _funcionarios.value = repository.getFuncionarios()
                _ranking.value = repository.getRanking()
                _initialDataStatus.value = InitialDataUiState.Success
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    _initialDataStatus.value = InitialDataUiState.AuthError("Dispositivo não autorizado a acessar a API!")
                } else {
                    _initialDataStatus.value = InitialDataUiState.OtherError(e.message() ?: "Erro desconhecido de HTTP ao carregar dados iniciais.")
                }
            } catch (e: Exception) {
                _initialDataStatus.value = InitialDataUiState.OtherError(e.message ?: "Erro desconhecido ao carregar dados iniciais.")
            }
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

    fun registrarConsumo(valor: Double, base64Image: String?) {
        val funcionario = _selectedFuncionario.value ?: return
        viewModelScope.launch {
            _consumoStatus.value = UiState.Loading
            try {
                val response = repository.registrarConsumo(funcionario.codigo, funcionario.nome, valor)

                base64Image?.let { image ->
                    // Envia a foto após registrar o consumo
                    val photoStatus = sendPhoto(funcionario.codigo, image)
                    // Você pode lidar com o status da foto separadamente se necessário
                    // Por exemplo: se photoStatus is UiState.Error, mostrar um toast
                }
                
                _consumoStatus.value = UiState.Success(response.message)
                _ranking.value = repository.getRanking()
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    _consumoStatus.value = UiState.Error("Dispositivo não autorizado a registrar o consumo!")
                } else {
                    _consumoStatus.value = UiState.Error(e.message() ?: "Erro desconhecido de HTTP")
                }
            } catch (e: Exception) {
                _consumoStatus.value = UiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
    
    private suspend fun sendPhoto(codigo: String, base64Image: String): UiState<String> {
        return try {
            val response = repository.sendPhoto(codigo, base64Image)
            UiState.Success(response.message)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Erro ao enviar foto")
        }
    }
}
