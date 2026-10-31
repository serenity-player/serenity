package io.github.serenity_player.serenity.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface SpotifyApi {
    /** 204 (empty body) when no device is active. */
    @GET("me/player")
    suspend fun getPlayback(@Query("additional_types") types: String = "track,episode"): Response<PlaybackStateDto>

    @PUT("me/player/play") suspend fun play(): Response<Unit>
    @PUT("me/player/pause") suspend fun pause(): Response<Unit>
    @POST("me/player/next") suspend fun next(): Response<Unit>
    @POST("me/player/previous") suspend fun previous(): Response<Unit>
    @PUT("me/player/seek") suspend fun seek(@Query("position_ms") positionMs: Long): Response<Unit>
    @PUT("me/player/shuffle") suspend fun shuffle(@Query("state") on: Boolean): Response<Unit>
    @PUT("me/player/repeat") suspend fun repeat(@Query("state") mode: String): Response<Unit>
    @PUT("me/player/volume") suspend fun volume(@Query("volume_percent") percent: Int): Response<Unit>

    @GET("me/player/devices")
    suspend fun getDevices(): Response<DevicesDto>

    /** Moves playback to another Connect device. */
    @PUT("me/player")
    suspend fun transfer(@Body body: TransferBody): Response<Unit>

    // ---- Library (all still available in dev mode after the Feb 2026 changes) ----
    @GET("me/playlists")
    suspend fun getPlaylists(@Query("limit") limit: Int, @Query("offset") offset: Int): Response<PagedDto<PlaylistSummaryDto>>

    @GET("me/tracks")
    suspend fun getSavedTracks(@Query("limit") limit: Int, @Query("offset") offset: Int): Response<PagedDto<SavedTrackDto>>

    @GET("me/albums")
    suspend fun getSavedAlbums(@Query("limit") limit: Int, @Query("offset") offset: Int): Response<PagedDto<SavedAlbumDto>>

    /** Cursor-paged: pass the previous page's `cursors.after`. */
    @GET("me/following")
    suspend fun getFollowedArtists(
        @Query("type") type: String,
        @Query("limit") limit: Int,
        @Query("after") after: String?,
    ): Response<FollowedArtistsDto>

    /** Renamed from /tracks in Feb 2026. Only returns contents for playlists the user owns. */
    @GET("playlists/{id}/items")
    suspend fun getPlaylistItems(
        @Path("id") id: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("additional_types") types: String,
    ): Response<PagedDto<PlaylistEntryDto>>

    @GET("albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Path("id") id: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): Response<PagedDto<TrackDto>>

    /** Start a context (playlist/album/artist) or a list of tracks, optionally on a specific device. */
    @PUT("me/player/play")
    suspend fun playContent(@Query("device_id") deviceId: String?, @Body body: PlayBody): Response<Unit>

    // ---- Search (limit is capped at 10 in dev mode), artist albums, queue ----
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): Response<SearchResponseDto>

    @GET("artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") id: String,
        @Query("include_groups") groups: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): Response<PagedDto<AlbumFullDto>>

    @GET("me/player/queue")
    suspend fun getQueue(): Response<QueueDto>

    @POST("me/player/queue")
    suspend fun addToQueue(@Query("uri") uri: String): Response<Unit>
}
