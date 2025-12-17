package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosGenerateCatalogResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosGenerateCatalogState
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject
import kotlin.math.pow

class WooPosFileBasedSyncAction @Inject constructor(
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val logger: WooPosLogWrapper,
) {
    companion object {
        private const val INITIAL_POLL_INTERVAL_MS = 3000L
        private const val MAX_POLL_ATTEMPTS = 20
        private const val MAX_CONSECUTIVE_FAILED_ATTEMPTS = 3

        private const val MAX_POLL_INTERVAL_MS = 30_000L
        private const val BACKOFF_MULTIPLIER = 1.3
    }

    @Suppress("ReturnCount")
    suspend fun syncCatalog(
        site: SiteModel
    ): Result<FileBasedSyncResult> {
        logger.d("Starting file-based catalog generation for site ${site.id}")

        var attemptCount = 0
        var failedConsecutiveAttempts = 0

        while (attemptCount < MAX_POLL_ATTEMPTS) {
            attemptCount++

            if (attemptCount > 1) {
                val delayMs = computeBackoffDelay(attemptCount)
                logger.d("Waiting ${delayMs}ms before poll attempt $attemptCount")
                delay(delayMs)
            }

            val response = posLocalCatalogStore.generateCatalog(site)

            if (response.isFailure) {
                if (++failedConsecutiveAttempts >= MAX_CONSECUTIVE_FAILED_ATTEMPTS) {
                    return Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
                } else {
                    logger.w("Poll attempt $attemptCount failed: ${response.exceptionOrNull()?.message}")
                    continue
                }
            }
            failedConsecutiveAttempts = 0

            val result = response.getOrThrow()
            logger.d(
                "Poll attempt $attemptCount"
            )

            val processedResult = processPollingResult(result)
            if (processedResult != null) {
                return processedResult
            }
        }

        logger.e("Catalog generation timed out after $MAX_POLL_ATTEMPTS attempts")
        return Result.failure(Exception("Catalog generation timed out"))
    }

    data class FileBasedSyncResult(
        val fileUrl: String,
        val productFields: List<String>?,
        val variationFields: List<String>?,
        val totalProducts: Int?,
        val completedAt: String?
    )

    @Suppress("ReturnCount")
    private fun processPollingResult(result: WooPosGenerateCatalogResult): Result<FileBasedSyncResult>? {
        return when (result.state) {
            WooPosGenerateCatalogState.COMPLETED -> {
                val url = result.url
                if (url != null) {
                    logger.d("Catalog available.")
                    // TBD Download the file or scheduled bg download job
                    Result.success(createFileBasedSyncResult(result, url))
                } else {
                    logger.e("Catalog generation completed but URL is missing")
                    Result.failure(Exception("Catalog generation completed but URL is missing"))
                }
            }
            else -> null.also { logger.d("State: ${result.state}, Progress: ${result.progress}/${result.total}") }
        }
    }

    private fun createFileBasedSyncResult(
        result: WooPosGenerateCatalogResult,
        fileUrl: String
    ): FileBasedSyncResult {
        return FileBasedSyncResult(
            fileUrl = fileUrl,
            productFields = result.productFields,
            variationFields = result.variationFields,
            totalProducts = result.total,
            completedAt = result.completedAt
        )
    }

    private fun computeBackoffDelay(attemptCount: Int): Long {
        val exponent = (attemptCount - 2).coerceAtLeast(0)
        val raw = INITIAL_POLL_INTERVAL_MS * BACKOFF_MULTIPLIER.pow(exponent.toDouble())
        val finalDelay = raw.coerceAtMost(MAX_POLL_INTERVAL_MS.toDouble())
        return finalDelay.toLong()
    }
}
