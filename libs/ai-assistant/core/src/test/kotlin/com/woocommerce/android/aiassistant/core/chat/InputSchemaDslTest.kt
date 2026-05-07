package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InputSchemaDslTest {
    @Test
    fun `given primitive properties, when building the schema, then the JSON shape is correct`() {
        val schema = inputSchema {
            string("search", description = "Filter by customer name")
            boolean("force", description = "Skip cache")
            array("include", itemType = "integer", description = "IDs to include")
        }
        assertThat(schema["type"]?.jsonPrimitive?.contentOrNull).isEqualTo("object")
        assertThat(schema["additionalProperties"]?.jsonPrimitive?.content).isEqualTo("false")
        assertThat(schema["required"]).isNull()

        val props = requireNotNull(schema["properties"]).jsonObject
        val search = requireNotNull(props["search"]).jsonObject
        assertThat(search["type"]?.jsonPrimitive?.contentOrNull).isEqualTo("string")
        assertThat(search["description"]?.jsonPrimitive?.contentOrNull).isEqualTo("Filter by customer name")
        assertThat(requireNotNull(props["force"]).jsonObject["type"]?.jsonPrimitive?.content).isEqualTo("boolean")

        val include = requireNotNull(props["include"]).jsonObject
        assertThat(include["type"]?.jsonPrimitive?.content).isEqualTo("array")
        assertThat(requireNotNull(include["items"]).jsonObject["type"]?.jsonPrimitive?.content).isEqualTo("integer")
    }

    @Test
    fun `given a required integer property, when building the schema, then it appears in the required array`() {
        val schema = inputSchema {
            integer("order_id", description = "Order ID", required = true)
        }
        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }
        assertThat(required).containsExactly("order_id")
    }

    @Test
    fun `given an enum property, when building the schema, then values are listed under enum`() {
        val schema = inputSchema {
            enum("status", values = listOf("pending", "completed"), description = "Status")
        }
        val status = requireNotNull(requireNotNull(schema["properties"]).jsonObject["status"]).jsonObject
        val values = requireNotNull(status["enum"]).jsonArray.map { it.jsonPrimitive.content }
        assertThat(values).containsExactly("pending", "completed")
    }

    @Test
    fun `given enum array property, when building schema, then item enum is listed`() {
        val schema = inputSchema {
            arrayEnum(
                name = "extra_fields",
                values = listOf("billing", "line_items"),
                description = "Optional compact fields."
            )
        }

        val extraFields = requireNotNull(schema["properties"]).jsonObject.getValue("extra_fields").jsonObject
        assertThat(extraFields.getValue("type").jsonPrimitive.content).isEqualTo("array")
        assertThat(extraFields.getValue("items").jsonObject.getValue("type").jsonPrimitive.content)
            .isEqualTo("string")
        assertThat(
            extraFields.getValue("items").jsonObject.getValue("enum").jsonArray.map {
                it.jsonPrimitive.content
            }
        ).containsExactly(
            "billing",
            "line_items",
        )
    }

    @Test
    fun `given a required object property, when building the schema, then nested object shape is correct`() {
        val schema = inputSchema {
            array("ids", itemType = "integer", required = true)
            objectProperty("patch", required = true) {
                string("status")
                string("customer_note")
            }
        }

        val props = requireNotNull(schema["properties"]).jsonObject
        val patch = requireNotNull(props["patch"]).jsonObject
        assertThat(patch["type"]?.jsonPrimitive?.content).isEqualTo("object")
        assertThat(patch["additionalProperties"]?.jsonPrimitive?.content).isEqualTo("false")

        val patchProps = requireNotNull(patch["properties"]).jsonObject
        assertThat(patchProps.keys).containsExactly("status", "customer_note")
        assertThat(requireNotNull(patchProps["status"]).jsonObject["type"]?.jsonPrimitive?.content).isEqualTo("string")
        assertThat(requireNotNull(patchProps["customer_note"]).jsonObject["type"]?.jsonPrimitive?.content)
            .isEqualTo("string")

        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }
        assertThat(required).containsExactly("ids", "patch")
    }
}
