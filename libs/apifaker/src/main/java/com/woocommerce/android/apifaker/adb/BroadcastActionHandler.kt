package com.woocommerce.android.apifaker.adb

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.woocommerce.android.apifaker.ApiFakerConfig
import com.woocommerce.android.apifaker.LOG_TAG
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.di.ApiFakerGson
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import java.io.File
import javax.inject.Inject

internal class BroadcastActionHandler @Inject constructor(
    private val endpointDao: EndpointDao,
    private val apiFakerConfig: ApiFakerConfig,
    @ApiFakerGson private val gson: Gson
) {
    suspend fun handle(intent: Intent) {
        when (intent.action) {
            Actions.SET_STATUS -> handleSetStatus(intent)
            Actions.ADD_ENDPOINT -> handleAddEndpoint(intent)
            Actions.EDIT_ENDPOINT -> handleEditEndpoint(intent)
            Actions.REMOVE_ENDPOINT -> handleRemoveEndpoint(intent)
            Actions.CLEAR_ENDPOINTS -> handleClearEndpoints()
            Actions.LIST_ENDPOINTS -> handleListEndpoints()
            Actions.IMPORT_ENDPOINTS -> handleImportEndpoints(intent)
            else -> Log.w(LOG_TAG, "ADB: Unknown action: ${intent.action}")
        }
    }

    private fun handleSetStatus(intent: Intent) {
        val enabled = intent.getBooleanExtra(Extras.ENABLED, false)
        apiFakerConfig.setStatus(enabled)
        Log.i(LOG_TAG, "ADB: ApiFaker status set to $enabled")
    }

    private suspend fun handleAddEndpoint(intent: Intent) {
        val request = Request(
            type = IntentExtrasParser.parseApiType(intent),
            path = intent.getStringExtra(Extras.PATH)
                ?: error("Missing required extra: ${Extras.PATH}"),
            httpMethod = IntentExtrasParser.parseHttpMethod(intent),
            queryParameters = IntentExtrasParser.parseQueryParameters(intent),
            body = IntentExtrasParser.parseRequestBody(intent)
        )
        val response = Response(
            statusCode = intent.getIntExtra(Extras.RESPONSE_STATUS_CODE, DEFAULT_STATUS_CODE),
            body = IntentExtrasParser.parseResponseBody(intent)
        )
        endpointDao.insertEndpoint(request, response)
        Log.i(LOG_TAG, "ADB: Endpoint added - ${request.type} ${request.httpMethod ?: "ANY"} ${request.path}")
    }

    private suspend fun handleEditEndpoint(intent: Intent) {
        val id = intent.getLongExtra(Extras.ENDPOINT_ID, -1)
        require(id > 0) { "Missing or invalid ${Extras.ENDPOINT_ID}" }

        val existing = endpointDao.getEndpoint(id)
            ?: error("Endpoint with id $id not found")

        val request = existing.request.copy(
            type = if (intent.hasExtra(Extras.API_TYPE)) {
                IntentExtrasParser.parseApiType(intent)
            } else {
                existing.request.type
            },
            path = intent.getStringExtra(Extras.PATH) ?: existing.request.path,
            httpMethod = if (intent.hasExtra(Extras.HTTP_METHOD)) {
                IntentExtrasParser.parseHttpMethod(intent)
            } else {
                existing.request.httpMethod
            },
            queryParameters = if (intent.hasExtra(Extras.QUERY_PARAMS)) {
                IntentExtrasParser.parseQueryParameters(intent)
            } else {
                existing.request.queryParameters
            },
            body = if (intent.hasExtra(Extras.REQUEST_BODY) || intent.hasExtra(Extras.REQUEST_BODY_FILE)) {
                IntentExtrasParser.parseRequestBody(intent)
            } else {
                existing.request.body
            }
        )
        val response = existing.response.copy(
            statusCode = intent.getIntExtra(
                Extras.RESPONSE_STATUS_CODE,
                existing.response.statusCode
            ),
            body = if (intent.hasExtra(Extras.RESPONSE_BODY) || intent.hasExtra(Extras.RESPONSE_BODY_FILE)) {
                IntentExtrasParser.parseResponseBody(intent)
            } else {
                existing.response.body
            }
        )
        endpointDao.insertEndpoint(request, response)
        Log.i(LOG_TAG, "ADB: Endpoint $id updated")
    }

    private suspend fun handleRemoveEndpoint(intent: Intent) {
        val id = intent.getLongExtra(Extras.ENDPOINT_ID, -1)
        require(id > 0) { "Missing or invalid ${Extras.ENDPOINT_ID}" }

        val endpoint = endpointDao.getEndpoint(id)
            ?: error("Endpoint with id $id not found")

        endpointDao.deleteRequest(endpoint.request)
        Log.i(LOG_TAG, "ADB: Endpoint $id removed")
    }

    private suspend fun handleClearEndpoints() {
        endpointDao.deleteAllRequests()
        Log.i(LOG_TAG, "ADB: All endpoints cleared")
    }

    private suspend fun handleListEndpoints() {
        val endpoints = endpointDao.getAllEndpoints()
        Log.i(LOG_TAG, "ADB: === ENDPOINTS_START (${endpoints.size} endpoints) ===")
        endpoints.forEach { endpoint ->
            val json = gson.toJson(endpoint)
            // Log.i has a ~4000 char limit per call, so chunk long entries
            json.chunked(MAX_LOG_LENGTH).forEach { chunk ->
                Log.i(LOG_TAG, "ADB: ENDPOINT: $chunk")
            }
        }
        Log.i(LOG_TAG, "ADB: === ENDPOINTS_END ===")
    }

    private suspend fun handleImportEndpoints(intent: Intent) {
        val filePath = intent.getStringExtra(Extras.FILE)
            ?: error("Missing required extra: ${Extras.FILE}")
        val file = File(filePath)
        require(file.exists()) { "File not found: $filePath" }

        val endpoints = file.bufferedReader().use { reader ->
            gson.fromJson(reader, Array<MockedEndpoint>::class.java).toList()
        }
        endpointDao.insertEndpoints(endpoints)
        Log.i(LOG_TAG, "ADB: Imported ${endpoints.size} endpoints from $filePath")
    }

    companion object {
        private const val DEFAULT_STATUS_CODE = 200
        private const val MAX_LOG_LENGTH = 3000
    }
}
