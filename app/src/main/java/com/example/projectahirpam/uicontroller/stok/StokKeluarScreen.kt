package com.example.projectahirpam.uicontroller.stok

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.projectahirpam.viewmodel.StokKeluarViewModel
import com.example.projectahirpam.data.database.AppDatabase
import com.example.projectahirpam.data.dao.BarangDao
import com.example.projectahirpam.utils.UserSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokKeluarScreen(viewModel: StokKeluarViewModel = viewModel(), onBack: () -> Unit = {}) {

    var nama by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showStockCheckDialog by remember { mutableStateOf(false) }
    var stockInfo by remember { mutableStateOf("") }
    var pendingTransaction by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val list by viewModel.list.collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userId = remember { UserSession(context).getUserId() }
    val kategoriDao = remember { db.kategoriDao() }
    val barangDao = remember { db.barangDao() }
    val kategoriList by kategoriDao.getAll(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedKategoriId by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stok Keluar", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFF1A1A1A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF2D2D2D),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
        ) {
            Column(Modifier.padding(padding).padding(16.dp)) {

                if (kategoriList.isNotEmpty() && selectedKategoriId == null) {
                    selectedKategoriId = kategoriList.first().id
                }

                // Kategori selector
                if (kategoriList.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedName = kategoriList.firstOrNull { it.id == selectedKategoriId }?.namaKategori ?: "-"
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("Kategori: $selectedName", color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        kategoriList.forEach { k ->
                            DropdownMenuItem(
                                text = { Text(k.namaKategori, color = Color.Black) },
                                onClick = { selectedKategoriId = k.id; expanded = false }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Barang", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFBB86FC),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFFBB86FC),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = jumlah,
                    onValueChange = { jumlah = it },
                    label = { Text("Jumlah", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFBB86FC),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFFBB86FC),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Spacer(Modifier.height(16.dp))

                Button(onClick = {
                    // Validasi input
                    if (nama.isBlank()) {
                        errorMessage = "Nama barang wajib diisi!"
                        showErrorDialog = true
                        return@Button
                    }

                    val jumlahInt = jumlah.toIntOrNull()
                    if (jumlahInt == null || jumlahInt <= 0) {
                        errorMessage = "Kolom jumlah wajib di isi dengan bilangan!"
                        showErrorDialog = true
                        return@Button
                    }

                    // Periksa ketersediaan stok menggunakan coroutine
                    coroutineScope.launch {
                        try {
                            val barangList = withContext(Dispatchers.IO) {
                                barangDao.getAllSync(userId)
                            }
                            val normalizedInput = nama.trim().lowercase()
                            val existingBarang = barangList.find { 
                                it.namaBarang.trim().lowercase() == normalizedInput 
                            }

                            println("DEBUG: Nama input: '$nama' (normalized: '$normalizedInput')")
                            println("DEBUG: Jumlah input: $jumlahInt")
                            println("DEBUG: Barang list size: ${barangList.size}")
                            barangList.forEach { 
                                println("DEBUG: Barang tersedia: '${it.namaBarang}' (normalized: '${it.namaBarang.trim().lowercase()}'), stok: ${it.jumlah}")
                            }
                            println("DEBUG: Barang ditemukan: ${existingBarang?.namaBarang}, stok: ${existingBarang?.jumlah}")

                            if (existingBarang == null) {
                                errorMessage = "Barang '$nama' tidak ditemukan dalam daftar!"
                                showErrorDialog = true
                                return@launch
                            }

                            println("DEBUG: Cek stok - Tersedia: ${existingBarang.jumlah}, Diminta: $jumlahInt")
                            println("DEBUG: Kondisi: ${existingBarang.jumlah} < $jumlahInt = ${existingBarang.jumlah < jumlahInt}")

                            if (existingBarang.jumlah < jumlahInt) {
                                stockInfo = "Stok tersedia untuk '$nama': ${existingBarang.jumlah}\nJumlah yang diminta: $jumlahInt\n\nStok tidak mencukupi!"
                                pendingTransaction = Pair(nama, jumlahInt)
                                showStockCheckDialog = true
                                return@launch
                            }

                            // Jika stok mencukupi, lanjutkan transaksi (termasuk jika akan habis)
                            println("DEBUG: Stok mencukupi, melanjutkan transaksi")
                            val kid = selectedKategoriId ?: kategoriList.firstOrNull()?.id ?: 0
                            viewModel.tambah(nama, jumlahInt, kid)
                            // Reset form
                            nama = ""
                            jumlah = ""
                        } catch (e: Exception) {
                            println("DEBUG: Exception: ${e.message}")
                            e.printStackTrace()
                            errorMessage = "Terjadi kesalahan: ${e.message}"
                            showErrorDialog = true
                        }
                    }
                }) {
                    Text("Tambah Stok Keluar")
                }


                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2D2D2D).copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                "${it.namaBarang} -${it.jumlah}",
                                modifier = Modifier.padding(12.dp),
                                color = Color.White
                            )
                        }
                    }
                }

            }
        }

        // Stock Check Dialog
        if (showStockCheckDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showStockCheckDialog = false
                    pendingTransaction = null
                },
                title = {
                    Text("Pemeriksaan Stok", color = Color.Black)
                },
                text = {
                    Text(stockInfo, color = Color.Black)
                },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            showStockCheckDialog = false
                            pendingTransaction = null
                        }
                    ) {
                        Text("OK", color = Color(0xFFBB86FC))
                    }
                },
                containerColor = Color.White
            )
        }

        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text("Validasi Error", color = Color.Black)
                },
                text = {
                    Text(errorMessage, color = Color.Black)
                },
                confirmButton = {
                    TextButton(
                        onClick = { showErrorDialog = false }
                    ) {
                        Text("OK", color = Color(0xFFBB86FC))
                    }
                },
                containerColor = Color.White
            )
        }
    }
}
