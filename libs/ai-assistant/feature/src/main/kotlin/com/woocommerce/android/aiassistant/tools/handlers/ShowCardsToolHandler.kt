package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.handlers.cards.DefaultShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.MAX_SHOW_CARDS_REFS
import com.woocommerce.android.aiassistant.tools.handlers.cards.MissingRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.ResolvedRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.SHOW_CARDS_TOOL_NAME
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsArguments
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsReferenceValidator
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsRejectionReason
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolution
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsStructured
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.tools.handlers.cards.ValidatedRef
import com.woocommerce.android.aiassistant.tools.handlers.cards.toJsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

internal class ShowCardsToolHandler internal constructor(
    private val resolver: ShowCardsResolver,
    private val json: Json,
) : AssistantToolHandler {
    private val referenceValidator = ShowCardsReferenceValidator()

    @Inject constructor(
        resolver: DefaultShowCardsResolver,
        @AiAssistantJson json: Json,
    ) : this(resolver as ShowCardsResolver, json)

    internal constructor(resolver: ShowCardsResolver) : this(
        resolver = resolver,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    override val descriptor = ToolDescriptor(
        name = SHOW_CARDS_TOOL_NAME,
        description = "Show rich cards in the Android UI for order/product/variation/customer entity references " +
            "or an analytics_stats ID produced after a successful analytics_orders result. Variation references " +
            "use strict {parentProductId}/{variationId} ids and should be used only for explicit " +
            "variation-level questions about sizes, colors, options, or known variation IDs. For broad product " +
            "inventory lists, render product references.",
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
                                    add("variation")
                                    add("analytics_stats")
                                    add("customer")
                                }
                            }
                            putJsonObject("id") {
                                put("type", "string")
                                put(
                                    "description",
                                    "Entity id. For variation, use strict {parentProductId}/{variationId}. " +
                                        "For analytics_stats, pass the exact card_id returned by " +
                                        "analytics_orders; do not construct it manually.",
                                )
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

        val validation = referenceValidator.validate(arguments.references)
        val validRefs = validation.validRefs
        val resolutions = resolveRefs(validRefs)
        val resolvedCards = resolutions.filterIsInstance<ShowCardsResolution.Resolved>()
        val structured = ShowCardsStructured(
            requested = validation.requested,
            validated = validRefs.size,
            rendered = resolvedCards.size,
            resolvedRefs = resolvedCards.map { it.toResolvedRef() },
            missingRefs = resolutions.filterIsInstance<ShowCardsResolution.Missing>().map { it.toMissingRef() },
            rejectedRefs = validation.rejectedRefs,
        )

        return ToolResult.Success(
            toolCallId = call.id,
            structured = json.encodeToJsonElement(structured),
            uiStructured = json.encodeToJsonElement(
                ShowCardsUiStructured(cards = resolvedCards.map { it.card })
            ),
        )
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun resolveRefs(validRefs: List<ValidatedRef>) =
        try {
            resolver.resolve(validRefs)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            validRefs.map { ref ->
                ShowCardsResolution.Missing(
                    ref = ref,
                    reason = ShowCardsRejectionReason.FetchFailed,
                )
            }
        }

    private fun ShowCardsResolution.Resolved.toResolvedRef(): ResolvedRef =
        ResolvedRef(
            family = ref.family.serializedName,
            id = ref.id,
            summary = summary.toJsonObject(json),
        )

    private fun ShowCardsResolution.Missing.toMissingRef(): MissingRef =
        MissingRef(
            family = ref.family.serializedName,
            id = ref.id,
            reason = reason,
        )
}
