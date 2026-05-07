package com.woocommerce.android.aiassistant.tools

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ToolExtraFieldsTest {
    @Test
    fun `given known argument keys, when validating, then success is returned`() {
        val result = validateAllowedArguments(
            args = buildJsonObject {
                put("id", 12)
                putJsonArray("extra_fields") { add("billing") }
            },
            allowed = setOf("id", "extra_fields"),
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
            allowed = setOf("id", "extra_fields"),
            toolName = "orders_get",
        )

        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Unsupported orders_get argument(s): unexpected")
    }

    @Test
    fun `given absent extra fields, when parsing, then empty set is returned`() {
        val result = parseExtraFields(
            args = buildJsonObject { },
            allowed = setOf("billing"),
            toolName = "orders_list",
        )

        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `given allowed extra fields, when parsing, then normalized set is returned`() {
        val result = parseExtraFields(
            args = buildJsonObject {
                putJsonArray("extra_fields") {
                    add("billing")
                    add("line_items")
                    add("billing")
                }
            },
            allowed = setOf("billing", "line_items"),
            toolName = "orders_list",
        )

        assertThat(result.getOrThrow()).containsExactly("billing", "line_items")
    }

    @Test
    fun `given unknown extra field, when parsing, then validation failure names unsupported value`() {
        val result = parseExtraFields(
            args = buildJsonObject {
                putJsonArray("extra_fields") {
                    add("billing")
                    add("metadata")
                }
            },
            allowed = setOf("billing", "line_items"),
            toolName = "orders_list",
        )

        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "Unsupported orders_list extra_fields: metadata. Allowed values: billing, line_items"
        )
    }
}
