package br.com.sgsistemas.cafesg.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumos_offline")
data class ConsumoOfflineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val codigo: String,
    val nome: String,
    val valor: Double,
    val fotoBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val serverId: Int? = null,
    val syncConsumoPending: Boolean = true,
    val syncFotoPending: Boolean = true
)
