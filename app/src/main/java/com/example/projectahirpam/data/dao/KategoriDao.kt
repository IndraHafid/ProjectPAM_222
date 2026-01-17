package com.example.projectahirpam.data.dao

import androidx.room.*
import com.example.projectahirpam.data.entity.KategoriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KategoriDao {

    @Query("SELECT * FROM kategori WHERE userId = :userId ORDER BY namaKategori ASC")
    fun getAll(userId: Int): Flow<List<KategoriEntity>>

    @Query("SELECT * FROM kategori WHERE userId = :userId AND namaKategori = :name LIMIT 1")
    suspend fun getByName(userId: Int, name: String): KategoriEntity?

    @Insert
    suspend fun insert(kategori: KategoriEntity): Long

    @Update
    suspend fun update(kategori: KategoriEntity)

    @Delete
    suspend fun delete(kategori: KategoriEntity)

    @Query("DELETE FROM kategori WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
}
