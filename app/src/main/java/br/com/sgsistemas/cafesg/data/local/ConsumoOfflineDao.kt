package br.com.sgsistemas.cafesg.data.local

import androidx.room.*

@Dao
interface ConsumoOfflineDao {
    @Query("SELECT * FROM consumos_offline ORDER BY timestamp ASC")
    suspend fun getAllPending(): List<ConsumoOfflineEntity>

    @Insert
    suspend fun insert(consumo: ConsumoOfflineEntity)

    @Delete
    suspend fun delete(consumo: ConsumoOfflineEntity)

    @Query("DELETE FROM consumos_offline")
    suspend fun deleteAll()
}
