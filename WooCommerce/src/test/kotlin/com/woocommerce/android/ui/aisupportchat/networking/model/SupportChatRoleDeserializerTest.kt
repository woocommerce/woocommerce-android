package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class SupportChatRoleDeserializerTest {
    private val deserializer = SupportChatRole.Deserializer()
    private val context: JsonDeserializationContext = mock()

    @Test
    fun `given user wire value, when deserialized, then USER is returned`() {
        val element = JsonParser.parseString("\"user\"")

        val result = deserializer.deserialize(element, SupportChatRole::class.java, context)

        assertThat(result).isEqualTo(SupportChatRole.USER)
    }

    @Test
    fun `given bot wire value, when deserialized, then BOT is returned`() {
        val element = JsonParser.parseString("\"bot\"")

        val result = deserializer.deserialize(element, SupportChatRole::class.java, context)

        assertThat(result).isEqualTo(SupportChatRole.BOT)
    }

    @Test
    fun `given unknown wire value, when deserialized, then UNKNOWN is returned`() {
        val element = JsonParser.parseString("\"system\"")

        val result = deserializer.deserialize(element, SupportChatRole::class.java, context)

        assertThat(result).isEqualTo(SupportChatRole.UNKNOWN)
    }

    @Test
    fun `given null json element, when deserialized, then UNKNOWN is returned`() {
        val result = deserializer.deserialize(JsonNull.INSTANCE, SupportChatRole::class.java, context)

        assertThat(result).isEqualTo(SupportChatRole.UNKNOWN)
    }

    @Test
    fun `given non-primitive json element, when deserialized, then UNKNOWN is returned`() {
        val result = deserializer.deserialize(JsonObject(), SupportChatRole::class.java, context)

        assertThat(result).isEqualTo(SupportChatRole.UNKNOWN)
    }
}
