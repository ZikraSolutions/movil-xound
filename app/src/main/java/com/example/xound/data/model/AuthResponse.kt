package com.example.xound.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")       val token: String? = null,
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("message")     val message: String? = null,
    @SerializedName("user")        val user: UserResponse? = null
) {
    fun resolveToken(): String = token ?: accessToken ?: ""
}

data class UserResponse(
    @SerializedName("id")       val id: Long? = null,
    @SerializedName("name")     val name: String? = null,
    @SerializedName("email")    val email: String? = null,
    @SerializedName("roleName") val roleName: String? = null
)
