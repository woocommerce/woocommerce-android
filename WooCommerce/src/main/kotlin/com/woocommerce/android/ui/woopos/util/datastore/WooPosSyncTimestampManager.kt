package com.woocommerce.android.ui.woopos.util.datastore

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

class WooPosSyncTimestampManager @Inject constructor(
    private val timestampRepository: WooPosSyncTimestampRepository,
    private val logger: WooPosLogWrapper
) {
    private val gmtFormatter = DateTimeFormatter.ofPattern(GMT_DATE_FORMAT, Locale.US).withZone(ZoneOffset.UTC)

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

    fun formatTimestampForApi(timestamp: Long): String {
        return gmtFormatter.format(Instant.ofEpochMilli(timestamp))
    }

    fun parseTimestampFromApi(dateFromApi: String): Long? {
        return parseGmtTimestamp(dateFromApi)
    }

    private fun parseGmtTimestamp(dateFromApi: String): Long? {
        return try {
            val localDateTime = LocalDateTime.parse(dateFromApi, gmtFormatter)
            return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (e: DateTimeParseException) {
            logger.e("Failed to parse GMT timestamp: '$dateFromApi'", e)
            null
        }
    }

    private companion object {
        const val GMT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    }
}
