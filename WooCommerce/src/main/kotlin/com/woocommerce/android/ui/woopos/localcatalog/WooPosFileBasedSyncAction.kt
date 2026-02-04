package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.LocalOrRemoteId
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
    private val syncWithFts: WooPosLocalCatalogSyncWithFts,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val syncTimestampManager: WooPosSyncTimestampManager,
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

        val siteId = site.localId()
        val accumulatedPollAttempts = preferencesRepository.getAndClearFileBasedSyncPollAttempts(siteId)
        var lastGenerationState: WooPosGenerateCatalogState? = null
        var failedConsecutiveAttempts = 0

        repeat(MAX_POLL_ATTEMPTS) { attemptIndex ->
            if (attemptIndex > 0) {
                val delayMs = computeBackoffDelay(attemptIndex)
                logger.d("WooPosFileBasedSyncAction: Waiting ${delayMs}ms before poll attempt $attemptIndex")
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
                    logger.w("Poll attempt $attemptIndex failed: ${response.exceptionOrNull()?.message}")
                    return@repeat
                }
            }
            failedConsecutiveAttempts = 0

            val result = response.getOrThrow()
            lastGenerationState = result.state
            logger.d("WooPosFileBasedSyncAction: Poll attempt $attemptIndex, state: ${result.state}")

            val totalPollAttempts = accumulatedPollAttempts + attemptIndex + 1
            val processedResult = processPollingResult(result, site, startTime, totalPollAttempts)
            if (processedResult != null) {
                return processedResult
            }
        }

        return handleTimeout(
            siteId = siteId,
            totalPollAttempts = accumulatedPollAttempts + MAX_POLL_ATTEMPTS,
            lastGenerationState = lastGenerationState
        )
    }

    private suspend fun handleTimeout(
        siteId: LocalOrRemoteId.LocalId,
        totalPollAttempts: Int,
        lastGenerationState: WooPosGenerateCatalogState?
    ): WooPosFileBasedSyncResult.Failure {
        preferencesRepository.setFileBasedSyncPollAttempts(siteId, totalPollAttempts)

        logger.e(
            "WooPosFileBasedSyncAction: Catalog generation timed out after $totalPollAttempts total attempts. " +
                "Last state: $lastGenerationState"
        )
        return WooPosFileBasedSyncResult.Failure(
            PosLocalCatalogSyncResult.Failure.CatalogGenerationTimeout(
                error = "Catalog generation is taking longer than expected.",
                lastGenerationState = lastGenerationState?.rawValue,
                pollAttempts = totalPollAttempts
            )
        )
    }

    private suspend fun processPollingResult(
        result: WooPosGenerateCatalogResult,
        site: SiteModel,
        startTime: Long,
        pollAttempts: Int
    ): WooPosFileBasedSyncResult? {
        return when (result.state) {
            WooPosGenerateCatalogState.COMPLETED -> {
                if (result.url != null) {
                    logger.d("WooPosFileBasedSyncAction: Catalog available, starting download.")
                    processDownloadAndStore(result, site, startTime, pollAttempts)
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
        startTime: Long,
        pollAttempts: Int
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

        syncWithFts.syncFtsForFullSync(
            siteIdString = site.localId().value.toString(),
            products = parsedData.products,
            variations = parsedData.variations
        )

        catalogFileDownloader.cleanupOldCatalogFiles(keepLatest = downloadedFile)

        val syncDuration = System.currentTimeMillis() - startTime
        val generationDuration = syncTimestampManager.calculateGenerationDuration(
            scheduledAt = result.scheduledAt,
            completedAt = result.completedAt
        )

        logger.d(
            "WooPosFileBasedSyncAction: File-based sync completed successfully. " +
                "Products: ${parsedData.products.size}, Variations: ${parsedData.variations.size}. " +
                "Duration: ${syncDuration}ms. Generation: ${generationDuration}ms. Poll attempts: $pollAttempts."
        )

        return WooPosFileBasedSyncResult.Success(
            PosLocalCatalogSyncResult.Success(
                productsSynced = parsedData.products.size,
                variationsSynced = parsedData.variations.size,
                syncDurationMs = syncDuration,
                generationDurationMs = generationDuration,
                pollAttempts = pollAttempts
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
