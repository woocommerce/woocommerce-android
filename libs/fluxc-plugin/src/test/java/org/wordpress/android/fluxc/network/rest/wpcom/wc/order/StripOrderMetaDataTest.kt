package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.metadata.WCMetaData

class StripOrderMetaDataTest {
    private lateinit var sut: StripOrderMetaData

    @Before
    fun setUp() {
        sut = StripOrderMetaData()
    }

    @Test
    fun `when metadata contains internal keys, should remove all of them`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1,
                key = "_internal key",
                value = "internal value"
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = "valid value"
            )
        )

        // When
        val result: List<WCMetaData> = sut(rawMetadata)

        // Then
        assertThat(result).isEqualTo(
            listOf(
                WCMetaData(
                    id = 2L,
                    key = "valid key",
                    value = "valid value"
                )
            )
        )
    }

    @Test
    fun `when Metadata value contains JSON and is displayable, then keep it in the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 1L,
                key = "key",
                value = "{\"key\":\"value\"}"
            ),
            WCMetaData(
                id = 2,
                key = "valid key",
                value = "valid value"
            )
        )

        // When
        val result = sut(rawMetadata)

        // Then
        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `when Metadata key is invalid, then remove it from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(
                id = 2L,
                key = "_internal key",
                value = "valid value"
            ),
            WCMetaData(
                id = 3L,
                key = "valid key",
                value = "valid value"
            )
        )

        // When
        val result = sut(rawMetadata)

        // Then
        assertThat(result).isEqualTo(
            listOf(
                WCMetaData(
                    id = 3L,
                    key = "valid key",
                    value = "valid value"
                )
            )
        )
    }

    @Test
    fun `when metadata contains additional checkout field keys, then keep them in the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(id = 1L, key = "_wc_billing/my-plugin/gov-id", value = "B-123"),
            WCMetaData(id = 2L, key = "_wc_shipping/my-plugin/gov-id", value = "S-456"),
            WCMetaData(id = 3L, key = "_wc_other/my-plugin/gift-message", value = "Happy birthday")
        )

        // When
        val result = sut(rawMetadata)

        // Then
        assertThat(result).isEqualTo(rawMetadata)
    }

    @Test
    fun `when metadata contains other internal wc keys, then remove them from the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(id = 1L, key = "_wc_foo/my-plugin/field", value = "value"),
            WCMetaData(id = 2L, key = "_wc_billing", value = "value"),
            WCMetaData(id = 3L, key = "_wc_other/my-plugin/field", value = "value")
        )

        // When
        val result = sut(rawMetadata)

        // Then
        assertThat(result.map { it.key }).containsExactly("_wc_other/my-plugin/field")
    }

    @Test
    fun `when metadata contains order attribution keys, then keep them in the list`() {
        // Given
        val rawMetadata = listOf(
            WCMetaData(id = 1L, key = "_wc_order_attribution_source_type", value = "utm")
        )

        // When
        val result = sut(rawMetadata)

        // Then
        assertThat(result).isEqualTo(rawMetadata)
    }
}
