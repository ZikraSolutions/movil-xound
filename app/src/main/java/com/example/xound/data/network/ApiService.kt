package com.example.xound.data.network

import com.example.xound.data.model.AuthRequest
import com.example.xound.data.model.AuthResponse
import com.example.xound.data.model.BandMemberResponse
import com.example.xound.data.model.BandResponse
import com.example.xound.data.model.CreateEventRequest
import com.example.xound.data.model.CreateSongRequest
import com.example.xound.data.model.EventResponse
import com.example.xound.data.model.LyricsResponse
import com.example.xound.data.model.RegisterRequest
import com.example.xound.data.model.SetlistSongResponse
import com.example.xound.data.model.SongResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    // Bands
    @POST("api/bands/join")
    suspend fun joinBand(@Body body: Map<String, String>): Any

    @POST("api/bands/leave")
    suspend fun leaveBand(): Any


    @POST("api/bands")
    suspend fun createBand(@Body body: Map<String, String>): Any

    @GET("api/bands/my")
    suspend fun getMyBand(): BandResponse

    @GET("api/bands/member")
    suspend fun getMyBandAsMember(): BandResponse

    @GET("api/bands/{bandId}/members")
    suspend fun getBandMembers(@Path("bandId") bandId: Long): List<BandMemberResponse>

    @POST("api/bands/regenerate-code")
    suspend fun regenerateInviteCode(): Map<String, String>

    // Admin code
    @POST("api/admin/use-admin-code")
    suspend fun useAdminCode(@Body body: Map<String, String>): Any

    // Events
    @GET("api/events")
    suspend fun getEvents(): List<EventResponse>

    @GET("api/events/published")
    suspend fun getPublishedEvents(): List<EventResponse>

    @POST("api/events")
    suspend fun createEvent(@Body request: CreateEventRequest): EventResponse

    @PUT("api/events/{id}")
    suspend fun updateEvent(@Path("id") id: Long, @Body request: CreateEventRequest): EventResponse

    @PUT("api/events/{id}/publish")
    suspend fun togglePublish(@Path("id") id: Long): EventResponse

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Any

    // Setlist
    @GET("api/events/{eventId}/setlist")
    suspend fun getSetlist(@Path("eventId") eventId: Long): List<SetlistSongResponse>

    @POST("api/events/{eventId}/setlist")
    suspend fun addToSetlist(@Path("eventId") eventId: Long, @Body body: Map<String, Long>): Any

    @DELETE("api/events/{eventId}/setlist/{songId}")
    suspend fun removeFromSetlist(@Path("eventId") eventId: Long, @Path("songId") songId: Long): Any

    // Songs
    @GET("api/songs")
    suspend fun getSongs(): List<SongResponse>

    @GET("api/songs/band")
    suspend fun getBandSongs(): List<SongResponse>

    @GET("api/songs/search")
    suspend fun searchSongs(@Query("title") title: String): List<SongResponse>

    // Favorites
    @GET("api/favorites")
    suspend fun getFavorites(): List<Long>

    @POST("api/favorites/{songId}")
    suspend fun toggleFavorite(@Path("songId") songId: Long): Any

    // Create / Update / Delete song
    @POST("api/songs")
    suspend fun createSong(@Body request: CreateSongRequest): SongResponse

    @PUT("api/songs/{id}")
    suspend fun updateSong(@Path("id") id: Long, @Body request: CreateSongRequest): SongResponse

    @DELETE("api/songs/{id}")
    suspend fun deleteSong(@Path("id") id: Long): Any

    // Lyrics
    @GET("api/lyrics/search")
    suspend fun searchLyrics(@Query("artist") artist: String, @Query("title") title: String): LyricsResponse

    // Chords
    @GET("api/chords/search")
    suspend fun searchChords(@Query("q") query: String): List<Map<String, String>>

    @GET("api/chords/fetch")
    suspend fun fetchChords(@Query("url") url: String): Map<String, String>
}
