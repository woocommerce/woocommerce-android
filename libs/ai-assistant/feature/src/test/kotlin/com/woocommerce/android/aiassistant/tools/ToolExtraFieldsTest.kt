package com.woocommerce.android.aiassistant.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ToolExtraFieldsTest {
    @Test
    fun `given known argument keys, when validating, then success is returned`() {
        val result = validateAllowedArguments(
            args = buildJsonObject {
                put("id", 12)
            },
            allowed = setOf("id"),
            toolName = "orders_get",
        )

        assertThat(result.isSuccess).isEqualTo(true)
    }

    @Test
    fun `given unknown argument key, when validating, then failure names unsupported key`() {
        val result = validateAllowedArguments(
            args = buildJsonObject {
                put("id", 12)
                put("unexpected", true)
            },
            allowed = setOf("id"),
            toolName = "orders_get",
        )

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Unsupported orders_get argument(s): unexpected")
    }
}
