package br.com.sgsistemas.cafesg.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CafeApi {
    @GET("api/funcionarios/{codigo}")
    suspend fun getFuncionario(@Path("codigo") codigo: String): Funcionario

    @GET("api/funcionarios")
    suspend fun getFuncionarios(): List<Funcionario>

    @POST("api/consumo")
    suspend fun registrarConsumo(@Body request: ConsumoRequest): ConsumoResponse

    @GET("api/ranking/hoje")
    suspend fun getRankingHoje(): List<RankingItem>

    @GET("api/ranking/semana")
    suspend fun getRankingSemana(): List<RankingItem>

    @GET("api/ranking/mes")
    suspend fun getRankingMes(): List<RankingItem>

    @POST("api/fotos")
    suspend fun sendPhoto(@Body request: FotoRequest): FotoResponse
}
