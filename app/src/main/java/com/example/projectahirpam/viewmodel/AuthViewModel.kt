package com.example.projectahirpam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectahirpam.data.database.AppDatabase
import com.example.projectahirpam.data.entity.UserEntity
import com.example.projectahirpam.data.entity.KategoriEntity
import com.example.projectahirpam.utils.UserSession
import com.example.projectahirpam.utils.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getInstance(application).userDao()

    private val _authMessage = MutableStateFlow("")
    val authMessage = _authMessage.asStateFlow()

    fun register(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Validasi input
            if (username.isBlank()) {
                _authMessage.value = "Username tidak boleh kosong"
                return@launch
            }
            if (password.length < 4) {
                _authMessage.value = "Password minimal 4 karakter"
                return@launch
            }

            // Cek batas maksimal akun
            val count = userDao.getUserCount()
            if (count >= 2) {
                _authMessage.value = "Maksimal 2 akun dalam satu perangkat"
                return@launch
            }

            // Cek duplikasi username
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                _authMessage.value = "Username sudah digunakan"
                return@launch
            }

            // Hash password dan simpan
            val hashedPassword = PasswordHasher.hash(password)
            userDao.register(UserEntity(username = username, password = hashedPassword))
            onSuccess()
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Validasi input
            if (username.isBlank() || password.isBlank()) {
                _authMessage.value = "Username dan password harus diisi"
                return@launch
            }

            val user = userDao.getUserByUsername(username)
            if (user != null && PasswordHasher.verify(password, user.password)) {
                UserSession(getApplication()).saveUser(user.id)
                seedDefaultCategories(user.id)
                onSuccess()
            } else {
                _authMessage.value = "Username atau password salah"
            }
        }
    }

    private suspend fun seedDefaultCategories(userId: Int) {
        val db = AppDatabase.getInstance(getApplication())
        val kategoriDao = db.kategoriDao()
        val defaults = listOf("Kamera", "Lensa", "Lampu", "Stand", "Audio", "Aksesoris")
        for (name in defaults) {
            val existing = kategoriDao.getByName(userId, name)
            if (existing == null) {
                kategoriDao.insert(KategoriEntity(namaKategori = name, userId = userId, isFixed = true))
            }
        }
    }
}
