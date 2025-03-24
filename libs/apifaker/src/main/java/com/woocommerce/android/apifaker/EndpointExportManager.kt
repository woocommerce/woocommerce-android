package com.woocommerce.android.apifaker

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.MockedEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class EndpointExportManager @Inject constructor(
    private val gson: Gson,
    private val endpointDao: EndpointDao,
    private val context: Context
) {
    suspend fun exportEndpoints(
        endpoints: List<MockedEndpoint>,
        uri: Uri
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open output stream for: $uri"))

            val endpointsExcludingIds = endpoints.map {
                it.copy(
                    request = it.request.copy(id = 0),
                    response = it.response.copy(endpointId = 0)
                )
            }

            runCatching {
                outputStream.bufferedWriter().use { writer ->
                    gson.toJson(endpointsExcludingIds, writer)
                }
            }
        }
    }

    suspend fun importEndpoints(uri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for uri: $uri"))

            runCatching {
                val endpoints = inputStream.bufferedReader().use { reader ->
                    gson.fromJson(reader, Array<MockedEndpoint>::class.java).toList()
                }

                endpointDao.insertEndpoints(endpoints)
            }
        }
    }
}
