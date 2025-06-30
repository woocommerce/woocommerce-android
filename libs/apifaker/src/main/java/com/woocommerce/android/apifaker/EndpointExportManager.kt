package com.woocommerce.android.apifaker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.getSystemService
import com.google.gson.Gson
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.di.ApiFakerGson
import com.woocommerce.android.apifaker.models.MockedEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class EndpointExportManager @Inject constructor(
    @ApiFakerGson private val gson: Gson,
    private val endpointDao: EndpointDao,
    private val context: Context,
    private val clipDataFactory: ClipDataFactory
) {
    suspend fun exportEndpoints(endpoints: List<MockedEndpoint>, destination: ExportImportDestination): Result<Unit> {
        return when (destination) {
            is ExportImportDestination.File -> exportEndpointsToFile(endpoints, destination.uri)
            ExportImportDestination.Clipboard -> exportEndpointsToClipboard(endpoints)
        }
    }

    private suspend fun exportEndpointsToFile(
        endpoints: List<MockedEndpoint>,
        uri: Uri
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open output stream for: $uri"))

            runCatching {
                outputStream.bufferedWriter().use { writer ->
                    gson.toJson(endpoints.copyWithoutIds(), writer)
                }
            }
        }
    }

    private suspend fun exportEndpointsToClipboard(endpoints: List<MockedEndpoint>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val clipboardManager = context.getSystemService<ClipboardManager>()
                ?: return@withContext Result.failure(IllegalStateException("Could not get clipboard manager"))

            runCatching {
                val json = gson.toJson(endpoints.copyWithoutIds())
                clipboardManager.setPrimaryClip(clipDataFactory.createClipData(json))
            }
        }
    }

    suspend fun importEndpoints(destination: ExportImportDestination): Result<Unit> {
        return when (destination) {
            is ExportImportDestination.File -> importEndpointsFromFile(destination.uri)
            ExportImportDestination.Clipboard -> importEndpointsFromClipboard()
        }
    }

    private suspend fun importEndpointsFromFile(uri: Uri): Result<Unit> {
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

    private suspend fun importEndpointsFromClipboard(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val clipboardManager = context.getSystemService<ClipboardManager>()
                ?: return@withContext Result.failure(IllegalStateException("Could not get clipboard manager"))

            val clipData = clipboardManager.primaryClip
                ?: return@withContext Result.failure(IllegalStateException("Clipboard is empty"))

            runCatching {
                val json = clipData.getItemAt(0).text.toString()
                val endpoints = gson.fromJson(json, Array<MockedEndpoint>::class.java).toList()
                endpointDao.insertEndpoints(endpoints)
            }
        }
    }

    private fun List<MockedEndpoint>.copyWithoutIds(): List<MockedEndpoint> {
        return map {
            it.copy(
                request = it.request.copy(id = 0),
                response = it.response.copy(endpointId = 0)
            )
        }
    }
}

internal class ClipDataFactory @Inject constructor() {
    fun createClipData(text: String): ClipData {
        return ClipData.newPlainText("API Faker Endpoints", text)
    }
}

sealed interface ExportImportDestination {
    data class File(val uri: Uri) : ExportImportDestination
    object Clipboard : ExportImportDestination
}
