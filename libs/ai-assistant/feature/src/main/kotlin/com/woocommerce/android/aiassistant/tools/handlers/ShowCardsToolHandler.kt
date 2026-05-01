package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.cards.MAX_SHOW_CARDS_REFS
import javax.inject.Inject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ShowCardsToolHandler @Inject constructor() : StubToolHandler() {
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
}
