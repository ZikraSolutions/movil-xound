package com.example.xound.data.model

data class EventResponse(
    val id: Long = 0,
    val title: String = "",
    val eventDate: String? = null,
    val venue: String? = null,
    val published: Boolean = false,
    val shareCode: String? = null,
    val userId: Long? = null,
    val status: Boolean = true,
    val createdAt: String? = null
)

data class CreateEventRequest(
    val title: String,
    val eventDate: String?,
    val venue: String?
)

data class SetlistSongResponse(
    val id: Long = 0,
    val eventId: Long = 0,
    val songId: Long = 0,
    val position: Int = 0
)
