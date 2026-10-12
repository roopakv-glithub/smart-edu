package com.studysync.app.data.network.unicc

import com.google.gson.annotations.SerializedName

class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("cookies") val cookies: String?,
    @SerializedName("csrf") val csrf: String?,
    @SerializedName("authorizedID") val authorizedID: String?,
    @SerializedName("error") val error: String? = null
)

class UniCcSession(
    val authorizedID: String,
    val csrf: String,
    val cookies: String
)

class AcademicRequest(
    val cookies: String,
    val authorizedID: String,
    val csrf: String,
    val semesterId: String,
    val type: String = "ALL"
) {
    init {
        require(cookies.isNotBlank() && authorizedID.isNotBlank() && csrf.isNotBlank())
        require(semesterId.isNotBlank()) { "A verified semester ID is required" }
    }
}

data class StatusResponse(val text: String?)
