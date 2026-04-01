package br.com.sgsistemas.cafesg.data

import android.util.Log
import br.com.sgsistemas.cafesg.data.local.ConsumoOfflineDao
import br.com.sgsistemas.cafesg.data.local.ConsumoOfflineEntity
import br.com.sgsistemas.cafesg.data.local.FuncionarioDao
import br.com.sgsistemas.cafesg.data.local.FuncionarioEntity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import br.com.sgsistemas.cafesg.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CafeRepository(
    baseUrl: String,
    private val funcionarioDao: FuncionarioDao,
    private val consumoOfflineDao: ConsumoOfflineDao
) {
    private var api: CafeApi = createApi(baseUrl)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val syncMutex = Mutex()

    private fun createApi(baseUrl: String): CafeApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(BuildConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(BuildConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(BuildConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CafeApi::class.java)
    }

    fun updateBaseUrl(newBaseUrl: String) {
        this.api = createApi(newBaseUrl)
    }

    suspend fun getFuncionario(codigo: String): Funcionario {
        return try {
            api.getFuncionario(codigo)
        } catch (e: Exception) {
            funcionarioDao.getByCodigo(codigo)?.let {
                Funcionario(it.codigo, it.nome, it.rfid)
            } ?: throw e
        }
    }

    suspend fun syncFuncionarios() {
        Log.d("CafeRepository", "Iniciando sincronização global de funcionários...")
        try {
            val response = api.getFuncionarios()
            val entities = response.map { FuncionarioEntity(it.codigo, it.nome, it.rfid) }
            
            // Usando replaceAll para garantir que quem não veio na API seja removido do banco local
            funcionarioDao.replaceAll(entities)
            
            Log.d("CafeRepository", "Sincronização concluída: ${entities.size} funcionários salvos (antigos removidos).")
        } catch (e: IOException) {
            Log.e("CafeRepository", "Falha na sincronização global (rede): ${e.message}. Usando dados locais.")
        } catch (e: Exception) {
            Log.e("CafeRepository", "Falha na sincronização global (outros erros): ${e.message}")
        }
    }

    suspend fun getFuncionarios(): List<Funcionario> {
        val cached = funcionarioDao.getAll()
        return if (cached.isNotEmpty()) {
            cached.map { Funcionario(it.codigo, it.nome, it.rfid) }
        } else {
            try {
                val response = api.getFuncionarios()
                funcionarioDao.insertAll(response.map { FuncionarioEntity(it.codigo, it.nome, it.rfid) })
                response
            } catch (e: IOException) {
                Log.e("CafeRepository", "Falha ao buscar funcionários da API (rede): ${e.message}. Retornando lista vazia.")
                emptyList()
            } catch (e: Exception) {
                Log.e("CafeRepository", "Falha ao buscar funcionários da API (outros erros): ${e.message}. Retornando lista vazia.")
                emptyList()
            }
        }
    }

    suspend fun registrarConsumo(codigo: String, nome: String, valor: Double, fotoBase64: String? = null): ConsumoResponse {
        val now = dateFormat.format(Date())
        return try {
            val response = api.registrarConsumo(ConsumoRequest(codigo, nome, valor, now))
            
            // Se o consumo foi registrado com sucesso na API e temos uma foto, tenta enviar agora
            if (response.id != -1 && fotoBase64 != null) {
                try {
                    api.enviarFoto(FotoRequest(response.id, codigo, fotoBase64))
                } catch (e: Exception) {
                    Log.e("CafeRepository", "Erro ao enviar foto após registro: ${e.message}. Salvando para envio posterior.")
                    // Se o consumo foi registrado mas a foto falhou, salva apenas para enviar a foto depois
                    consumoOfflineDao.insert(ConsumoOfflineEntity(
                        codigo = codigo,
                        nome = nome,
                        valor = valor,
                        fotoBase64 = fotoBase64,
                        serverId = response.id,
                        syncConsumoPending = false, // Consumo já está no servidor
                        syncFotoPending = true
                    ))
                    return response.copy(message = "Consumo registrado, foto será enviada em breve.")
                }
            }
            
            syncOfflineConsumos()
            response
        } catch (e: IOException) {
            Log.d("CafeRepository", "Erro de rede ao registrar consumo na API, salvando offline: ${e.message}")
            consumoOfflineDao.insert(ConsumoOfflineEntity(codigo = codigo, nome = nome, valor = valor, fotoBase64 = fotoBase64))
            ConsumoResponse("Consumo salvo localmente (modo offline)", -1, isOffline = true)
        } catch (e: Exception) {
            Log.e("CafeRepository", "Erro de negócio ao registrar consumo: ${e.message}")
            throw e
        }
    }

    suspend fun syncOfflineConsumos() {
        if (syncMutex.isLocked) return
        
        syncMutex.withLock {
            val pending = consumoOfflineDao.getAllPending()
            if (pending.isEmpty()) return
            
            Log.d("CafeRepository", "Iniciando sincronização de ${pending.size} consumos offline...")
            
            pending.forEach { consumo ->
                try {
                    var currentServerId = consumo.serverId
                    
                    // 1. Sincroniza o consumo se pendente
                    if (consumo.syncConsumoPending) {
                        val dataHora = dateFormat.format(Date(consumo.timestamp))
                        val response = api.registrarConsumo(ConsumoRequest(consumo.codigo, consumo.nome, consumo.valor, dataHora))
                        if (response.id != -1) {
                            currentServerId = response.id
                        } else {
                            throw Exception("Erro retornado pela API no sync offline")
                        }
                    }

                    // 2. Sincroniza a foto se pendente e tiver serverId
                    if (consumo.syncFotoPending && currentServerId != null && consumo.fotoBase64 != null) {
                        api.enviarFoto(FotoRequest(currentServerId, consumo.codigo, consumo.fotoBase64))
                    }
                    
                    // Se chegou aqui sem Exception, ambos foram sincronizados (ou o que era necessário)
                    consumoOfflineDao.delete(consumo)
                } catch (e: IOException) {
                    Log.e("CafeRepository", "Falha de conexão no sync offline: ${e.message}. Mantendo registro.")
                    return@withLock // Para o sync atual, tenta novamente depois
                } catch (e: Exception) {
                    Log.e("CafeRepository", "Erro irrecuperável no sync offline para registro ${consumo.id}: ${e.message}")
                    // Se for erro de negócio no sync, mantemos para segurança
                }
            }
        }
    }

    suspend fun getRanking(): List<RankingItem> {
        return try {
            api.getRankingMes()
        } catch (e: IOException) {
            Log.e("CafeRepository", "Falha ao buscar ranking da API (rede): ${e.message}. Retornando lista vazia.")
            emptyList()
        } catch (e: Exception) {
            Log.e("CafeRepository", "Falha ao buscar ranking da API (outros erros): ${e.message}. Retornando lista vazia.")
            emptyList()
        }
    }

    suspend fun enviarFoto(consumoId: Int, codigo: String, fotoBase64: String): FotoResponse {
        return api.enviarFoto(FotoRequest(consumoId, codigo, fotoBase64))
    }
}
