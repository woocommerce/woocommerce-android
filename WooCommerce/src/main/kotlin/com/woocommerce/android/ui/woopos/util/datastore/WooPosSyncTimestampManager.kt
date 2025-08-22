package com.woocommerce.android.ui.woopos.util.datastore

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosSyncTimestampManager @Inject constructor(
    private val timestampRepository: WooPosSyncTimestampRepository,
    private val logger: WooPosLogWrapper
) {
    private val gmtDateFormat = SimpleDateFormat(GMT_DATE_FORMAT, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    suspend fun storeProductsLastSyncTimestamp(timestamp: Date) {
        val timestampGmt: String = gmtDateFormat.format(timestamp)
        timestampRepository.storeProductsLastSyncTimestamp(timestampGmt)
    }

    suspend fun getProductsLastSyncTimestamp(): Date? {
        val timestampString = timestampRepository.getProductsLastSyncTimestamp()
        return timestampString?.let { parseGmtTimestamp(it) }
    }

    suspend fun clearProductsLastSyncTimestamp() {
        timestampRepository.clearProductsLastSyncTimestamp()
    }

    suspend fun storeVariationsLastSyncTimestamp(timestamp: Date) {
        val timestampGmt: String = gmtDateFormat.format(timestamp)
        timestampRepository.storeVariationsLastSyncTimestamp(timestampGmt)
    }

    suspend fun getVariationsLastSyncTimestamp(): Date? {
        val timestampString = timestampRepository.getVariationsLastSyncTimestamp()
        return timestampString?.let { parseGmtTimestamp(it) }
    }

    suspend fun clearVariationsLastSyncTimestamp() {
        timestampRepository.clearVariationsLastSyncTimestamp()
    }

    suspend fun clearAllSyncTimestamps() {
        timestampRepository.clearAllSyncTimestamps()
    }

    fun formatTimestampForApi(timestamp: Date): String {
        return gmtDateFormat.format(timestamp)
    }

    fun parseTimestampFromApi(timestampString: String): Date? {
        return parseGmtTimestamp(timestampString)
    }

    private fun parseGmtTimestamp(timestampString: String): Date? {
        return try {
            gmtDateFormat.parse(timestampString)
        } catch (e: ParseException) {
            logger.e("Failed to parse GMT timestamp: '$timestampString'", e)
            null
        }
    }

    private companion object {
        const val GMT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    }
}
