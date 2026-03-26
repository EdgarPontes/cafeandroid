package br.com.sgsistemas.cafesg.data

import android.util.Log
import br.com.sgsistemas.cafesg.data.local.ConsumoOfflineDao
import br.com.sgsistemas.cafesg.data.local.ConsumoOfflineEntity
import br.com.sgsistemas.cafesg.data.local.FuncionarioDao
import br.com.sgsistemas.cafesg.data.local.FuncionarioEntity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class CafeRepository(
    baseUrl: String,
    private val funcionarioDao: FuncionarioDao,
    private val consumoOfflineDao: ConsumoOfflineDao
) {
    private var api: CafeApi = createApi(baseUrl)

    private fun createApi(baseUrl: String): CafeApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
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

    suspend fun registrarConsumo(codigo: String, nome: String, valor: Double): ConsumoResponse {
        return try {
            val response = api.registrarConsumo(ConsumoRequest(codigo, nome, valor))
            syncOfflineConsumos()
            response
        } catch (e: Exception) {
            consumoOfflineDao.insert(ConsumoOfflineEntity(codigo = codigo, nome = nome, valor = valor))
            ConsumoResponse("Consumo salvo localmente (modo offline)", -1)
        }
    }

    suspend fun sendPhoto(codigo: String, base64Image: String): FotoResponse {
        return try {
            api.sendPhoto(FotoRequest(codigo = codigo, imagem = base64Image))
        } catch (e: Exception) {
            Log.e("CafeRepository", "Erro ao enviar foto: ${e.message}")
            FotoResponse("Erro ao enviar foto: ${e.message}") // Retorna um erro na resposta da foto
        }
    }

    suspend fun syncOfflineConsumos() {
        val pending = consumoOfflineDao.getAllPending()
        if (pending.isEmpty()) return
        
        pending.forEach { consumo ->
            try {
                api.registrarConsumo(ConsumoRequest(consumo.codigo, consumo.nome, consumo.valor))
                // TODO: Adicionar lógica para sincronizar fotos offline se implementado
                consumoOfflineDao.delete(consumo)
            } catch (e: Exception) {
                Log.e("CafeRepository", "Erro ao sincronizar consumo offline")
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
}
