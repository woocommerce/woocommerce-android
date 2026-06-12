package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WooPosCatalogFileDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val logger: WooPosLogWrapper,
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
                    logger.e("Download failed with code: ${response.code}")
                    return@withContext if (response.code == HTTP_FORBIDDEN) {
                        Result.failure(WooPosCatalogFileBlockedException())
                    } else {
                        Result.failure(IOException("Download failed with code: ${response.code}"))
                    }
                }

                val contentType = response.header("Content-Type")
                if (contentType?.contains("text/html", ignoreCase = true) == true) {
                    logger.e("WooPosCatalogFileDownloader: Catalog response is HTML (Content-Type=$contentType)")
                    return@withContext Result.failure(WooPosCatalogFileBlockedException())
                }

                streamResponseToFile(response.body, file)

                if (file.startsWithHtmlMarker()) {
                    logger.e("WooPosCatalogFileDownloader: Catalog file is HTML, not JSON")
                    file.delete()
                    return@withContext Result.failure(WooPosCatalogFileBlockedException())
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

    private fun streamResponseToFile(responseBody: ResponseBody, file: File) {
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
        private const val HTTP_FORBIDDEN = 403
    }
}

/**
 * Thrown when the host blocks the generated catalog file — an HTTP 403, or an HTML body served instead
 * of JSON (a .htaccess / login-wall / error page).
 **/
class WooPosCatalogFileBlockedException : Exception(
    "Catalog file blocked by the host (HTTP 403 or an HTML response instead of JSON)"
)

/**
 * Our catalog file is a JSON array, so it starts with `[`. An HTML error page starts with `<`. Returns
 * `true` when the first non-whitespace character is `<`, i.e. the server returned HTML, not our JSON.
 */
internal fun File.startsWithHtmlMarker(): Boolean =
    bufferedReader().use { reader ->
        var ch = reader.read()
        while (ch != -1 && ch.toChar().isWhitespace()) {
            ch = reader.read()
        }
        ch != -1 && ch.toChar() == '<'
    }
