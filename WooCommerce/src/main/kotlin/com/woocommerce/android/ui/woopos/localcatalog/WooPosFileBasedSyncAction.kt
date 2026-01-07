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
    private val catalogFileDownloader: WooPosCatalogFileDownloader,
    private val catalogFileParser: WooPosCatalogFileParser,
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

        var failedConsecutiveAttempts = 0

        repeat(MAX_POLL_ATTEMPTS) { attemptCount ->
            if (attemptCount > 0) {
                val delayMs = computeBackoffDelay(attemptCount)
                logger.d("Waiting ${delayMs}ms before poll attempt $attemptCount")
                delay(delayMs)
            }

            val response = posLocalCatalogStore.generateCatalogOrGetStatus(site)

            if (response.isFailure) {
                if (++failedConsecutiveAttempts >= MAX_CONSECUTIVE_FAILED_ATTEMPTS) {
                    return Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
                } else {
                    logger.w("Poll attempt $attemptCount failed: ${response.exceptionOrNull()?.message}")
                    return@repeat
                }
            }
            failedConsecutiveAttempts = 0

            val result = response.getOrThrow()
            logger.d(
                "Poll attempt $attemptCount"
            )

            val processedResult = processPollingResult(result, site)
            if (processedResult != null) {
                return processedResult
            }
        }

        logger.e("Catalog generation timed out after $MAX_POLL_ATTEMPTS attempts")
        return Result.failure(Exception("Catalog generation timed out"))
    }

    data class FileBasedSyncResult(
        val totalProducts: Int?,
        val completedAt: String?,
        val productsStored: Int,
        val variationsStored: Int
    )

    @Suppress("ReturnCount")
    private suspend fun processPollingResult(
        result: WooPosGenerateCatalogResult,
        site: SiteModel
    ): Result<FileBasedSyncResult>? {
        return when (result.state) {
            WooPosGenerateCatalogState.COMPLETED -> {
                val url = result.url
                if (url != null) {
                    logger.d("Catalog available, starting download.")

                    processDownloadAndStore(url, result, site)
                } else {
                    logger.e("Catalog generation completed but URL is missing")
                    Result.failure(Exception("Catalog generation completed but URL is missing"))
                }
            }
            else -> null.also {
                logger.d("State: ${result.state}, Progress: ${result.progress}% out of ${result.total} items")
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun processDownloadAndStore(
        url: String,
        catalogResult: WooPosGenerateCatalogResult,
        site: SiteModel
    ): Result<FileBasedSyncResult> {
        val downloadedFile = catalogFileDownloader.downloadCatalogFile(url, site.localId())
            .onFailureLog("Failed to download catalog file")
            .getOrElse { return Result.failure(it) }

        val parsedData = catalogFileParser.parseCatalogFile(downloadedFile, site.localId())
            .onFailureLog("Failed to parse catalog file")
            .getOrElse { return Result.failure(it) }

        posLocalCatalogStore.storeCatalogData(
            localSiteId = site.localId(),
            products = parsedData.products,
            variations = parsedData.variations
        ).onFailureLog("Failed to store catalog data")
            .getOrElse { return Result.failure(it) }

        catalogFileDownloader.cleanupOldCatalogFiles(keepLatest = downloadedFile)

        return Result.success(
            createFileBasedSyncResult(
                result = catalogResult,
                productsStored = parsedData.products.size,
                variationsStored = parsedData.variations.size
            )
        )
    }

    private fun <T> Result<T>.onFailureLog(context: String): Result<T> {
        onFailure { logger.e("$context: ${it.message}") }
        return this
    }

    private fun createFileBasedSyncResult(
        result: WooPosGenerateCatalogResult,
        productsStored: Int,
        variationsStored: Int
    ): FileBasedSyncResult {
        return FileBasedSyncResult(
            totalProducts = result.total,
            completedAt = result.completedAt,
            productsStored = productsStored,
            variationsStored = variationsStored
        )
    }

    private fun computeBackoffDelay(attemptCount: Int): Long {
        val exponent = (attemptCount - 2).coerceAtLeast(0)
        val raw = INITIAL_POLL_INTERVAL_MS * BACKOFF_MULTIPLIER.pow(exponent.toDouble())
        val finalDelay = raw.coerceAtMost(MAX_POLL_INTERVAL_MS.toDouble())
        return finalDelay.toLong()
    }
}
