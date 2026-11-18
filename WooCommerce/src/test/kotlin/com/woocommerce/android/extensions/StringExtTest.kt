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

    @Test
    fun `given html, when stripHtml called, then tags are removed, entities decoded and paragraphs kept as lines`() {
        // Given
        val inputs = mapOf(
            "Plain text" to "Plain text",
            "<b>Bold</b> &amp; <i>italic</i>" to "Bold & italic",
            "<p>First</p><p>Second</p>" to "\nFirst\nSecond",
            "First<br/>Second" to "First\nSecond",
            "" to "",
        )

        // When & then
        inputs.forEach { (input, expected) ->
            assertThat(input.stripHtml()).isEqualTo(expected)
        }
    }

    @Test
    fun `given only tags, when stripHtml called, then returns an empty string`() {
        // Given
        val inputs = listOf("<p></p>", "<img src=\"a.png\" />", "<div><span></span></div>")

        // When & then
        inputs.forEach { input ->
            assertThat(input.stripHtml()).isEmpty()
        }
    }

    @Test
    fun `given only tags, when fastStripHtml called, then returns the input`() {
        // Given
        val inputs = listOf("<p></p>", "<img src=\"a.png\" />", "<div><span></span></div>")

        // When & then
        inputs.forEach { input ->
            assertThat(input.fastStripHtml()).isEqualTo(input)
        }
    }

    @Test
    fun `given text with tags, when fastStripHtml called, then returns the same as stripHtml`() {
        // Given
        val inputs = listOf("Plain text", "<b>Bold</b> &amp; <i>italic</i>", "<p>First</p><p>Second</p>", "A<br>B", "")

        // When & then
        inputs.forEach { input ->
            assertThat(input.fastStripHtml()).isEqualTo(input.stripHtml())
        }
    }
}
