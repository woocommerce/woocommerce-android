package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.cards.DefaultShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.MAX_SHOW_CARDS_REFS
import com.woocommerce.android.aiassistant.tools.handlers.cards.MissingRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.ResolvedRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardFamily
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsArguments
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsRejectionReason
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolution
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsStructured
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.tools.handlers.cards.ValidatedRef
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ShowCardsToolHandler internal constructor(
    private val resolver: ShowCardsResolver,
) : AssistantToolHandler {

    @Inject constructor() : this(DefaultShowCardsResolver())

    override val descriptor = ToolDescriptor(
        name = "show_cards",
        description = "Show entity cards in the UI for orders or products selected by the assistant.",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("references") {
                    put("type", "array")
                    put("maxItems", MAX_SHOW_CARDS_REFS)
                    putJsonObject("items") {
                        put("type", "object")
                        put("additionalProperties", false)
                        putJsonObject("properties") {
                            putJsonObject("family") {
                                put("type", "string")
                                putJsonArray("enum") {
                                    add("order")
                                    add("product")
                                }
                            }
                            putJsonObject("id") {
                                put("type", "string")
                            }
                        }
                        putJsonArray("required") {
                            add("family")
                            add("id")
                        }
                    }
                }
            }
            putJsonArray("required") {
                add("references")
            }
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val arguments = try {
            json.decodeFromJsonElement<ShowCardsArguments>(call.arguments)
        } catch (exception: SerializationException) {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${exception.message}")
        } catch (exception: IllegalArgumentException) {
            return ToolResult.ValidationError(call.id, "Invalid arguments: ${exception.message}")
        }

        val validRefs = parseValidRefs(arguments.references)
        val resolutions = runCatching { resolver.resolve(validRefs) }
            .getOrElse {
                validRefs.map { ref ->
                    ShowCardsResolution.Missing(
                        ref = ref,
                        reason = ShowCardsRejectionReason.FetchFailed,
                    )
                }
            }
        val resolvedCards = resolutions.filterIsInstance<ShowCardsResolution.Resolved>()
        val structured = ShowCardsStructured(
            requested = arguments.references.size,
            validated = validRefs.size,
            rendered = resolvedCards.size,
            resolvedRefs = resolvedCards.map { it.toResolvedRef() },
            missingRefs = resolutions.filterIsInstance<ShowCardsResolution.Missing>().map { it.toMissingRef() },
            rejectedRefs = emptyList(),
        )

        return ToolResult.Success(
            toolCallId = call.id,
            structured = json.encodeToJsonElement(structured),
            uiStructured = json.encodeToJsonElement(
                ShowCardsUiStructured(cards = resolvedCards.map { it.card })
            ),
        )
    }

    private fun parseValidRefs(references: List<JsonElement>): List<ValidatedRef> =
        references.mapIndexedNotNull { index, reference ->
            val ref = reference.jsonObject
            val family = ref["family"]?.jsonPrimitive?.content?.let(ShowCardFamily::from)
                ?: return@mapIndexedNotNull null
            val id = ref["id"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: return@mapIndexedNotNull null

            ValidatedRef(index = index, family = family, id = id)
        }

    private fun ShowCardsResolution.Resolved.toResolvedRef(): ResolvedRef =
        ResolvedRef(
            family = ref.family.serializedName,
            id = ref.id,
            summary = summary,
        )

    private fun ShowCardsResolution.Missing.toMissingRef(): MissingRef =
        MissingRef(
            family = ref.family.serializedName,
            id = ref.id,
            reason = reason,
        )

    private companion object {
        val json = Json {
            explicitNulls = false
        }
    }
}
