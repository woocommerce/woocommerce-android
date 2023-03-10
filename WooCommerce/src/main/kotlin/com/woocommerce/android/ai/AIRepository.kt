package com.woocommerce.android.ai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class AIRepository @Inject constructor() {
    companion object {
        private val OPENAI = OpenAI(BuildConfig.OPENAI_TOKEN)
    }
    suspend fun openAIGenerateCompletion(prompt: String): String {

        val completionRequest = CompletionRequest(
            model = ModelId(BuildConfig.OPENAI_CHOSEN_MODEL),
            prompt = prompt,
            maxTokens = BuildConfig.OPENAI_MAX_TOKENS.toInt()
        )
        val result = OPENAI.completion(completionRequest).choices.first().text
        WooLog.d(WooLog.T.UTILS, result)
        return result
    }

    @OptIn(BetaOpenAI::class)
    suspend fun openAIGenerateChat(prompt: String): String {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(BuildConfig.OPENAI_CHOSEN_MODEL),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = prompt
                )
            )
        )
        OPENAI.chatCompletion(chatCompletionRequest).choices.first().message?.let {
            return it.content
        } ?: return ""
    }
}
