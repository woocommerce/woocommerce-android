package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConfirmationPreviewProviderRegistryTest {
    private val genericProvider = GenericSchemaConfirmationPreviewProvider()

    private val importantUnsafeToolNames = setOf(
        "orders_update",
        "orders_bulk_update",
        "products_update",
        "products_bulk_update",
        "product_variations_update",
    )

    @Test
    fun `given dedicated and generic providers support a request, when resolving provider, then highest priority provider wins`() =
        runTest {
            val dedicatedProvider = RecordingProvider(
                key = "dedicated",
                priority = 100,
                supportedNames = setOf("orders_update"),
            )
            val genericProvider = RecordingProvider(
                key = "generic_schema",
                priority = 0,
                supportedNames = setOf("orders_update"),
            )
            val registry = DefaultConfirmationPreviewProviderRegistry(setOf(genericProvider, dedicatedProvider))

            val provider = registry.providerFor(context(descriptor("orders_update")))

            assertThat(provider.key).isEqualTo("dedicated")
        }

    @Test
    fun `given equal priority providers, when resolving provider, then key order makes selection deterministic`() =
        runTest {
            val zProvider = RecordingProvider(
                key = "z_provider",
                priority = 10,
                supportedNames = setOf("orders_update"),
            )
            val aProvider = RecordingProvider(
                key = "a_provider",
                priority = 10,
                supportedNames = setOf("orders_update"),
            )
            val registry = DefaultConfirmationPreviewProviderRegistry(setOf(zProvider, aProvider))

            val provider = registry.providerFor(context(descriptor("orders_update")))

            assertThat(provider.key).isEqualTo("a_provider")
        }

    @Test
    fun `given registry builds preview, when provider is selected, then selected provider receives request and descriptor`() =
        runTest {
            val selectedProvider = RecordingProvider(
                key = "selected",
                priority = 100,
                supportedNames = setOf("products_update"),
                previewMessage = "selected preview",
            )
            val registry = DefaultConfirmationPreviewProviderRegistry(setOf(genericProvider, selectedProvider))
            val descriptor = descriptor("products_update")
            val context = context(descriptor, buildJsonObject { put("reason", "Preview") })

            val preview = registry.buildPreview(context)

            assertThat(preview.message).isEqualTo(ConfirmationPreviewText.Raw("selected preview"))
            assertThat(selectedProvider.receivedContext).isEqualTo(context)
        }

    @Test
    fun `given important unsafe tools and simple unsafe tool, when providers resolve, then only important tools require dedicated providers`() =
        runTest {
            val registry = DefaultConfirmationPreviewProviderRegistry(
                setOf(
                    RecordingProvider(
                        key = "woocommerce_orders",
                        priority = 100,
                        supportedNames = setOf("orders_update", "orders_bulk_update"),
                    ),
                    RecordingProvider(
                        key = "woocommerce_products",
                        priority = 100,
                        supportedNames = setOf("products_update", "products_bulk_update"),
                    ),
                    RecordingProvider(
                        key = "woocommerce_product_variations",
                        priority = 100,
                        supportedNames = setOf("product_variations_update"),
                    ),
                    genericProvider,
                )
            )

            importantUnsafeToolNames.forEach { toolName ->
                val provider = registry.providerFor(context(descriptor(toolName)))

                assertThat(provider.key)
                    .`as`("$toolName should use a dedicated provider")
                    .isNotEqualTo("generic_schema")
            }

            val simpleProvider = registry.providerFor(context(simpleUnsafeDescriptor()))

            assertThat(simpleProvider.key).isEqualTo("generic_schema")
        }

    @Test
    fun `given unknown external unsafe descriptor, when provider resolves, then generic provider is selected`() =
        runTest {
            val registry = DefaultConfirmationPreviewProviderRegistry(
                setOf(
                    RecordingProvider(
                        key = "woocommerce_orders",
                        priority = 100,
                        supportedNames = setOf("orders_update", "orders_bulk_update"),
                    ),
                    genericProvider,
                )
            )

            val provider = registry.providerFor(context(externalUnsafeDescriptor()))

            assertThat(provider.key).isEqualTo("generic_schema")
        }

    private fun descriptor(
        name: String,
        inputSchema: JsonObject = inputSchema {
            string("reason", description = "Reason for the change.")
        },
        safetyLevel: ToolSafetyLevel = ToolSafetyLevel.UNSAFE,
    ) = ToolDescriptor(
        name = name,
        description = "$name descriptor",
        inputSchema = inputSchema,
        safetyLevel = safetyLevel,
    )

    private fun request(
        toolName: String,
        arguments: JsonObject = buildJsonObject { put("reason", "Preview") },
    ) = ConfirmationRequest(
        id = "confirmation-$toolName",
        toolCallId = "call-$toolName",
        toolName = toolName,
        arguments = arguments,
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    private fun context(
        descriptor: ToolDescriptor,
        arguments: JsonObject = buildJsonObject { put("reason", "Preview") },
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

    private fun simpleUnsafeDescriptor(name: String = "first_party_simple_write") = ToolDescriptor(
        name = name,
        description = "Simple first-party unsafe write used for preview policy tests.",
        inputSchema = inputSchema {
            string("reason", description = "Reason for the change.")
            boolean("enabled", description = "Whether the setting is enabled.")
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    private class RecordingProvider(
        override val key: String,
        override val priority: Int,
        private val supportedNames: Set<String>,
        private val previewMessage: String = key,
    ) : ConfirmationPreviewProvider {
        var receivedContext: ConfirmationPreviewContext? = null

        override fun canPreview(context: ConfirmationPreviewContext): Boolean =
            context.descriptor.name in supportedNames

        override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview {
            receivedContext = context
            return ConfirmationPreview(ConfirmationPreviewText.Raw(previewMessage))
        }
    }
}
