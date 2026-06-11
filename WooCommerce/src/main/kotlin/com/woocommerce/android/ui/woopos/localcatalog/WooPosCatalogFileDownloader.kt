package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WooPosCatalogFileDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val logger: WooPosLogWrapper,
    private val preferencesRepository: WooPosPreferencesRepository,
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadCatalogFile(
        fileUrl: String,
        localSiteId: LocalOrRemoteId.LocalId,
    ): Result<File> = withContext(dispatchers.io) {
        val file =
            File(context.cacheDir, "${FILE_NAME_PREFIX}_site_${localSiteId.value}_${System.currentTimeMillis()}.json")

        try {
            logger.d("WooPosCatalogFileDownloader: Starting catalog file download from: $fileUrl")
            logger.d("WooPosCatalogFileDownloader: Destination: ${file.absolutePath}")

            val request = Request.Builder()
                .url(fileUrl)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == HttpURLConnection.HTTP_FORBIDDEN) {
                        preferencesRepository.setLocalCatalogFileAccessBlocked(localSiteId, true)
                    }
                    val error = "Download failed with code: ${response.code}"
                    logger.e(error)
                    return@withContext Result.failure(IOException(error))
                }

                val responseBody = response.body

                val contentLength = responseBody.contentLength()
                logger.d("WooPosCatalogFileDownloader: Content length: $contentLength bytes")

                responseBody.byteStream().use { inputStream ->
                    file.outputStream().use { outputStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                        }

                        logger.d("WooPosCatalogFileDownloader: Download complete. Total bytes: $totalBytesRead")
                    }
                }
            }

            if (!file.exists() || file.length() == 0L) {
                val error = "Downloaded file is empty or doesn't exist"
                logger.e(error)
                file.delete()
                return@withContext Result.failure(IOException(error))
            }

            logger.d("WooPosCatalogFileDownloader: Catalog file downloaded successfully: ${file.absolutePath}")
            Result.success(file)
        } catch (e: IOException) {
            logger.e("Error downloading catalog file", e)
            if (file.exists()) {
                file.delete()
            }
            Result.failure(e)
        }
    }

    suspend fun cleanupOldCatalogFiles(keepLatest: File? = null) = withContext(dispatchers.io) {
        try {
            val catalogFiles = context.cacheDir.listFiles { file ->
                file.name.startsWith(FILE_NAME_PREFIX) && file.name.endsWith(".json")
            } ?: return@withContext

            catalogFiles.forEach { file ->
                if (file != keepLatest) {
                    val deleted = file.delete()
                    logger.d("WooPosCatalogFileDownloader: Deleted old catalog file: ${file.name}, success: $deleted")
                }
            }
        } catch (e: IOException) {
            logger.e("Error cleaning up old catalog files", e)
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 128 * 1024
        private const val FILE_NAME_PREFIX = "woopos_catalog_"
    }
}
