package br.com.sgsistemas.cafesg.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "funcionarios")
data class FuncionarioEntity(
    @PrimaryKey val codigo: String,
    val nome: String,
    val rfid: String? = null
)
