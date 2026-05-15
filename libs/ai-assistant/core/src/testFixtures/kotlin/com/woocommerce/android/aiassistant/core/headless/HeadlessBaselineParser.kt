package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class HeadlessBaselineParser(
    private val json: Json,
) {
    fun parse(source: String): HeadlessBaseline =
        json.decodeFromJsonElement(
            HeadlessBaseline.serializer(),
            migrateLegacyBaseline(json.parseToJsonElement(source).jsonObject),
        )

    fun parseStrict(source: String): HeadlessBaseline =
        json.decodeFromString(HeadlessBaseline.serializer(), source)

    fun parseApprovedBaseline(source: String): HeadlessApprovedBaseline =
        json.decodeFromString(HeadlessApprovedBaseline.serializer(), source)

    private fun migrateLegacyBaseline(source: JsonObject): JsonObject {
        val scenarios = source.getValue("scenarios").jsonArray.map { scenarioElement ->
            val scenario = scenarioElement.jsonObject
            JsonObject(
                scenario.toMutableMap().apply {
                    putIfAbsent("category", JsonPrimitive(HeadlessScenarioCategory.LEGACY_SCRIPTED.name))
                    putIfAbsent("scope", JsonPrimitive("GLOBAL"))
                    putIfAbsent("hardChecks", JsonArray(emptyList()))
                    putIfAbsent("smokeFixture", JsonNull)
                    put(
                        "turns",
                        JsonArray(
                            scenario.getValue("turns").jsonArray.map { turnElement ->
                                val turn = turnElement.jsonObject
                                JsonObject(
                                    turn.toMutableMap().apply {
                                        putIfAbsent("hardChecks", JsonArray(emptyList()))
                                    }
                                )
                            }
                        )
                    )
                }
            )
        }
        return JsonObject(source.toMutableMap().apply { put("scenarios", JsonArray(scenarios)) })
    }
}
