package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorDetailsFormatterTest : BaseUnitTest() {
    @Test
    fun `when message is provided, then all fields are included`() {
        // WHEN
        val result = formatErrorDetails(
            operation = "Site Connection",
            errorType = "INVALID_RESPONSE",
            message = "Unexpected token in JSON"
        )

        // THEN
        assertThat(result).isEqualTo(
            "Operation: Site Connection\n" +
                "Error Type: INVALID_RESPONSE\n" +
                "Description: Unexpected token in JSON"
        )
    }

    @Test
    fun `when message is null, then description line is omitted`() {
        // WHEN
        val result = formatErrorDetails(
            operation = "Fetch Orders",
            errorType = "TIMEOUT",
            message = null
        )

        // THEN
        assertThat(result).isEqualTo(
            "Operation: Fetch Orders\n" +
                "Error Type: TIMEOUT"
        )
    }

    @Test
    fun `when message is blank, then description line is omitted`() {
        // WHEN
        val result = formatErrorDetails(
            operation = "Fetch Products",
            errorType = "GENERIC_ERROR",
            message = "   "
        )

        // THEN
        assertThat(result).isEqualTo(
            "Operation: Fetch Products\n" +
                "Error Type: GENERIC_ERROR"
        )
    }
}
