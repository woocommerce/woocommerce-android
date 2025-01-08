package com.woocommerce.android.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringExtTest {
    @Test
    fun `given string with valid size, when readableFileSize called, then returns formatted size`() {
        // Given
        val inputs = mapOf(
            "1024" to "1 kB",
            "1500000" to "1.4 MB",
            "1234567" to "1.2 MB",
            "1073741824" to "1 GB",
            "1099511627776" to "1 TB"
        )

        // When & then
        inputs.forEach { (input, expected) ->
            assertThat(input.readableFileSize()).isEqualTo(expected)
        }
    }

    @Test
    fun `given zero or negative size, when readableFileSize called, then returns zero`() {
        // Given
        val inputs = listOf("0", "-1024", "-1")

        // When & then
        inputs.forEach { input ->
            assertThat(input.readableFileSize()).isEqualTo("0")
        }
    }

    @Test
    fun `given invalid number string, when readableFileSize called, then returns zero`() {
        // Given
        val inputs = listOf("", "abc", "12.34", "1,000")

        // When & then
        inputs.forEach { input ->
            assertThat(input.readableFileSize()).isEqualTo("0")
        }
    }

    @Test
    fun `given very large number, when readableFileSize called, then returns correct unit`() {
        // Given
        val size = "1" + "0".repeat(18) // 1 quintillion bytes

        // When
        val result = size.readableFileSize()

        // Then
        assertThat(result).isEqualTo("888.2 PB")
    }
}
