package com.example.petmatch.domain.model

data class Pet @JvmOverloads constructor(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val age: Int = 0,
    val type: String = "",
    val breed: String = "",
    val sex: String = "",
    val vaccinated: Boolean = false,
    val sterilized: Boolean = false
)