package com.woocommerce.android.aiassistant.di

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSources
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AiAssistantModuleTest {
    @Test
    fun `given a serializable type, when encoded with the provided Json, then output is compact json`() {
        val json = AiAssistantModule.provideAiAssistantJson()

        val encoded = json.encodeToString(Sample(value = "hello"))

        assertThat(encoded).isEqualTo("""{"value":"hello"}""")
    }

    @Test
    fun `given json with unknown keys, when decoded with the provided Json, then unknown keys are ignored`() {
        val json = AiAssistantModule.provideAiAssistantJson()

        val decoded = json.decodeFromString<Sample>("""{"value":"hi","extra":1}""")

        assertThat(decoded.value).isEqualTo("hi")
    }

    @Test
    fun `given an OkHttpClient, when creating an EventSource via okhttp-sse, then the factory returns a non-null source`() {
        val factory = EventSources.createFactory(OkHttpClient())
        val request = Request.Builder().url("https://example.com/").build()

        val eventSource = factory.newEventSource(request, NoopListener)

        assertThat(eventSource).isNotNull
    }

    @Serializable
    private data class Sample(val value: String)

    private object NoopListener : okhttp3.sse.EventSourceListener()
}
