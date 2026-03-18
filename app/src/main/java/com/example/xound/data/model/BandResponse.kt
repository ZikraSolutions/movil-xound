package com.example.xound.data.model

import com.google.gson.annotations.SerializedName

data class BandResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("inviteCode") val inviteCode: String? = null,
    @SerializedName("adminUserId") val adminUserId: Long? = null,
    @SerializedName("band") val band: String? = null // "null" when no band
)

data class BandMemberResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("userUsername") val userUsername: String? = null,
    @SerializedName("roleName") val roleName: String? = null
)
