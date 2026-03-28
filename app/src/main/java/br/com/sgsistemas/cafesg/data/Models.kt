package br.com.sgsistemas.cafesg.data

import com.google.gson.annotations.SerializedName

data class Funcionario(
    val codigo: String,
    val nome: String,
    val rfid: String? = null
) {
    fun displayCodigo(): String {
        return if (codigo.contains(":")) codigo.split(":")[1] else codigo
    }
}

data class ConsumoRequest(
    val codigo: String,
    val nome: String,
    val valor: Double
)

data class ConsumoResponse(
    val message: String,
    val id: Int
)

data class RankingItem(
    @SerializedName("NOME") val nome: String,
    @SerializedName("TOTAL") val total: Double
)
