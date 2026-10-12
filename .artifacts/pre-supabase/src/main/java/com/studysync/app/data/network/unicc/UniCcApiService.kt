package com.studysync.app.data.network.unicc

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import com.google.gson.JsonObject

interface UniCcApiService {

    @GET("api/status")
    suspend fun getStatus(): Response<StatusResponse>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Academic foundation endpoint stubs for subsequent phases
    @POST("api/schedule")
    suspend fun getSchedule(@Body session: AcademicRequest): Response<JsonObject>

    @POST("api/attendance")
    suspend fun getAttendance(@Body session: AcademicRequest): Response<JsonObject>

    @POST("api/grades")
    suspend fun getGrades(@Body session: AcademicRequest): Response<JsonObject>

    @POST("api/calendar")
    suspend fun getCalendar(@Body session: AcademicRequest): Response<JsonObject>
}
