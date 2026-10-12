package com.studysync.app.data.mapper

import com.studysync.app.data.network.unicc.LoginResponse
import com.studysync.app.data.network.unicc.UniCcSession

object UniCcMappers {

    fun mapLoginResponseToSession(response: LoginResponse): UniCcSession? {
        if (!response.success) return null
        val authId = response.authorizedID?.takeIf { it.isNotBlank() } ?: return null
        val csrf = response.csrf?.takeIf { it.isNotBlank() } ?: return null
        val cookies = response.cookies?.takeIf { it.isNotBlank() } ?: return null
        return UniCcSession(
            authorizedID = authId,
            csrf = csrf,
            cookies = cookies
        )
    }
}
