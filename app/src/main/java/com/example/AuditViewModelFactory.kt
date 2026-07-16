package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AuditViewModelFactory(private val dao: CashierDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuditViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
