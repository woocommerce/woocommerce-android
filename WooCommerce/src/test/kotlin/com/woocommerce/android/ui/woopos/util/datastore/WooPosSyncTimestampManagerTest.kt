package com.woocommerce.android.ui.woopos.util.datastore

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class WooPosSyncTimestampManagerTest {
    private val repository: WooPosSyncTimestampRepository = mock()
    private val logger: WooPosLogWrapper = mock()

    private lateinit var manager: WooPosSyncTimestampManager
    private val gmtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US).withZone(ZoneOffset.UTC)

    @Before
    fun setup() {
        manager = WooPosSyncTimestampManager(repository, logger)
    }

    @Test
    fun `given timestamp in millis, when storing products timestamp, then timestamp is stored`() {
        runTest {
            // GIVEN
            val timestamp = LocalDateTime.parse("2024-01-15T10:30:00", gmtFormatter)
                .toInstant(ZoneOffset.UTC).toEpochMilli()

            // WHEN
            manager.storeProductsLastSyncTimestamp(timestamp)

            // THEN
            verify(repository).storeProductsLastSyncTimestamp(timestamp)
        }
    }

    @Test
    fun `given valid timestamp in repository, when getting products timestamp, then timestamp is returned`() {
        runTest {
            // GIVEN
            val expectedTimestamp = LocalDateTime.parse("2024-01-15T10:30:00", gmtFormatter)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
            whenever(repository.getProductsLastSyncTimestamp()).thenReturn(expectedTimestamp)

            // WHEN
            val result = manager.getProductsLastSyncTimestamp()

            // THEN
            assertThat(result).isEqualTo(expectedTimestamp)
        }
    }

    @Test
    fun `given no timestamp in repository, when getting products timestamp, then null is returned`() {
        runTest {
            // GIVEN
            whenever(repository.getProductsLastSyncTimestamp()).thenReturn(null)

            // WHEN
            val result = manager.getProductsLastSyncTimestamp()

            // THEN
            assertThat(result).isNull()
        }
    }

    @Test
    fun `when clearing products timestamp, then repository clear method is called`() {
        runTest {
            // WHEN
            manager.clearProductsLastSyncTimestamp()

            // THEN
            verify(repository).clearProductsLastSyncTimestamp()
        }
    }

    @Test
    fun `given timestamp in millis, when storing variations timestamp, then timestamp is stored`() {
        runTest {
            // GIVEN
            val timestamp = LocalDateTime.parse("2024-01-15T11:45:00", gmtFormatter)
                .toInstant(ZoneOffset.UTC).toEpochMilli()

            // WHEN
            manager.storeVariationsLastSyncTimestamp(timestamp)

            // THEN
            verify(repository).storeVariationsLastSyncTimestamp(timestamp)
        }
    }

    @Test
    fun `given valid timestamp in repository, when getting variations timestamp, then timestamp is returned`() {
        runTest {
            // GIVEN
            val expectedTimestamp = LocalDateTime.parse("2024-01-15T11:45:00", gmtFormatter)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
            whenever(repository.getVariationsLastSyncTimestamp()).thenReturn(expectedTimestamp)

            // WHEN
            val result = manager.getVariationsLastSyncTimestamp()

            // THEN
            assertThat(result).isEqualTo(expectedTimestamp)
        }
    }

    @Test
    fun `given no timestamp in repository, when getting variations timestamp, then null is returned`() {
        runTest {
            // GIVEN
            whenever(repository.getVariationsLastSyncTimestamp()).thenReturn(null)

            // WHEN
            val result = manager.getVariationsLastSyncTimestamp()

            // THEN
            assertThat(result).isNull()
        }
    }

    @Test
    fun `when clearing variations timestamp, then repository clear method is called`() {
        runTest {
            // WHEN
            manager.clearVariationsLastSyncTimestamp()

            // THEN
            verify(repository).clearVariationsLastSyncTimestamp()
        }
    }

    @Test
    fun `when clearing all timestamps, then repository clear all method is called`() {
        runTest {
            // WHEN
            manager.clearAllSyncTimestamps()

            // THEN
            verify(repository).clearAllSyncTimestamps()
        }
    }

    @Test
    fun `given timestamp in millis, when formatting for API, then correct GMT string is returned`() {
        // GIVEN
        val timestamp = LocalDateTime.parse("2024-01-15T14:30:00", gmtFormatter)
            .toInstant(ZoneOffset.UTC).toEpochMilli()

        // WHEN
        val result = manager.formatTimestampForApi(timestamp)

        // THEN
        assertThat(result).isEqualTo("2024-01-15T14:30:00")
    }

    @Test
    fun `given valid timestamp string, when parsing from API, then correct timestamp in millis is returned`() {
        // GIVEN
        val timestampString = "2024-01-15T14:30:00"
        val expectedTimestamp = LocalDateTime.parse(timestampString, gmtFormatter)
            .toInstant(ZoneOffset.UTC).toEpochMilli()

        // WHEN
        val result = manager.parseTimestampFromApi(timestampString)

        // THEN
        assertThat(result).isEqualTo(expectedTimestamp)
    }

    @Test
    fun `given invalid timestamp string, when parsing from API, then null is returned and error logged`() {
        // GIVEN
        val invalidTimestamp = "2024-15-45T25:70:90"

        // WHEN
        val result = manager.parseTimestampFromApi(invalidTimestamp)

        // THEN
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: '2024-15-45T25:70:90'"), any())
    }

    @Test
    fun `given timestamp without milliseconds, when parsing, then parsing handles format correctly`() {
        // GIVEN
        val timestampWithoutMillis = "2024-01-15T10:30:00"
        val expectedTimestamp = LocalDateTime.parse(timestampWithoutMillis, gmtFormatter)
            .toInstant(ZoneOffset.UTC).toEpochMilli()

        // WHEN
        val result = manager.parseTimestampFromApi(timestampWithoutMillis)

        // THEN
        assertThat(result).isEqualTo(expectedTimestamp)
    }

    @Test
    fun `given empty string timestamp, when parsing from API, then null is returned and error logged`() {
        // GIVEN
        val emptyTimestamp = ""

        // WHEN
        val result = manager.parseTimestampFromApi(emptyTimestamp)

        // THEN
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: ''"), any())
    }

    @Test
    fun `given whitespace timestamp, when parsing from API, then null is returned and error logged`() {
        // GIVEN
        val whitespaceTimestamp = "   "

        // WHEN
        val result = manager.parseTimestampFromApi(whitespaceTimestamp)

        // THEN
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: '   '"), any())
    }

    @Test
    fun `given timestamp with timezone suffix, when parsing from API, then null is returned`() {
        // GIVEN
        val timestampWithTZ = "2024-01-15T10:30:00Z"

        // WHEN
        val result = manager.parseTimestampFromApi(timestampWithTZ)

        // THEN
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: '2024-01-15T10:30:00Z'"), any())
    }
}
