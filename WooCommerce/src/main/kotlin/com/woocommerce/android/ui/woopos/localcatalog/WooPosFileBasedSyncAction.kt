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
    sealed class WooPosFileBasedSyncResult {
        data class Success(
            val result: PosLocalCatalogSyncResult.Success,
            val lastModifiedDate: String?
        ) : WooPosFileBasedSyncResult()

        data class Failure(
            val result: PosLocalCatalogSyncResult.Failure
        ) : WooPosFileBasedSyncResult()
    }

    suspend fun syncCatalog(site: SiteModel): WooPosFileBasedSyncResult {
        val startTime = System.currentTimeMillis()
        logger.d("WooPosFileBasedSyncAction: Starting file-based catalog generation for site ${site.id}")

        var failedConsecutiveAttempts = 0

        repeat(MAX_POLL_ATTEMPTS) { attemptCount ->
            if (attemptCount > 0) {
                val delayMs = computeBackoffDelay(attemptCount)
                logger.d("WooPosFileBasedSyncAction: Waiting ${delayMs}ms before poll attempt $attemptCount")
                delay(delayMs)
            }

            val response = posLocalCatalogStore.generateCatalogOrGetStatus(site)

            if (response.isFailure) {
                if (++failedConsecutiveAttempts >= MAX_CONSECUTIVE_FAILED_ATTEMPTS) {
                    val error = response.exceptionOrNull()
                    logger.e(
                        "WooPosFileBasedSyncAction: File-based sync failed " +
                            "after $MAX_CONSECUTIVE_FAILED_ATTEMPTS consecutive failures"
                    )
                    return WooPosFileBasedSyncResult.Failure(
                        PosLocalCatalogSyncResult.Failure.NetworkError(
                            error?.message ?: "API error during catalog sync"
                        )
                    )
                } else {
                    logger.w("Poll attempt $attemptCount failed: ${response.exceptionOrNull()?.message}")
                    return@repeat
                }
            }
            failedConsecutiveAttempts = 0

            val result = response.getOrThrow()
            logger.d("WooPosFileBasedSyncAction: Poll attempt $attemptCount")

            val processedResult = processPollingResult(result, site, startTime = startTime)
            if (processedResult != null) {
                return processedResult
            }
        }

        logger.e("WooPosFileBasedSyncAction: Catalog generation timed out after $MAX_POLL_ATTEMPTS attempts")
        return WooPosFileBasedSyncResult.Failure(
            PosLocalCatalogSyncResult.Failure.CatalogGenerationTimeout(
                "Catalog generation is taking longer than expected."
            )
        )
    }

    private suspend fun processPollingResult(
        result: WooPosGenerateCatalogResult,
        site: SiteModel,
        startTime: Long
    ): WooPosFileBasedSyncResult? {
        return when (result.state) {
            WooPosGenerateCatalogState.COMPLETED -> {
                if (result.url != null) {
                    logger.d("WooPosFileBasedSyncAction: Catalog available, starting download.")
                    processDownloadAndStore(result, site, startTime)
                } else {
                    logger.e("WooPosFileBasedSyncAction: Catalog generation completed but URL is missing")
                    WooPosFileBasedSyncResult.Failure(
                        PosLocalCatalogSyncResult.Failure.InvalidResponse(
                            "Catalog generation completed but download URL is missing."
                        )
                    )
                }
            }

            else -> null.also {
                logger.d(
                    "WooPosFileBasedSyncAction: State: ${result.state}, Progress: ${result.progress}% " +
                        "out of ${result.total} items"
                )
            }
        }
    }

    private suspend fun processDownloadAndStore(
        result: WooPosGenerateCatalogResult,
        site: SiteModel,
        startTime: Long
    ): WooPosFileBasedSyncResult {
        val downloadedFile = catalogFileDownloader.downloadCatalogFile(result.url!!, site.localId())
            .onFailureLog("Failed to download catalog file")
            .getOrElse {
                return WooPosFileBasedSyncResult.Failure(
                    PosLocalCatalogSyncResult.Failure.NetworkError(
                        it.message ?: "Failed to download catalog file"
                    )
                )
            }

        val parsedData = catalogFileParser.parseCatalogFile(downloadedFile, site.localId())
            .onFailureLog("Failed to parse catalog file")
            .getOrElse {
                return WooPosFileBasedSyncResult.Failure(
                    PosLocalCatalogSyncResult.Failure.InvalidResponse(
                        it.message ?: "Failed to parse catalog file"
                    )
                )
            }

        posLocalCatalogStore.storeCatalogData(
            localSiteId = site.localId(),
            products = parsedData.products,
            variations = parsedData.variations
        ).onFailureLog("Failed to store catalog data")
            .getOrElse {
                return WooPosFileBasedSyncResult.Failure(
                    PosLocalCatalogSyncResult.Failure.DatabaseError(
                        it.message ?: "Failed to store catalog data"
                    )
                )
            }

        catalogFileDownloader.cleanupOldCatalogFiles(keepLatest = downloadedFile)

        val syncDuration = System.currentTimeMillis() - startTime
        logger.d(
            "WooPosFileBasedSyncAction: File-based sync completed successfully. " +
                "Products: ${parsedData.products.size}, Variations: ${parsedData.variations.size}. " +
                "Duration: ${syncDuration}ms."
        )

        return WooPosFileBasedSyncResult.Success(
            PosLocalCatalogSyncResult.Success(
                productsSynced = parsedData.products.size,
                variationsSynced = parsedData.variations.size,
                syncDurationMs = syncDuration
            ),
            // Using scheduledAt (not completedAt) to not miss changes made during generation
            lastModifiedDate = result.scheduledAt
        )
    }

    private fun <T> Result<T>.onFailureLog(context: String): Result<T> {
        onFailure { logger.e("WooPosFileBasedSyncAction: $context: ${it.message}") }
        return this
    }

    private fun computeBackoffDelay(attemptCount: Int): Long {
        val exponent = (attemptCount - 2).coerceAtLeast(0)
        val raw = INITIAL_POLL_INTERVAL_MS * BACKOFF_MULTIPLIER.pow(exponent.toDouble())
        val finalDelay = raw.coerceAtMost(MAX_POLL_INTERVAL_MS.toDouble())
        return finalDelay.toLong()
    }
}
