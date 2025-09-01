package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.network.rest.Header
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@RunWith(MockitoJUnitRunner::class)
class HeadersParserTest {
    private val parser = HeadersParser(mock())

    @Test
    fun `when total pages header is present, then returns correct value`() {
        // GIVEN
        val headers = listOf(
            Header("x-wp-totalpages", "5"),
            Header("content-type", "application/json")
        )
        val result = WooResult("test", headers)

        // WHEN
        val totalPages = parser.getTotalPages(result)

        // THEN
        assertThat(totalPages).isEqualTo(5)
    }

    @Test
    fun `when total pages header has different case, then returns correct value`() {
        // GIVEN
        val headers = listOf(
            Header("X-WP-TOTALPAGES", "3"),
            Header("content-type", "application/json")
        )
        val result = WooResult("test", headers)

        // WHEN
        val totalPages = parser.getTotalPages(result)

        // THEN
        assertThat(totalPages).isEqualTo(3)
    }

    @Test
    fun `when total pages header is missing, then returns null`() {
        // GIVEN
        val headers = listOf(
            Header("content-type", "application/json"),
            Header("cache-control", "no-cache")
        )
        val result = WooResult("test", headers)

        // WHEN
        val totalPages = parser.getTotalPages(result)

        // THEN
        assertThat(totalPages).isNull()
    }

    @Test
    fun `when multiple total pages headers exist, then returns last one`() {
        // GIVEN
        val headers = listOf(
            Header("x-wp-totalpages", "2"),
            Header("x-wp-totalpages", "7")
        )
        val result = WooResult("test", headers)

        // WHEN
        val totalPages = parser.getTotalPages(result)

        // THEN
        assertThat(totalPages).isEqualTo(7)
    }

    @Test
    fun `when server date header is present, then returns correct value`() {
        // GIVEN
        val expectedDate = "Wed, 21 Oct 2024 07:28:00 GMT"
        val headers = listOf(
            Header("date", expectedDate),
            Header("content-type", "application/json")
        )
        val result = WooResult("test", headers)

        // WHEN
        val serverDate = parser.getServerDate(result)

        // THEN
        assertThat(serverDate).isEqualTo(expectedDate)
    }

    @Test
    fun `when server date header has different case, then returns correct value`() {
        // GIVEN
        val expectedDate = "Wed, 21 Oct 2024 07:28:00 GMT"
        val headers = listOf(
            Header("DATE", expectedDate),
            Header("content-type", "application/json")
        )
        val result = WooResult("test", headers)

        // WHEN
        val serverDate = parser.getServerDate(result)

        // THEN
        assertThat(serverDate).isEqualTo(expectedDate)
    }

    @Test
    fun `when server date header is missing, then returns null`() {
        // GIVEN
        val headers = listOf(
            Header("content-type", "application/json"),
            Header("cache-control", "no-cache")
        )
        val result = WooResult("test", headers)

        // WHEN
        val serverDate = parser.getServerDate(result)

        // THEN
        assertThat(serverDate).isNull()
    }

    @Test
    fun `when multiple date headers exist, then returns last one`() {
        // GIVEN
        val oldDate = "Wed, 20 Oct 2024 07:28:00 GMT"
        val newDate = "Wed, 21 Oct 2024 07:28:00 GMT"
        val headers = listOf(
            Header("date", oldDate),
            Header("date", newDate)
        )
        val result = WooResult("test", headers)

        // WHEN
        val serverDate = parser.getServerDate(result)

        // THEN
        assertThat(serverDate).isEqualTo(newDate)
    }

    @Test
    fun `when headers list is empty, then both methods return null`() {
        // GIVEN
        val result = WooResult("test", emptyList())

        // WHEN
        val totalPages = parser.getTotalPages(result)
        val serverDate = parser.getServerDate(result)

        // THEN
        assertThat(totalPages).isNull()
        assertThat(serverDate).isNull()
    }

    @Test
    fun `when total pages header has invalid value, then returns null`() {
        // GIVEN
        val headers = listOf(
            Header("x-wp-totalpages", "not-a-number")
        )
        val result = WooResult("test", headers)

        // WHEN
        val totalPages = parser.getTotalPages(result)

        // THEN
        assertThat(totalPages).isNull()
    }
}
