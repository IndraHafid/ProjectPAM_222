package com.example.projectahirpam.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barang")
data class BarangEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 0,
    val namaBarang: String,
    val jumlah: Int = 0,
    val kategoriId: Int? = null,
    val updatedAt: String = ""
)