package br.com.sgsistemas.cafesg.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CafeRepository(baseUrl: String) {
    private val api: CafeApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CafeApi::class.java)

    suspend fun getFuncionario(codigo: String) = api.getFuncionario(codigo)
    suspend fun getFuncionarios() = api.getFuncionarios()
    suspend fun registrarConsumo(codigo: String, valor: Double) = api.registrarConsumo(ConsumoRequest(codigo, valor))
    
    // Updated to use getRankingMes as per common use, or you can switch to getRankingHoje()
    suspend fun getRanking() = api.getRankingMes() 
}
