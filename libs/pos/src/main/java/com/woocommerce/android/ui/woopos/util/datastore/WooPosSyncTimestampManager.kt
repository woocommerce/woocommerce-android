package com.woocommerce.android.ui.woopos.util.datastore

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor
import java.util.Locale
import javax.inject.Inject

class WooPosSyncTimestampManager @Inject constructor(
    private val timestampRepository: WooPosSyncTimestampRepository,
    private val logger: WooPosLogWrapper
) {

    suspend fun storeProductsLastSyncTimestamp(timestamp: Long) {
        timestampRepository.storeProductsLastSyncTimestamp(timestamp)
    }

    suspend fun getProductsLastSyncTimestamp(): Long? = timestampRepository.getProductsLastSyncTimestamp()

    suspend fun clearProductsLastSyncTimestamp() = timestampRepository.clearProductsLastSyncTimestamp()

    suspend fun storeVariationsLastSyncTimestamp(timestamp: Long) =
        timestampRepository.storeVariationsLastSyncTimestamp(timestamp)

    suspend fun getVariationsLastSyncTimestamp(): Long? = timestampRepository.getVariationsLastSyncTimestamp()

    suspend fun clearVariationsLastSyncTimestamp() {
        timestampRepository.clearVariationsLastSyncTimestamp()
    }

    suspend fun clearAllSyncTimestamps() {
        timestampRepository.clearAllSyncTimestamps()
    }

    suspend fun storeFullSyncLastCompletedTimestamp(timestamp: Long) {
        timestampRepository.storeFullSyncLastCompletedTimestamp(timestamp)
    }

    suspend fun getFullSyncLastCompletedTimestamp(): Long? = timestampRepository.getFullSyncLastCompletedTimestamp()

    suspend fun setCatalogFileBlocked(blocked: Boolean) {
        timestampRepository.setCatalogFileBlocked(blocked)
    }

    suspend fun isCatalogFileBlocked(): Boolean = timestampRepository.isCatalogFileBlocked()

    fun formatTimestampForApi(timestamp: Long): String {
        return defaultApiDateFormatter.format(Instant.ofEpochMilli(timestamp))
    }

    fun parseTimestampFromApi(dateFromApi: String): Long? {
        return parseGmtTimestamp(dateFromApi)
    }

    fun calculateGenerationDuration(scheduledAt: String?, completedAt: String?): Long? {
        if (scheduledAt == null || completedAt == null) return null
        val scheduledTs = parseGmtTimestamp(scheduledAt) ?: return null
        val completedTs = parseGmtTimestamp(completedAt) ?: return null
        return completedTs - scheduledTs
    }

    private fun parseGmtTimestamp(dateFromApi: String): Long? {
        for (formatter in PARSING_FORMATTERS) {
            try {
                val temporal: TemporalAccessor = formatter.parse(dateFromApi)
                return Instant.from(temporal).toEpochMilli()
            } catch (_: Exception) {
                continue
            }
        }

        logger.e(
            "Failed to parse GMT timestamp: '$dateFromApi' with any supported format",
            DateTimeParseException("None of the supported formats could parse the date", dateFromApi, 0)
        )
        return null
    }

    private companion object {
        private const val DEFAULT_API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
        private val defaultApiDateFormatter = DateTimeFormatter.ofPattern(DEFAULT_API_DATE_FORMAT, Locale.US)
            .withZone(ZoneOffset.UTC)

        private val PARSING_FORMATTERS = listOf(
            defaultApiDateFormatter,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).withZone(ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).withZone(ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).withZone(ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).withZone(ZoneOffset.UTC)
        )
    }
}
