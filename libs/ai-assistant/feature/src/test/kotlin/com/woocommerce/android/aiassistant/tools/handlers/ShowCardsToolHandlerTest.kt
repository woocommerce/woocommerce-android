package com.woocommerce.android.aiassistant.tools.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShowCardsToolHandlerTest {
    private val handler = ShowCardsToolHandler()

    @Test
    fun `show cards descriptor accepts references with Android v1 order and product families`() {
        val descriptor = handler.descriptor

        assertThat(descriptor.name).isEqualTo("show_cards")
        assertThat(descriptor.description).contains("order")
        assertThat(descriptor.description).contains("product")
        assertThat(descriptor.inputSchema.toString()).contains("references")
        assertThat(descriptor.inputSchema.toString()).contains("family")
        assertThat(descriptor.inputSchema.toString()).contains("id")
        assertThat(descriptor.inputSchema.toString()).contains("order")
        assertThat(descriptor.inputSchema.toString()).contains("product")
    }
}
