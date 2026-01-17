package com.example.projectahirpam.data.dao

import androidx.room.*
import com.example.projectahirpam.data.entity.StokKeluarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StokKeluarDao {

    @Query("SELECT * FROM stok_keluar WHERE userId = :userId ORDER BY id DESC")
    fun getAll(userId: Int): Flow<List<StokKeluarEntity>>

    @Insert
    suspend fun insert(data: StokKeluarEntity)

    @Query("DELETE FROM stok_keluar WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
}
