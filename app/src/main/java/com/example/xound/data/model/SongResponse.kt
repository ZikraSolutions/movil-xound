package com.example.xound.data.model

data class SongResponse(
    val id: Long = 0,
    val title: String = "",
    val artist: String? = null,
    val tone: String? = null,
    val content: String? = null,
    val lyrics: String? = null,
    val notes: String? = null,
    val bpm: Int? = null,
    val timeSignature: String? = null,
    val userId: Long? = null,
    val status: Boolean = true,
    val createdAt: String? = null
)
