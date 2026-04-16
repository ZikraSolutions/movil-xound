// app/src/main/java/com/example/xound/data/model/LiveEvent.kt
package com.example.xound.data.model

import com.google.gson.annotations.SerializedName

data class LiveEvent(
    @SerializedName("type") val type: String = "",
    @SerializedName("bandId") val bandId: Long = 0L,
    @SerializedName("eventId") val eventId: Long? = null,
    @SerializedName("songIndex") val songIndex: Int = 0,
    @SerializedName("lineIndex") val lineIndex: Int = 0,
    @SerializedName("isPlaying") val isPlaying: Boolean = true,
    @SerializedName("comment") val comment: String? = null
)
