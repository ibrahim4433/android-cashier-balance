package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AuditViewModel(private val dao: CashierDao) : ViewModel() {

    private val moshi = Moshi.Builder().build()
    private val productListAdapter = moshi.adapter<List<Product>>(
        Types.newParameterizedType(List::class.java, Product::class.java)
    )

    // Current session products
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _amountHandedOver1 = MutableStateFlow("")
    val amountHandedOver1 = _amountHandedOver1.asStateFlow()
    
    private val _amountHandedOver2 = MutableStateFlow("")
    val amountHandedOver2 = _amountHandedOver2.asStateFlow()
    
    private val _amountHandedOver3 = MutableStateFlow("")
    val amountHandedOver3 = _amountHandedOver3.asStateFlow()

    val systemTotal: StateFlow<Int> = _products
        .map { list -> list.sumOf { it.total } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val cashLeftInDrawer: StateFlow<Int> = combine(
        systemTotal,
        _amountHandedOver1,
        _amountHandedOver2,
        _amountHandedOver3
    ) { total, a1, a2, a3 ->
        val sum = (a1.toIntOrNull() ?: 0) + (a2.toIntOrNull() ?: 0) + (a3.toIntOrNull() ?: 0)
        sum - total
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // DB state
    val dbProducts = dao.getAllProducts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dbSnapshots = dao.getAllSnapshots().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Load initial products if empty
        viewModelScope.launch {
            dao.getAllProducts().first().let { entities ->
                if (entities.isEmpty()) {
                    // Seed initial data
                    val initial = listOf(
                        ProductEntity(name = "عرض فيشة", defaultPrice = 5000),
                        ProductEntity(name = "عرض 6", defaultPrice = 25000),
                        ProductEntity(name = "عرض 11", defaultPrice = 40000),
                        ProductEntity(name = "7500", defaultPrice = 7500),
                        ProductEntity(name = "12500", defaultPrice = 12500),
                        ProductEntity(name = "15000", defaultPrice = 15000),
                        ProductEntity(name = "العاب", defaultPrice = 1),
                        ProductEntity(name = "مرسم", defaultPrice = 1)
                    )
                    initial.forEach { dao.insertProduct(it) }
                }
            }
        }

        // Keep session products in sync with db products when they are added/removed
        viewModelScope.launch {
            dbProducts.collect { entities ->
                // Map db products to current session products, preserving quantities if possible
                val currentMap = _products.value.associateBy { it.id }
                val newList = entities.map { entity ->
                    val idStr = entity.id.toString()
                    val existing = currentMap[idStr]
                    Product(
                        id = idStr,
                        name = entity.name,
                        price = entity.defaultPrice,
                        quantityString = existing?.quantityString ?: ""
                    )
                }
                _products.value = newList
            }
        }
    }

    fun updateQuantity(id: String, qtyString: String) {
        if (qtyString.all { it.isDigit() } || qtyString.isEmpty()) {
            _products.update { list ->
                list.map { if (it.id == id) it.copy(quantityString = qtyString) else it }
            }
        }
    }

    fun updateAmountHandedOver(index: Int, amountStr: String) {
        if (amountStr.all { it.isDigit() } || amountStr.isEmpty()) {
            when (index) {
                1 -> _amountHandedOver1.value = amountStr
                2 -> _amountHandedOver2.value = amountStr
                3 -> _amountHandedOver3.value = amountStr
            }
        }
    }

    fun saveSession() {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val json = productListAdapter.toJson(_products.value) ?: "[]"
            
            val snapshot = AuditSnapshotEntity(
                dateString = dateStr,
                timestamp = System.currentTimeMillis(),
                systemTotal = systemTotal.value,
                cashLeft = cashLeftInDrawer.value,
                amountHandedOver1 = _amountHandedOver1.value,
                amountHandedOver2 = _amountHandedOver2.value,
                amountHandedOver3 = _amountHandedOver3.value,
                productsJson = json
            )
            dao.insertSnapshot(snapshot)
        }
    }

    fun loadSnapshot(snapshot: AuditSnapshotEntity) {
        viewModelScope.launch {
            val loadedProducts = try {
                productListAdapter.fromJson(snapshot.productsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            _products.value = loadedProducts
            _amountHandedOver1.value = snapshot.amountHandedOver1
            _amountHandedOver2.value = snapshot.amountHandedOver2
            _amountHandedOver3.value = snapshot.amountHandedOver3
        }
    }

    fun deleteSnapshot(snapshot: AuditSnapshotEntity) {
        viewModelScope.launch {
            dao.deleteSnapshot(snapshot.id)
        }
    }
    
    fun resetSession() {
        _amountHandedOver1.value = ""
        _amountHandedOver2.value = ""
        _amountHandedOver3.value = ""
        _products.update { list ->
            list.map { it.copy(quantityString = "") }
        }
    }

    // --- Product Management ---
    fun addProduct(name: String, price: Int) {
        viewModelScope.launch {
            dao.insertProduct(ProductEntity(name = name, defaultPrice = price))
        }
    }

    fun updateProduct(id: Int, name: String, price: Int) {
        viewModelScope.launch {
            dao.updateProduct(ProductEntity(id = id, name = name, defaultPrice = price))
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            dao.getAllProducts().first().find { it.id == id }?.let {
                dao.deleteProduct(it)
            }
        }
    }
}
