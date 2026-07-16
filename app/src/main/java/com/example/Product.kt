package com.example

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Product(
    val id: String,
    val name: String,
    val price: Int,
    val quantityString: String = ""
) {
    val total: Int get() = price * (quantityString.toIntOrNull() ?: 0)
}
