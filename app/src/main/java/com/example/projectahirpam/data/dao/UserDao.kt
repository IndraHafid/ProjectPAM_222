package com.example.projectahirpam.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.projectahirpam.data.entity.UserEntity

@Dao
interface UserDao {

    @Insert
    suspend fun register(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Int)
}
