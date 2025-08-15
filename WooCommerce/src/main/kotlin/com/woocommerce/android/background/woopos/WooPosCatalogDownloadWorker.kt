package com.woocommerce.android.background.woopos

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.POS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.sink

/**
 * Proof of concept. Periodic worker that downloads a large JSON file using streaming to avoid OOM, logs it and writes to file.
 */
@HiltWorker
class WooPosCatalogDownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        delay(5000)
        WooLog.i(POS, "Starting WooPOS catalog download from $POS_CATALOG_URL2")

        val client = OkHttpClient.Builder()
            .cache(null)
            .retryOnConnectionFailure(true)
            .build()

        val request = Request.Builder()
            .url(POS_CATALOG_URL2)
            .cacheControl(CacheControl.Builder().noStore().build())
            .get()
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    WooLog.e(POS, "HTTP ${'$'}{response.code} while downloading catalog")
                    return@withContext Result.retry()
                }

                val body: ResponseBody? = response.body
                if (body == null) {
                    WooLog.w(POS, "Empty response body for catalog download")
                    return@withContext Result.retry()
                }

				val timestamp = System.currentTimeMillis()
				val outputFile = File(appContext.filesDir, "pos-catalog-poc-${timestamp}")
                body.source().use { source: BufferedSource ->
                    outputFile.sink().buffer().use { fileSink ->
                        streamToFile(source, CHUNK_SIZE_BYTES, fileSink) { chunk ->
                            // WooLog.i(POS, chunk)
                        }
                    }
                }

                WooLog.i(POS, "WooPOS catalog download finished successfully")
				try {
					WooLog.i(POS, "Starting WooPOS products extraction to file")
					val productsOutFile = File(appContext.filesDir, "pos-catalog-products-${timestamp}.txt")
					deserializeAndStore(outputFile, productsOutFile)
					WooLog.i(POS, "Finished WooPOS products extraction to file: ${'$'}productsOutFile")
				} catch (t: Throwable) {
					WooLog.e(POS, "Failed to parse/write products from saved catalog", t)
				}
                Result.success()
            }
        } catch (io: IOException) {
            WooLog.e(POS, "Network error while downloading catalog", io)
            Result.retry()
        } catch (t: Throwable) {
            WooLog.e(POS, "Unexpected error while downloading catalog", t)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "WooPosCatalogDownloadWork"
        private const val ONE_TIME_WORK_NAME = "WooPosCatalogDownloadWorkOnce"
        private const val CHUNK_SIZE_BYTES = 8 * 1024 // 8 KiB
        private const val POS_CATALOG_URL = "https://poslarge.mystagingwebsite.com/wp-content/uploads/pos-catalog.json"
        private const val POS_CATALOG_URL2 = "https://github.com/szalony9szymek/large/releases/download/free/large"

        fun schedule(applicationContext: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WooPosCatalogDownloadWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun runNow(applicationContext: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<WooPosCatalogDownloadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

/**
 * Streams data from the given [source] in fixed-size chunks and delivers each chunk to [onChunk].
 * This avoids loading the entire content in memory.
 */
internal fun streamToFile(
    source: BufferedSource,
    chunkSizeBytes: Int,
    fileSink: okio.BufferedSink,
    onChunk: (String) -> Unit
) {
    require(chunkSizeBytes > 0) { "chunkSizeBytes must be > 0" }
    val byteArray = ByteArray(chunkSizeBytes)
    while (true) {
        val read = source.read(byteArray)
        if (read == -1) break
        // Write bytes to file
        fileSink.write(byteArray, 0, read)
        // Log chunk as UTF-8 text
        val chunk = String(byteArray, 0, read, Charsets.UTF_8)
        if (chunk.isNotEmpty()) {
            onChunk(chunk)
        }
    }
    fileSink.flush()
}

private data class CatalogProduct(
    val id: Long?,
    val name: String?
)

private fun deserializeAndStore(sourceFile: File, outFile: File) {
    sourceFile.inputStream().buffered().reader(Charsets.UTF_8).use { isr ->
        outFile.sink().buffer().use { sink ->
            val reader = JsonReader(isr)
            val gson = Gson()
            sink.writeUtf8("id\tname\n")
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "products") {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val product = gson.fromJson<CatalogProduct>(reader, CatalogProduct::class.java)
                        if (product.id != null) {
                            val safeName = product.name?.replace("\n", " ") ?: ""
                            sink.writeUtf8("${product.id}\t$safeName\n")
                        }
                    }
                    break
                } else {
                    reader.skipValue()
                }
            }
            reader.close()
            sink.flush()
        }
    }
}
