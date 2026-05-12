package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantTelemetryTest {
    @Test
    fun `when interface is inspected, then it exposes only a Trackable track method`() {
        val methods = AssistantTelemetry::class.java.declaredMethods

        assertThat(methods.map { it.name }).containsExactly("track")
        assertThat(methods.single().parameterTypes.toList()).containsExactly(Trackable::class.java)
    }
}
