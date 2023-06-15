package com.woocommerce.android.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val jetpackAIStore: JetpackAIStore
) {
    suspend fun fetchJetpackAICompletionsForSite(
        site: SiteModel,
        prompt: String,
        skipCache: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        jetpackAIStore.fetchJetpackAICompletionsForSite(site, prompt, skipCache).run {
            when (this) {
                is JetpackAICompletionsResponse.Success -> {
                    Result.success(completion)
                }
                is JetpackAICompletionsResponse.Error -> {
                    Result.failure(Exception(message ?: "Unable to fetch AI completions"))
                }
            }
        }
    }
}
