package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

internal class GenericSchemaConfirmationPreviewProvider @Inject constructor() : ConfirmationPreviewProvider {
    override val key: String = "generic_schema"
    override val priority: Int = 0

    override fun canPreview(context: ConfirmationPreviewContext): Boolean =
        context.descriptor.safetyLevel == ToolSafetyLevel.UNSAFE

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview {
        val properties = context.descriptor.inputSchema.objectValue("properties").orEmpty()
        val fields = properties.keys.sorted().mapNotNull { propertyName ->
            val value = context.request.arguments[propertyName] ?: return@mapNotNull null
            ConfirmationPreviewField(
                name = propertyName,
                label = raw(propertyName.toSchemaLabel()),
                value = raw(value.toPreviewValue()),
            )
        }
        return ConfirmationPreview(
            message = string(
                R.string.ai_assistant_confirmation_generic_tool_call,
                raw(context.descriptor.name),
            ),
            fields = fields,
        )
    }

    private fun JsonElement.toPreviewValue(): String = when (this) {
        JsonNull -> "null"
        is JsonPrimitive -> contentOrNull ?: toString()
        is JsonArray -> if (all { it is JsonPrimitive } && size <= ARRAY_INLINE_LIMIT) {
            joinToString(", ") { element ->
                val primitive = element as JsonPrimitive
                primitive.contentOrNull ?: primitive.toString()
            }
        } else {
            "$size items"
        }
        is JsonObject -> "$size fields"
    }

    private fun String.toSchemaLabel(): String =
        split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { first -> first.uppercaseChar() }
            }
            .ifBlank { this }

    private companion object {
        const val ARRAY_INLINE_LIMIT = 5
    }
}
