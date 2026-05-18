package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GenericSchemaConfirmationPreviewProviderTest {
    private val genericProvider = GenericSchemaConfirmationPreviewProvider()

    @Test
    fun `given first-party unsafe descriptor without dedicated provider, when preview is built, then schema rows are rendered`() =
        runTest {
            val descriptor = ToolDescriptor(
                name = "first_party_simple_write",
                description = "Simple first-party unsafe write.",
                inputSchema = inputSchema {
                    boolean("enabled", description = "Whether the setting is enabled.")
                    integer("limit", description = "The maximum number of items.")
                    string("reason", description = "Reason for the change.")
                },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )
            val preview = genericProvider.buildPreview(
                context(
                    descriptor = descriptor,
                    arguments = buildJsonObject {
                        put("enabled", true)
                        put("limit", 5)
                        put("reason", "Sync stock")
                        put("ignored_extra", "not in schema")
                    },
                )
            )

            assertThat(preview.message).isEqualTo(
                ConfirmationPreviewText.Resource(
                    R.string.ai_assistant_confirmation_generic_tool_call,
                    listOf(ConfirmationPreviewText.Raw("first_party_simple_write")),
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "enabled",
                    label = ConfirmationPreviewText.Raw("Enabled"),
                    value = ConfirmationPreviewText.Raw("true"),
                ),
                ConfirmationPreviewField(
                    name = "limit",
                    label = ConfirmationPreviewText.Raw("Limit"),
                    value = ConfirmationPreviewText.Raw("5"),
                ),
                ConfirmationPreviewField(
                    name = "reason",
                    label = ConfirmationPreviewText.Raw("Reason"),
                    value = ConfirmationPreviewText.Raw("Sync stock"),
                ),
            )
        }

    @Test
    fun `given unsafe descriptor has empty or missing properties, when preview is built, then message is generic and rows are empty`() =
        runTest {
            val emptyPropertiesDescriptor = ToolDescriptor(
                name = "first_party_empty_schema_write",
                description = "Unsafe write with empty schema properties.",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {}
                },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )
            val missingPropertiesDescriptor = ToolDescriptor(
                name = "first_party_missing_properties_write",
                description = "Unsafe write with missing schema properties.",
                inputSchema = buildJsonObject { put("type", "object") },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )

            val emptyPropertiesPreview = genericProvider.buildPreview(
                context(
                    descriptor = emptyPropertiesDescriptor,
                    arguments = buildJsonObject { put("reason", "Preview") },
                )
            )
            val missingPropertiesPreview = genericProvider.buildPreview(
                context(
                    descriptor = missingPropertiesDescriptor,
                    arguments = buildJsonObject { put("reason", "Preview") },
                )
            )

            assertThat(emptyPropertiesPreview.fields).isEmpty()
            assertThat(missingPropertiesPreview.fields).isEmpty()
            assertThat(emptyPropertiesPreview.message).isEqualTo(
                ConfirmationPreviewText.Resource(
                    R.string.ai_assistant_confirmation_generic_tool_call,
                    listOf(ConfirmationPreviewText.Raw("first_party_empty_schema_write")),
                )
            )
            assertThat(missingPropertiesPreview.message).isEqualTo(
                ConfirmationPreviewText.Resource(
                    R.string.ai_assistant_confirmation_generic_tool_call,
                    listOf(ConfirmationPreviewText.Raw("first_party_missing_properties_write")),
                )
            )
        }

    @Test
    fun `given schema argument is non primitive array, when preview is built, then value is summarized`() =
        runTest {
            val descriptor = ToolDescriptor(
                name = "first_party_array_write",
                description = "Unsafe write with array payload.",
                inputSchema = inputSchema {
                    array("items", itemType = "object", description = "Items to update.")
                },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )

            val preview = genericProvider.buildPreview(
                context(
                    descriptor = descriptor,
                    arguments = buildJsonObject {
                        putJsonArray("items") {
                            addJsonObject { put("id", 1) }
                            addJsonObject { put("id", 2) }
                        }
                    },
                )
            )

            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "items",
                    label = ConfirmationPreviewText.Raw("Items"),
                    value = ConfirmationPreviewText.Raw("2 items"),
                )
            )
        }

    @Test
    fun `given unknown external unsafe descriptor, when preview is built repeatedly, then output is deterministic`() =
        runTest {
            val descriptor = externalUnsafeDescriptor()
            val arguments = buildJsonObject {
                put("warehouse_code", "A1")
                put("quantity_delta", -3)
                put("notify", false)
                putJsonObject("ignored_payload") { put("raw", true) }
            }
            val context = context(descriptor, arguments)

            val first = genericProvider.buildPreview(context)
            val second = genericProvider.buildPreview(context)

            assertThat(first).isEqualTo(second)
            assertThat(first.fields.map { it.name }).containsExactly(
                "notify",
                "quantity_delta",
                "warehouse_code",
            )
            assertThat(first.fields.map { it.label }).containsExactly(
                ConfirmationPreviewText.Raw("Notify"),
                ConfirmationPreviewText.Raw("Quantity Delta"),
                ConfirmationPreviewText.Raw("Warehouse Code"),
            )
            assertThat(first.fields.map { it.value }).containsExactly(
                ConfirmationPreviewText.Raw("false"),
                ConfirmationPreviewText.Raw("-3"),
                ConfirmationPreviewText.Raw("A1"),
            )
            assertThat(first.fields.map { (it.value as ConfirmationPreviewText.Raw).value })
                .allSatisfy { renderedValue ->
                    assertThat(renderedValue).doesNotContain("{")
                    assertThat(renderedValue).doesNotContain("}")
                }
        }

    private fun request(
        toolName: String,
        arguments: JsonObject,
    ) = ConfirmationRequest(
        id = "confirmation-$toolName",
        toolCallId = "call-$toolName",
        toolName = toolName,
        arguments = arguments,
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    private fun context(
        descriptor: ToolDescriptor,
        arguments: JsonObject,
    ) = ConfirmationPreviewContext(
        request = request(descriptor.name, arguments),
        descriptor = descriptor,
    )

    private fun externalUnsafeDescriptor(): ToolDescriptor = ToolDescriptor(
        name = "external.inventory_adjust",
        description = "External unsafe schema-based tool.",
        inputSchema = inputSchema {
            string("warehouse_code", description = "Warehouse code.")
            integer("quantity_delta", description = "Quantity delta.")
            boolean("notify", description = "Whether to notify subscribers.")
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )
}
