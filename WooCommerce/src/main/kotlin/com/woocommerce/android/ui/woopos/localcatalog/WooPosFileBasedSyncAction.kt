package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val analyticsTracker: WooPosAnalyticsTracker,
) {
    companion object {
        private const val INITIAL_POLL_INTERVAL_MS = 3000L
        private const val MAX_POLL_ATTEMPTS = 20
        private const val MAX_CONSECUTIVE_FAILED_ATTEMPTS = 3

        private const val MAX_POLL_INTERVAL_MS = 30_000L
        private const val BACKOFF_MULTIPLIER = 1.3
    }
    sealed class SyncState {
        data object Preparing : SyncState()
        data class Progress(val processed: Int, val total: Int) : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState?>(null)
    val syncState: StateFlow<SyncState?> = _syncState

    sealed class WooPosFileBasedSyncResult {
        data class Success(
            val result: PosLocalCatalogSyncResult.Success,
            val lastModifiedDate: String?
        ) : WooPosFileBasedSyncResult()

        data class Failure(
            val result: PosLocalCatalogSyncResult.Failure
        ) : WooPosFileBasedSyncResult()
    }

    suspend fun syncCatalog(site: SiteModel, force: Boolean = false): WooPosFileBasedSyncResult {
        _syncState.value = null
        val startTime = System.currentTimeMillis()
        logger.d("WooPosFileBasedSyncAction: Starting file-based catalog generation for site ${site.id}")

        val siteId = site.localId()
        val accumulatedPollAttempts = preferencesRepository.getAndClearFileBasedSyncPollAttempts(siteId)
        var lastGenerationState: WooPosGenerateCatalogState? = null
        var failedConsecutiveAttempts = 0
        var pollsSinceLastStateChange = 0
        var totalAttempts = 0
        var forceGeneration = force

        while (pollsSinceLastStateChange < MAX_POLL_ATTEMPTS) {
            delayBeforeNextPoll(totalAttempts, pollsSinceLastStateChange)
            totalAttempts++

            val response = posLocalCatalogStore.generateCatalogOrGetStatus(site, force = forceGeneration)

            if (response.isFailure) {
                if (++failedConsecutiveAttempts >= MAX_CONSECUTIVE_FAILED_ATTEMPTS) {
                    val totalPollAttempts = accumulatedPollAttempts + totalAttempts
                    return handleConsecutiveFailures(
                        siteId,
                        totalPollAttempts,
                        lastGenerationState,
                        response.exceptionOrNull()
                    )
                } else {
                    logger.w("Poll attempt $totalAttempts failed: ${response.exceptionOrNull()?.message}")
                    pollsSinceLastStateChange++
                    continue
                }
            }
            failedConsecutiveAttempts = 0
            forceGeneration = false

            val result = response.getOrThrow()
            pollsSinceLastStateChange = if (result.state != lastGenerationState) 0 else pollsSinceLastStateChange + 1
            lastGenerationState = result.state
            logger.d("WooPosFileBasedSyncAction: Poll attempt $totalAttempts, state: ${result.state}")

            val totalPollAttempts = accumulatedPollAttempts + totalAttempts
            val processedResult = processPollingResult(
                result,
                site,
                startTime,
                totalPollAttempts
            )
            if (processedResult != null) {
                if (processedResult is WooPosFileBasedSyncResult.Failure) {
                    preferencesRepository.setFileBasedSyncPollAttempts(siteId, totalPollAttempts)
                    return WooPosFileBasedSyncResult.Failure(
                        processedResult.result.withTrackingData(totalPollAttempts, lastGenerationState.rawValue)
                    )
                }
                return processedResult
            }
        }

        return handleTimeout(
            siteId = siteId,
            totalPollAttempts = accumulatedPollAttempts + totalAttempts,
            lastGenerationState = lastGenerationState
        )
    }

    private suspend fun delayBeforeNextPoll(totalAttempts: Int, pollsSinceLastStateChange: Int) {
        if (totalAttempts == 0) return
        val delayMs = computeBackoffDelay(pollsSinceLastStateChange)
        logger.d("WooPosFileBasedSyncAction: Waiting ${delayMs}ms before poll attempt ${totalAttempts + 1}")
        delay(delayMs)
    }

    private suspend fun handleConsecutiveFailures(
        siteId: LocalOrRemoteId.LocalId,
        totalPollAttempts: Int,
        lastGenerationState: WooPosGenerateCatalogState?,
        error: Throwable?
    ): WooPosFileBasedSyncResult.Failure {
        preferencesRepository.setFileBasedSyncPollAttempts(siteId, totalPollAttempts)
        logger.e(
            "WooPosFileBasedSyncAction: File-based sync failed " +
                "after $MAX_CONSECUTIVE_FAILED_ATTEMPTS consecutive failures"
        )
        return WooPosFileBasedSyncResult.Failure(
            PosLocalCatalogSyncResult.Failure.NetworkError(
                error = error?.message?.takeIf { it.isNotBlank() }
                    ?: "API error during catalog sync (${error?.let { it::class.simpleName } ?: "unknown"})",
                pollAttempts = totalPollAttempts,
                lastGenerationState = lastGenerationState?.rawValue
            )
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
        logger.d(
            "WooPosFileBasedSyncAction: State: ${result.state}, Progress: ${result.progress}% " +
                "out of ${result.total} items"
        )
        return when (result.state) {
            WooPosGenerateCatalogState.COMPLETED -> {
                if (result.url != null) {
                    logger.d("WooPosFileBasedSyncAction: Catalog available, starting download.")
                    processDownloadAndStore(result, site, startTime, pollAttempts)
                } else {
                    logger.e("WooPosFileBasedSyncAction: Catalog generation completed but URL is missing")
                    WooPosFileBasedSyncResult.Failure(
                        PosLocalCatalogSyncResult.Failure.InvalidResponse(
                            error = "Catalog generation completed but download URL is missing."
                        )
                    )
                }
            }

            WooPosGenerateCatalogState.IN_PROGRESS -> {
                val processed = result.processed
                val total = result.total
                if (processed != null && total != null) {
                    _syncState.value = SyncState.Progress(processed = processed, total = total)
                }
                null
            }

            WooPosGenerateCatalogState.SCHEDULED -> {
                _syncState.value = SyncState.Preparing
                null
            }

            WooPosGenerateCatalogState.UNKNOWN -> null
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
                    if (it is WooPosCatalogFileBlockedException) {
                        PosLocalCatalogSyncResult.Failure.CatalogFileBlocked(error = it.message.orEmpty())
                    } else {
                        PosLocalCatalogSyncResult.Failure.NetworkError(
                            error = it.message?.takeIf { msg -> msg.isNotBlank() }
                                ?: "Failed to download catalog file (${it::class.simpleName})"
                        )
                    }
                )
            }

        val parsedData = catalogFileParser.parseCatalogFile(downloadedFile, site.localId())
            .onFailureLog("Failed to parse catalog file")
            .getOrElse {
                return WooPosFileBasedSyncResult.Failure(
                    PosLocalCatalogSyncResult.Failure.InvalidResponse(
                        error = it.message?.takeIf { msg -> msg.isNotBlank() }
                            ?: "Failed to parse catalog file (${it::class.simpleName})"
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
                        error = it.message?.takeIf { msg -> msg.isNotBlank() }
                            ?: "Failed to store catalog data (${it::class.simpleName})"
                    )
                )
            }

        syncFtsAndTrack(site, parsedData)

        catalogFileDownloader.cleanupOldCatalogFiles(keepLatest = downloadedFile)

        return buildSuccessResult(result, parsedData, startTime, pollAttempts)
    }

    private fun buildSuccessResult(
        result: WooPosGenerateCatalogResult,
        parsedData: WooPosCatalogFileParser.ParsedCatalogData,
        startTime: Long,
        pollAttempts: Int
    ): WooPosFileBasedSyncResult.Success {
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
            lastModifiedDate = result.scheduledAt
        )
    }

    private suspend fun syncFtsAndTrack(site: SiteModel, parsedData: WooPosCatalogFileParser.ParsedCatalogData) {
        val ftsSyncResult = syncWithFts.syncFtsForFullSync(
            siteIdString = site.localId().value.toString(),
            products = parsedData.products,
            variations = parsedData.variations
        )
        ftsSyncResult?.let {
            analyticsTracker.track(
                WooPosAnalyticsEvent.Event.FtsIndexBuilt(
                    syncType = "full",
                    indexDurationMs = it.durationMs,
                    productsIndexed = it.productsIndexed,
                )
            )
        }
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
