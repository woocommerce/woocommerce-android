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
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
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
    fun dispatcher(): Dispatcher
    fun accountStore(): AccountStore
    fun siteStore(): SiteStore
    fun wpComAuthenticator(): WooAiSmokeWpComAuthenticator
    fun wpComSiteResolver(): WooAiSmokeWpComSiteResolver
    fun liveChatServiceFactory(): WooAiSmokeLiveChatServiceFactory
}
