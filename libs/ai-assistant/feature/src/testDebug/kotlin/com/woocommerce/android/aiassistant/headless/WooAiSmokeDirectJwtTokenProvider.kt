@file:Suppress("ImportOrdering")

package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WooAiSmokeDirectJwtTokenProvider(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val siteUrl: String,
    private val username: String,
    private val appPassword: String,
    private val redactor: WooAiSmokeRedactor,
) : JwtTokenProvider {
    private var cachedToken: String? = null

    override suspend fun provide(): String =
        cachedToken ?: mintToken().also { cachedToken = it }

    override suspend fun invalidate() {
        cachedToken = null
    }

    private suspend fun mintToken(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(siteUrl.trimEnd('/') + JETPACK_AI_JWT_PATH)
            .header("Authorization", Credentials.basic(username, appPassword))
            .header("Accept", "application/json")
            .post("".toRequestBody(APPLICATION_JSON))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw authException(
                        "Jetpack AI JWT mint failed status=${response.code} endpoint=jetpack-ai-jwt $body",
                    )
                }
                tokenFrom(body) ?: throw authException(
                    "Jetpack AI JWT response did not include a token endpoint=jetpack-ai-jwt",
                )
            }
        } catch (e: AssistantAuthException) {
            throw e
        } catch (e: IOException) {
            throw authException("Jetpack AI JWT network failure endpoint=jetpack-ai-jwt ${e.message}", e)
        } catch (e: SerializationException) {
            throw authException("Malformed Jetpack AI JWT response endpoint=jetpack-ai-jwt ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw authException("Malformed Jetpack AI JWT response endpoint=jetpack-ai-jwt ${e.message}", e)
        }
    }

    private fun tokenFrom(body: String): String? {
        val root = json.parseToJsonElement(body).jsonObject
        return root["token"]?.jsonPrimitive?.contentOrNull
            ?: root["jwt"]?.jsonPrimitive?.contentOrNull
            ?: root["data"]?.jsonObject?.get("token")?.jsonPrimitive?.contentOrNull
            ?: root["data"]?.jsonObject?.get("jwt")?.jsonPrimitive?.contentOrNull
    }

    private fun authException(
        message: String,
        cause: Throwable? = null,
    ) = AssistantAuthException(redactor.redact(message), cause)

    private companion object {
        private const val JETPACK_AI_JWT_PATH = "/wp-json/jetpack/v4/jetpack-ai-jwt"
        private val APPLICATION_JSON = "application/json".toMediaType()
    }
}
