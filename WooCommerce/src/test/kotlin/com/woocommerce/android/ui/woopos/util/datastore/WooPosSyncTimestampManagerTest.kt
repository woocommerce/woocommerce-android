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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WooPosSyncTimestampManagerTest {
    private val repository: WooPosSyncTimestampRepository = mock()
    private val logger: WooPosLogWrapper = mock()

    private lateinit var manager: WooPosSyncTimestampManager
    private lateinit var gmtDateFormat: SimpleDateFormat

    @Before
    fun setup() {
        manager = WooPosSyncTimestampManager(repository, logger)
        gmtDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    @Test
    fun `given date object, when storing products timestamp, then formatted GMT string is stored`() {
        runTest {
            // Given
            val date: Date = gmtDateFormat.parse("2024-01-15T10:30:00")!!

            // When
            manager.storeProductsLastSyncTimestamp(date)

            // Then
            verify(repository).storeProductsLastSyncTimestamp("2024-01-15T10:30:00")
        }
    }

    @Test
    fun `given valid timestamp string in repository, when getting products timestamp, then date object is returned`() {
        runTest {
            // Given
            val timestampString = "2024-01-15T10:30:00"
            val expectedDate = gmtDateFormat.parse(timestampString)
            whenever(repository.getProductsLastSyncTimestamp()).thenReturn(timestampString)

            // When
            val result = manager.getProductsLastSyncTimestamp()

            // Then
            assertThat(result).isEqualTo(expectedDate)
        }
    }

    @Test
    fun `given no timestamp in repository, when getting products timestamp, then null is returned`() {
        runTest {
            // Given
            whenever(repository.getProductsLastSyncTimestamp()).thenReturn(null)

            // When
            val result = manager.getProductsLastSyncTimestamp()

            // Then
            assertThat(result).isNull()
        }
    }

    @Test
    fun `given invalid timestamp string in repository, when getting products timestamp, then null is returned and error logged`() {
        runTest {
            // Given
            val invalidTimestamp = "not-a-valid-timestamp"
            whenever(repository.getProductsLastSyncTimestamp()).thenReturn(invalidTimestamp)

            // When
            val result = manager.getProductsLastSyncTimestamp()

            // Then
            assertThat(result).isNull()
            verify(logger).e(eq("Failed to parse GMT timestamp: 'not-a-valid-timestamp'"), any())
        }
    }

    @Test
    fun `when clearing products timestamp, then repository clear method is called`() {
        runTest {
            // When
            manager.clearProductsLastSyncTimestamp()

            // Then
            verify(repository).clearProductsLastSyncTimestamp()
        }
    }

    @Test
    fun `given date object, when storing variations timestamp, then formatted GMT string is stored`() {
        runTest {
            // Given
            val date = gmtDateFormat.parse("2024-01-15T11:45:00")!!

            // When
            manager.storeVariationsLastSyncTimestamp(date)

            // Then
            verify(repository).storeVariationsLastSyncTimestamp("2024-01-15T11:45:00")
        }
    }

    @Test
    fun `given valid timestamp string in repository, when getting variations timestamp, then date object is returned`() {
        runTest {
            // Given
            val timestampString = "2024-01-15T11:45:00"
            val expectedDate = gmtDateFormat.parse(timestampString)
            whenever(repository.getVariationsLastSyncTimestamp()).thenReturn(timestampString)

            // When
            val result = manager.getVariationsLastSyncTimestamp()

            // Then
            assertThat(result).isEqualTo(expectedDate)
        }
    }

    @Test
    fun `given no timestamp in repository, when getting variations timestamp, then null is returned`() {
        runTest {
            // Given
            whenever(repository.getVariationsLastSyncTimestamp()).thenReturn(null)

            // When
            val result = manager.getVariationsLastSyncTimestamp()

            // Then
            assertThat(result).isNull()
        }
    }

    @Test
    fun `given invalid timestamp string in repository, when getting variations timestamp, then null is returned and error logged`() {
        runTest {
            // Given
            val invalidTimestamp = "invalid-date-format"
            whenever(repository.getVariationsLastSyncTimestamp()).thenReturn(invalidTimestamp)

            // When
            val result = manager.getVariationsLastSyncTimestamp()

            // Then
            assertThat(result).isNull()
            verify(logger).e(eq("Failed to parse GMT timestamp: 'invalid-date-format'"), any())
        }
    }

    @Test
    fun `when clearing variations timestamp, then repository clear method is called`() {
        runTest {
            // When
            manager.clearVariationsLastSyncTimestamp()

            // Then
            verify(repository).clearVariationsLastSyncTimestamp()
        }
    }

    @Test
    fun `when clearing all timestamps, then repository clear all method is called`() {
        runTest {
            // When
            manager.clearAllSyncTimestamps()

            // Then
            verify(repository).clearAllSyncTimestamps()
        }
    }

    @Test
    fun `given date object, when formatting for API, then correct GMT string is returned`() {
        // Given
        val date = gmtDateFormat.parse("2024-01-15T14:30:00")!!

        // When
        val result = manager.formatTimestampForApi(date)

        // Then
        assertThat(result).isEqualTo("2024-01-15T14:30:00")
    }

    @Test
    fun `given valid timestamp string, when parsing from API, then correct date object is returned`() {
        // Given
        val timestampString = "2024-01-15T14:30:00"
        val expectedDate = gmtDateFormat.parse(timestampString)

        // When
        val result = manager.parseTimestampFromApi(timestampString)

        // Then
        assertThat(result).isEqualTo(expectedDate)
    }

    @Test
    fun `given invalid timestamp string, when parsing from API, then null is returned and error logged`() {
        // Given
        val invalidTimestamp = "2024-15-45T25:70:90"

        // When
        val result = manager.parseTimestampFromApi(invalidTimestamp)

        // Then
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: '2024-15-45T25:70:90'"), any())
    }

    @Test
    fun `given timestamp with milliseconds, when parsing, then parsing handles format correctly`() {
        // Given
        val timestampWithoutMillis = "2024-01-15T10:30:00"
        val expectedDate = gmtDateFormat.parse(timestampWithoutMillis)

        // When
        val result = manager.parseTimestampFromApi(timestampWithoutMillis)

        // Then
        assertThat(result).isEqualTo(expectedDate)
    }

    @Test
    fun `given empty string timestamp, when parsing from API, then null is returned and error logged`() {
        // Given
        val emptyTimestamp = ""

        // When
        val result = manager.parseTimestampFromApi(emptyTimestamp)

        // Then
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: ''"), any())
    }

    @Test
    fun `given whitespace timestamp, when parsing from API, then null is returned and error logged`() {
        // Given
        val whitespaceTimestamp = "   "

        // When
        val result = manager.parseTimestampFromApi(whitespaceTimestamp)

        // Then
        assertThat(result).isNull()
        verify(logger).e(eq("Failed to parse GMT timestamp: '   '"), any())
    }
}
