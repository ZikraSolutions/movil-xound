package com.example.xound.data.network

import com.example.xound.data.model.AuthRequest
import com.example.xound.data.model.AuthResponse
import com.example.xound.data.model.CreateEventRequest
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.RegisterRequest
import com.example.xound.data.model.SetlistSongResponse
import com.example.xound.data.model.SongResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/users/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    // Events
    @GET("api/events")
    suspend fun getEvents(): List<EventResponse>

    @POST("api/events")
    suspend fun createEvent(@Body request: CreateEventRequest): EventResponse

    @PUT("api/events/{id}/publish")
    suspend fun togglePublish(@Path("id") id: Long): EventResponse

    // Setlist
    @GET("api/events/{eventId}/setlist")
    suspend fun getSetlist(@Path("eventId") eventId: Long): List<SetlistSongResponse>

    // Songs
    @GET("api/songs")
    suspend fun getSongs(): List<SongResponse>

    @GET("api/songs/search")
    suspend fun searchSongs(@Query("title") title: String): List<SongResponse>

    // Favorites
    @GET("api/favorites")
    suspend fun getFavorites(): List<Long>

    @POST("api/favorites/{songId}")
    suspend fun toggleFavorite(@Path("songId") songId: Long): Any
}
