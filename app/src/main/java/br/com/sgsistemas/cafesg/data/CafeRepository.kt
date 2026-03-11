package br.com.sgsistemas.cafesg.data

import android.util.Log
import br.com.sgsistemas.cafesg.data.local.ConsumoOfflineDao
import br.com.sgsistemas.cafesg.data.local.ConsumoOfflineEntity
import br.com.sgsistemas.cafesg.data.local.FuncionarioDao
import br.com.sgsistemas.cafesg.data.local.FuncionarioEntity
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        try {
            Log.d("CafeRepository", "Iniciando sincronização global de funcionários...")
            val response = api.getFuncionarios()
            val entities = response.map { FuncionarioEntity(it.codigo, it.nome, it.rfid) }
            
            // Usando replaceAll para garantir que quem não veio na API seja removido do banco local
            funcionarioDao.replaceAll(entities)
            
            Log.d("CafeRepository", "Sincronização concluída: ${entities.size} funcionários salvos (antigos removidos).")
        } catch (e: Exception) {
            Log.e("CafeRepository", "Falha na sincronização global: ${e.message}")
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
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun registrarConsumo(codigo: String, valor: Double): ConsumoResponse {
        return try {
            val response = api.registrarConsumo(ConsumoRequest(codigo, valor))
            syncOfflineConsumos()
            response
        } catch (e: Exception) {
            consumoOfflineDao.insert(ConsumoOfflineEntity(codigo = codigo, valor = valor))
            ConsumoResponse("Consumo salvo localmente (modo offline)", -1)
        }
    }

    suspend fun syncOfflineConsumos() {
        val pending = consumoOfflineDao.getAllPending()
        if (pending.isEmpty()) return
        
        pending.forEach { consumo ->
            try {
                api.registrarConsumo(ConsumoRequest(consumo.codigo, consumo.valor))
                consumoOfflineDao.delete(consumo)
            } catch (e: Exception) {
                Log.e("CafeRepository", "Erro ao sincronizar consumo offline")
            }
        }
    }

    suspend fun getRanking() = try {
        api.getRankingMes()
    } catch (e: Exception) {
        emptyList<RankingItem>()
    }
}
