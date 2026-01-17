package com.example.projectahirpam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectahirpam.data.database.AppDatabase
import com.example.projectahirpam.data.dao.StokMasukDao
import com.example.projectahirpam.data.dao.KategoriDao
import com.example.projectahirpam.data.entity.BarangEntity
import com.example.projectahirpam.data.entity.KategoriEntity
import com.example.projectahirpam.data.entity.StokMasukEntity
import com.example.projectahirpam.utils.UserSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StokMasukViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao: StokMasukDao = db.stokMasukDao()
    private val barangDao = db.barangDao()
    private val kategoriDao: KategoriDao = db.kategoriDao()
    private val userId = UserSession(application).getUserId()

    val list = dao.getAll(userId)

    private suspend fun defaultKategoriId(): Int {
        val name = "Umum"
        val existing = kategoriDao.getByName(userId, name)
        return existing?.id ?: kategoriDao.insert(KategoriEntity(namaKategori = name, userId = userId)).toInt()
    }

    private fun nowString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    fun tambah(nama: String, jumlah: Int, kategoriId: Int) {
        viewModelScope.launch {
            dao.insert(StokMasukEntity(namaBarang = nama, jumlah = jumlah, tanggal = nowString(), userId = userId))
            val existing = barangDao.getByName(userId, nama)
            if (existing != null) {
                val updated = existing.copy(jumlah = existing.jumlah + jumlah, updatedAt = nowString())
                barangDao.update(updated)
            } else {
                val katId = kategoriId
                barangDao.insert(BarangEntity(
                    userId = userId,
                    namaBarang = nama.trim(),
                    jumlah = jumlah,
                    kategoriId = katId,
                    updatedAt = nowString()
                ))
            }
        }
    }
}
