package com.example.petmatch.domain.model

data class Pet(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L
)