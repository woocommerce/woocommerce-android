package com.woocommerce.android.ai

import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.WooLog

class AIRepository {
    suspend fun openAIGenerateCompletion(prompt: String): String {
        val openAi = OpenAI(BuildConfig.OPENAI_TOKEN)
        val completionRequest = CompletionRequest(
            model = ModelId(BuildConfig.OPENAI_CHOSEN_MODEL),
            prompt = prompt,
            maxTokens = BuildConfig.OPENAI_MAX_TOKENS.toInt()
        )
        val result = openAi.completion(completionRequest).choices.first().text
        WooLog.d(WooLog.T.UTILS, result)
        return result
    }
}
