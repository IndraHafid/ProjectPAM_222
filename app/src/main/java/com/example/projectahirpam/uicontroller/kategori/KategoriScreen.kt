package com.example.projectahirpam.uicontroller.kategori

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

import com.example.projectahirpam.utils.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategoriScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userId = remember { UserSession(context).getUserId() }
    val db = AppDatabase.getInstance(context)
    val kategoriDao = db.kategoriDao()
    val barangDao = db.barangDao()

    val daftarKategori by kategoriDao.getAll(userId).collectAsState(initial = emptyList())

    var selectedKategori by remember { mutableStateOf<KategoriEntity?>(null) }
    var kategoriSearch by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf<KategoriEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // New local UI state for adding kategori
    var showAddDialog by remember { mutableStateOf(false) }
    var newKategoriName by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedKategori == null) "Halaman Kategori" else "Barang: ${selectedKategori?.namaKategori ?: ""}", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedKategori == null) {
                            onBack()
                        } else {
                            selectedKategori = null
                        }
                    }) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        // Jika belum memilih kategori, tampilkan daftar kategori
        if (selectedKategori == null) {
            OutlinedTextField(
                value = kategoriSearch,
                onValueChange = { kategoriSearch = it },
                label = { Text("Cari Kategori / Jenis", color = Color.White) },
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
            val filteredKategori = remember(daftarKategori, kategoriSearch) {
                if (kategoriSearch.isBlank()) daftarKategori
                else daftarKategori.filter { it.namaKategori.contains(kategoriSearch, ignoreCase = true) }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredKategori) { kategori ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                selectedKategori = kategori
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2D2D2D).copy(alpha = 0.8f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(kategori.namaKategori, modifier = Modifier.weight(1f), color = Color.White)
                            TextButton(onClick = {
                                showEditDialog = kategori
                                editName = kategori.namaKategori
                                editError = ""
                            }) { Text("Edit", color = Color(0xFFBB86FC)) }
                            TextButton(onClick = { scope.launch { kategoriDao.delete(kategori) } }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            FloatingActionButton(onClick = {
                // Open dialog to input kategori name instead of inserting immediately
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        } else {
            // Jika kategori dipilih, tampilkan manajemen barang inline di halaman yang sama
            BarangManagementContent(
                kategori = selectedKategori!!,
                onBackToList = { selectedKategori = null },
                barangDao = barangDao,
                userId = userId
            )
        }
    }
    }
    }

    // Add dialog outside the Column so it overlays properly
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newKategoriName = ""
                addError = ""
            },
            title = { Text("Tambah Kategori") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newKategoriName,
                        onValueChange = { newKategoriName = it; addError = "" },
                        label = { Text("Nama Kategori") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (addError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(addError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newKategoriName.trim()
                    if (trimmed.isBlank()) {
                        addError = "Nama kategori tidak boleh kosong"
                        return@TextButton
                    }
                    scope.launch {
                        kategoriDao.insert(KategoriEntity(namaKategori = trimmed, userId = userId))
                        showAddDialog = false
                        newKategoriName = ""
                        addError = ""
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newKategoriName = ""
                    addError = ""
                }) { Text("Batal") }
            }
        )
    }

    if (showEditDialog != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = null
                editName = ""
                editError = ""
            },
            title = { Text("Edit Kategori") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; editError = "" },
                        label = { Text("Nama Kategori") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(editError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = editName.trim()
                    if (trimmed.isBlank()) {
                        editError = "Nama kategori tidak boleh kosong"
                        return@TextButton
                    }
                    scope.launch {
                        val existing = kategoriDao.getByName(userId, trimmed)
                        val current = showEditDialog!!
                        if (existing != null && existing.id != current.id) {
                            editError = "Nama kategori sudah ada"
                            return@launch
                        }
                        kategoriDao.update(current.copy(namaKategori = trimmed))
                        showEditDialog = null
                        editName = ""
                        editError = ""
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = null
                    editName = ""
                    editError = ""
                }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun BarangManagementContent(
    kategori: KategoriEntity,
    onBackToList: () -> Unit,
    barangDao: com.example.projectahirpam.data.dao.BarangDao,
    userId: Int
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val stokMasukDao = remember { db.stokMasukDao() }
    val stokKeluarDao = remember { db.stokKeluarDao() }

    var search by remember { mutableStateOf("") }
    var barangList by remember { mutableStateOf<List<BarangEntity>>(emptyList()) }

    // collect search with debounce
    LaunchedEffect(kategori.id) {
        snapshotFlow { search }
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(300)
            .collectLatest { q ->
                if (q.isBlank()) {
                    barangDao.getAllByKategori(kategori.id).collectLatest { barangList = it }
                } else {
                    barangDao.searchInKategori(kategori.id, q).collectLatest { barangList = it }
                }
            }
    }

    var editingBarang by remember { mutableStateOf<BarangEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<BarangEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari barang", color = Color.White) },
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

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(barangList) { barang ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D2D2D).copy(alpha = 0.8f)
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(barang.namaBarang, color = Color.White)
                            Text("Stok: ${barang.jumlah}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
            if (barangList.isEmpty()) {
                item {
                    Text("Belum ada barang", modifier = Modifier.padding(8.dp), color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // detail kategori menjadi read-only: tidak ada dialog tambah/edit/hapus barang
}

