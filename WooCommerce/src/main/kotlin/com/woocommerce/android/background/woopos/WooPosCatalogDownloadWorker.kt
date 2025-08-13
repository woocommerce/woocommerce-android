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
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.POS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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

    override suspend fun doWork(): Result {
        WooLog.i(POS, "Starting WooPOS catalog download from $POS_CATALOG_URL")

        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()

        val request = Request.Builder()
            .url(POS_CATALOG_URL)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    WooLog.e(POS, "HTTP ${'$'}{response.code} while downloading catalog")
                    return Result.retry()
                }

                val body: ResponseBody? = response.body
                if (body == null) {
                    WooLog.w(POS, "Empty response body for catalog download")
                    return Result.retry()
                }

                val outputFile = File(appContext.filesDir, "pos-catalog-poc-${System.currentTimeMillis()}")
                body.source().use { source: BufferedSource ->
                    outputFile.sink().buffer().use { fileSink ->
                        streamToLogAndFile(source, CHUNK_SIZE_BYTES, fileSink) { chunk ->
                            WooLog.i(POS, chunk)
                        }
                    }
                }

                WooLog.i(POS, "WooPOS catalog download finished successfully")
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
internal fun streamToLogAndFile(
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


