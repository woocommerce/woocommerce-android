package com.woocommerce.android.aiassistant.telemetry

internal class FakeAssistantTelemetryIdGenerator(
    ids: List<String> = listOf("conversation-1") +
        generateSequence(1) { it + 1 }.map { "request-$it" }.take(100).toList(),
) : AssistantTelemetryIdGenerator {
    private val iterator = ids.iterator()
    val recorded = mutableListOf<String>()

    override fun nextId(): String {
        val next = iterator.next()
        recorded += next
        return next
    }
}
