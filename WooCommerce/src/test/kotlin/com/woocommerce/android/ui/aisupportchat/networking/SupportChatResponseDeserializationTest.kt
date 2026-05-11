package com.woocommerce.android.ui.aisupportchat.networking

import com.google.gson.Gson
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatRole
import com.woocommerce.android.util.UnitTestUtils.jsonFileAs
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SupportChatResponseDeserializationTest {
    private val gson = Gson()

    @Test
    fun `given send-message fixture, when decoded, then full thread is exposed`() {
        val response = "support-chat/support-chat-send-message.json"
            .jsonFileAs(SupportChatResponse::class.java)!!

        assertThat(response.chatId).isEqualTo(4242L)
        assertThat(response.sessionId).isEqualTo("session-abc-123")
        assertThat(response.botSlug).isEqualTo("woo-workflow-support_mobile_inapp")
        assertThat(response.messages).hasSize(2)

        val userMessage = response.messages[0]
        assertThat(userMessage.role).isEqualTo(SupportChatRole.USER)
        assertThat(userMessage.content).isEqualTo("I can't load my orders")
        assertThat(userMessage.context).isNull()

        val botMessage = response.messages[1]
        assertThat(botMessage.role).isEqualTo(SupportChatRole.BOT)
        assertThat(botMessage.content).contains("**First**")
        val botContext = requireNotNull(botMessage.context)
        val flags = requireNotNull(botContext.flags)
        assertThat(flags.forwardToHumanSupport).isFalse
        assertThat(flags.loggedIn).isTrue
        assertThat(flags.branch).isNull()
        assertThat(botContext.sources).hasSize(1)
        assertThat(botContext.sources[0].url).isEqualTo("https://woocommerce.com/help/orders")
    }

    @Test
    fun `given forward-to-human fixture, when decoded, then escalation flag is set`() {
        val response = "support-chat/support-chat-forward-to-human.json"
            .jsonFileAs(SupportChatResponse::class.java)!!

        val context = requireNotNull(response.messages.single().context)
        val flags = requireNotNull(context.flags)
        assertThat(flags.forwardToHumanSupport).isTrue
        assertThat(flags.branch).isEqualTo("escalation")
    }

    @Test
    fun `given unknown role in fixture, when decoded, then role falls back to UNKNOWN`() {
        val response = "support-chat/support-chat-unknown-role.json"
            .jsonFileAs(SupportChatResponse::class.java)!!

        assertThat(response.messages.single().role).isEqualTo(SupportChatRole.UNKNOWN)
    }

    @Test
    fun `given fetch-chat fixture, when decoded, then multi-turn thread is preserved`() {
        val response = "support-chat/support-chat-fetch-chat.json"
            .jsonFileAs(SupportChatResponse::class.java)!!

        assertThat(response.messages).hasSize(4)
        assertThat(response.messages.map { it.role }).containsExactly(
            SupportChatRole.USER,
            SupportChatRole.BOT,
            SupportChatRole.USER,
            SupportChatRole.BOT
        )
        // Optional fields tolerated when missing
        assertThat(response.messages[2].context).isNull()
        // Last bot message preserves opaque branch text
        val lastContext = requireNotNull(response.messages[3].context)
        val lastFlags = requireNotNull(lastContext.flags)
        assertThat(lastFlags.branch).isEqualTo("diagnostics")
    }

    @Test
    fun `given missing optional flags, when decoded, then defaults to false`() {
        val json = """
            {
              "chat_id": 1,
              "messages": [
                {
                  "message_id": 1,
                  "role": "bot",
                  "content": "ok",
                  "context": { "flags": {} }
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, SupportChatResponse::class.java)
        val context = requireNotNull(response.messages.single().context)
        val flags = requireNotNull(context.flags)
        assertThat(flags.forwardToHumanSupport).isFalse
        assertThat(flags.cannedResponse).isFalse
        assertThat(flags.loggedIn).isFalse
        assertThat(flags.branch).isNull()
    }
}
