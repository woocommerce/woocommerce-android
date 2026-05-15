package com.woocommerce.android.aiassistant.headless

import android.app.Application
import dagger.hilt.EntryPoints
import java.io.File

data class WooAiSmokeRunExit(
    val artifactsDirectory: File,
    val failureMessage: String?,
)

object WooAiSmokeDebugBridge {
    suspend fun run(
        application: Application,
        instrumentationArguments: Map<String, String?>,
    ): WooAiSmokeRunExit {
        val config = WooAiSmokeConfig.fromInstrumentationArguments(instrumentationArguments)
        if (!config.enabled) {
            return WooAiSmokeRunExit(
                artifactsDirectory = File(application.filesDir, "${config.outputDirectoryName}/latest"),
                failureMessage = null,
            )
        }

        val entryPoint = EntryPoints.get(application, WooAiSmokeDebugEntryPoint::class.java)
        val preflight = WooAiSmokePreflight(entryPoint.selectedSite())
        val selectedSite = preflight.requireReady()
        val outputDirectory = File(application.filesDir, "${config.outputDirectoryName}/latest")

        return WooAiSmokeRunner(
            chatService = entryPoint.chatService(),
            toolRegistry = entryPoint.toolRegistry(),
            toolCatalogSelector = entryPoint.toolCatalogSelector(),
            retryPolicy = entryPoint.retryPolicy(),
            historyBudgeter = entryPoint.historyBudgeter(),
            systemPromptProvider = entryPoint.systemPromptProvider(),
            json = entryPoint.json(),
            timeSource = entryPoint.timeSource(),
            config = config,
            selectedSiteId = selectedSite.siteId,
            outputDirectory = outputDirectory,
        ).run()
    }
}
