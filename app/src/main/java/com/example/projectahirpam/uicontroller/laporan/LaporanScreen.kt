package com.example.projectahirpam.uicontroller.laporan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.projectahirpam.data.database.AppDatabase
import com.example.projectahirpam.data.entity.BarangEntity
import com.example.projectahirpam.data.entity.KategoriEntity
import com.example.projectahirpam.data.entity.StokKeluarEntity
import com.example.projectahirpam.data.entity.StokMasukEntity
import com.example.projectahirpam.utils.UserSession
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class HistoryType { object Masuk: HistoryType(); object Keluar: HistoryType() }

data class HistoryItem(
    val id: Int,
    val namaBarang: String,
    val jumlah: Int,
    val tanggal: String,
    val type: HistoryType,
    val kategoriId: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val userId = remember { UserSession(context).getUserId() }
    val db = AppDatabase.getInstance(context)
    val stokMasukDao = db.stokMasukDao()
    val stokKeluarDao = db.stokKeluarDao()
    val kategoriDao = db.kategoriDao()
    val barangDao = db.barangDao()

    var stokMasukList by remember { mutableStateOf<List<StokMasukEntity>>(emptyList()) }
    var stokKeluarList by remember { mutableStateOf<List<StokKeluarEntity>>(emptyList()) }
    var kategoriList by remember { mutableStateOf<List<KategoriEntity>>(emptyList()) }
    var barangList by remember { mutableStateOf<List<BarangEntity>>(emptyList()) }

    // Filters
    var selectedType by remember { mutableStateOf<String>("Semua") } // Semua / Masuk / Keluar
    var dateQuery by remember { mutableStateOf("") } // substring match on tanggal
    var selectedKategoriId by remember { mutableStateOf<Int?>(null) } // null means semua
    var thresholdText by remember { mutableStateOf("5") }

    // Collect flows
    LaunchedEffect(Unit) {
        launch { stokMasukDao.getAll(userId).collectLatest { stokMasukList = it } }
        launch { stokKeluarDao.getAll(userId).collectLatest { stokKeluarList = it } }
        launch { kategoriDao.getAll(userId).collectLatest { kategoriList = it } }
        launch { barangDao.getAll(userId).collectLatest { barangList = it } }
    }

    // Combine stok lists into history items
    val historyItems = remember(stokMasukList, stokKeluarList, barangList) {
        val masuk = stokMasukList.map { m ->
            // Cari barang yang cocok berdasarkan nama (case insensitive)
            val barang = barangList.find { it.namaBarang.trim().equals(m.namaBarang.trim(), ignoreCase = true) }
            val katId = barang?.kategoriId
            HistoryItem(id = m.id, namaBarang = m.namaBarang, jumlah = m.jumlah, tanggal = m.tanggal, type = HistoryType.Masuk, kategoriId = katId)
        }
        val keluar = stokKeluarList.map { k ->
            // Cari barang yang cocok berdasarkan nama (case insensitive)
            val barang = barangList.find { it.namaBarang.trim().equals(k.namaBarang.trim(), ignoreCase = true) }
            val katId = barang?.kategoriId
            HistoryItem(id = k.id, namaBarang = k.namaBarang, jumlah = k.jumlah, tanggal = k.tanggal, type = HistoryType.Keluar, kategoriId = katId)
        }
        (masuk + keluar).sortedByDescending { it.tanggal }
    }

    // Apply filters
    val filteredHistory = remember(historyItems, selectedType, dateQuery, selectedKategoriId) {
        historyItems.filter { item ->
            val matchesType = when (selectedType) {
                "Masuk" -> item.type is HistoryType.Masuk
                "Keluar" -> item.type is HistoryType.Keluar
                else -> true
            }
            val matchesDate = dateQuery.isBlank() || item.tanggal.contains(dateQuery, ignoreCase = true)
            val matchesKategori = selectedKategoriId == null || item.kategoriId == selectedKategoriId
            matchesType && matchesDate && matchesKategori
        }
    }

    // Low stock list
    val threshold = thresholdText.toIntOrNull() ?: 0
    val lowStockItems = remember(barangList, threshold) {
        if (threshold <= 0) emptyList() else barangList.filter { it.jumlah <= threshold }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Stok", color = Color.White) },
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
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)) {

        Spacer(Modifier.height(12.dp))

        // Filters row
        Column {
            // Type filter buttons dengan reset button sejajar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SegmentedTypeFilter(selectedType) { selectedType = it }
                TextButton(
                    onClick = { /* reset */ selectedKategoriId = null; dateQuery = ""; selectedType = "Semua" },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBB86FC))
                ) {
                    Text("Reset", color = Color(0xFFBB86FC))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Filter tanggal di bawah tombol
            OutlinedTextField(
                value = dateQuery,
                onValueChange = { dateQuery = it },
                label = { Text("Filter tanggal (substring)", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
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

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CategoryDropdown(kategoriList = kategoriList, selectedKategoriId = selectedKategoriId, onSelected = { selectedKategoriId = it })
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Ambang hampir habis", color = Color.White) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFBB86FC),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFFBB86FC),
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Riwayat Perubahan Stok", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

        if (filteredHistory.isEmpty()) {
            Text("Tidak ada riwayat sesuai filter", modifier = Modifier.padding(8.dp), color = Color.White)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredHistory) { h ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2D2D2D).copy(alpha = 0.8f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(h.namaBarang, color = Color.White)
                                Text(
                                    "${if (h.type is HistoryType.Masuk) "+" else "-"}${h.jumlah}  •  ${h.tanggal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            val katName = if (h.kategoriId == null) {
                            "(kategori terhapus)"
                        } else {
                            val kategori = kategoriList.find { it.id == h.kategoriId }
                            kategori?.namaKategori ?: "(kategori tidak ditemukan)"
                        }
                            Text(katName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Barang Hampir Habis (≤ $threshold)", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

        if (lowStockItems.isEmpty()) {
            Text("Tidak ada barang hampir habis", modifier = Modifier.padding(8.dp), color = Color.White)
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(lowStockItems) { b ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2D2D2D).copy(alpha = 0.8f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.namaBarang, color = Color.White)
                                Text(
                                    "Stok: ${b.jumlah}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                            val katName = kategoriList.firstOrNull { it.id == b.kategoriId }?.namaKategori ?: "-"
                            Text(katName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
        }
    }
}

@Composable
private fun SegmentedTypeFilter(selectedType: String, onSelected: (String) -> Unit) {
    Row {
        val options = listOf("Semua", "Masuk", "Keluar")
        options.forEach { opt ->
            val selected = selectedType == opt
            FilterChip(
                selected = selected,
                onClick = { onSelected(opt) },
                label = { Text(opt, color = if (selected) Color.White else Color.Gray) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFBB86FC),
                    containerColor = Color(0xFF2D2D2D)
                )
            )
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun CategoryDropdown(kategoriList: List<KategoriEntity>, selectedKategoriId: Int?, onSelected: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = selectedKategoriId?.let { id -> kategoriList.firstOrNull { it.id == id }?.namaKategori } ?: "Semua Kategori"
    Box(modifier = Modifier.wrapContentSize()) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text(selectedName, color = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF2D2D2D)
        ) {
            DropdownMenuItem(
                text = { Text("Semua Kategori", color = Color.White) },
                onClick = { onSelected(null); expanded = false }
            )
            kategoriList.forEach { k ->
                DropdownMenuItem(
                    text = { Text(k.namaKategori, color = Color.White) },
                    onClick = { onSelected(k.id); expanded = false }
                )
            }
        }
    }
}
