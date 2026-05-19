package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.store.SiteStore
import kotlin.time.TimeSource

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WooAiSmokeDebugEntryPoint {
    fun toolRegistry(): ToolRegistry
    fun toolCatalogSelector(): ToolCatalogSelector
    fun retryPolicy(): RetryPolicy
    fun historyBudgeter(): HistoryBudgeter
    fun systemPromptProvider(): AssistantSystemPromptProvider

    @AiAssistantJson
    fun json(): Json
    fun timeSource(): TimeSource
    fun selectedSite(): SelectedSite
    fun siteStore(): SiteStore
    fun applicationPasswordsStore(): ApplicationPasswordsStore
    fun liveChatServiceFactory(): WooAiSmokeLiveChatServiceFactory
}
