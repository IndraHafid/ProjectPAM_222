package com.example.projectahirpam.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stok_masuk")
data class StokMasukEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 0,
    val namaBarang: String,
    val jumlah: Int,
    val tanggal: String
)
