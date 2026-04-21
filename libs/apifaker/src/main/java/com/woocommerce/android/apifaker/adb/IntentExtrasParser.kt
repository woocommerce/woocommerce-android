package com.woocommerce.android.apifaker.adb

import android.content.Intent
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.QueryParameter
import java.io.File

internal object IntentExtrasParser {
    fun parseApiType(intent: Intent): ApiType {
        val typeStr = intent.getStringExtra(Extras.API_TYPE)
            ?: error("Missing required extra: ${Extras.API_TYPE}")
        return when (typeStr) {
            "wp-api" -> ApiType.WPApi
            "wp-com" -> ApiType.WPCom
            "custom" -> ApiType.Custom(
                host = intent.getStringExtra(Extras.CUSTOM_HOST)
                    ?: error("Custom API type requires '${Extras.CUSTOM_HOST}' extra")
            )
            else -> error("Unknown api_type: $typeStr. Expected: wp-api, wp-com, or custom")
        }
    }

    fun parseHttpMethod(intent: Intent): HttpMethod? {
        return intent.getStringExtra(Extras.HTTP_METHOD)?.let {
            HttpMethod.valueOf(it.uppercase())
        }
    }

    fun parseQueryParameters(intent: Intent): List<QueryParameter> {
        return intent.getStringExtra(Extras.QUERY_PARAMS)
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { param ->
                val parts = param.split("=", limit = 2)
                require(parts.size == 2) {
                    "Invalid query parameter format: '$param'. Expected: name=value"
                }
                QueryParameter(parts[0].trim(), parts[1].trim())
            }
            ?: emptyList()
    }

    fun parseResponseBody(intent: Intent): String? {
        intent.getStringExtra(Extras.RESPONSE_BODY_FILE)?.let { filePath ->
            return readFileContent(filePath)
        }
        return intent.getStringExtra(Extras.RESPONSE_BODY)
    }

    fun parseRequestBody(intent: Intent): String? {
        intent.getStringExtra(Extras.REQUEST_BODY_FILE)?.let { filePath ->
            return readFileContent(filePath)
        }
        return intent.getStringExtra(Extras.REQUEST_BODY)
    }

    private fun readFileContent(filePath: String): String {
        val file = File(filePath)
        require(file.exists()) { "File not found: $filePath" }
        return file.readText()
    }
}
