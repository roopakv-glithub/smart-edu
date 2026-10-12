package com.studysync.app.data.network.supabase

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApiService {

    @GET("rest/v1/tasks")
    suspend fun getTasks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("select") select: String = "*"
    ): Response<List<JsonObject>>

    @POST("rest/v1/tasks")
    suspend fun insertTask(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body task: JsonObject
    ): Response<List<JsonObject>>

    @GET("rest/v1/notes")
    suspend fun getNotes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("select") select: String = "*"
    ): Response<List<JsonObject>>

    @GET("rest/v1/pods")
    suspend fun getPods(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("select") select: String = "*"
    ): Response<List<JsonObject>>
}
