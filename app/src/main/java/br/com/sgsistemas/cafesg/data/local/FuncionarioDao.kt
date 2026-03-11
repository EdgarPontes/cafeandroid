package br.com.sgsistemas.cafesg.data.local

import androidx.room.*

@Dao
interface FuncionarioDao {
    @Query("SELECT * FROM funcionarios")
    suspend fun getAll(): List<FuncionarioEntity>

    @Query("SELECT * FROM funcionarios WHERE codigo = :codigo LIMIT 1")
    suspend fun getByCodigo(codigo: String): FuncionarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(funcionarios: List<FuncionarioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(funcionario: FuncionarioEntity)

    @Transaction
    suspend fun replaceAll(funcionarios: List<FuncionarioEntity>) {
        clearAll()
        insertAll(funcionarios)
    }

    @Query("DELETE FROM funcionarios")
    suspend fun clearAll()
}
