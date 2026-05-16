@file:Suppress("ImportOrdering")

package com.woocommerce.android.aiassistant.headless

import android.app.Application
import com.woocommerce.android.aiassistant.tools.WooCommerceToolRegistry
import dagger.hilt.EntryPoints
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class WooAiSmokeRunExit(
    val artifactsDirectory: File,
    val sourceArtifactsDirectory: File = artifactsDirectory,
    val failureMessage: String?,
) {
    fun artifactDirectories(): List<File> = listOf(sourceArtifactsDirectory, artifactsDirectory)
        .distinctBy { it.absolutePath }
}

object WooAiSmokeDebugBridge {
    @Suppress("LongMethod")
    suspend fun runLive(
        application: Application,
        credentials: WooAiSmokeCredentialConfig,
    ): WooAiSmokeRunExit {
        val entryPoint = EntryPoints.get(application, WooAiSmokeDebugEntryPoint::class.java)
        val redactor = WooAiSmokeRedactor(
            siteUrl = credentials.siteUrl,
            username = credentials.username,
            appPassword = credentials.appPassword,
        )
        val outputDirectory = credentials.outputDirectory
        return runCatching {
            runPhase("jwt_mint", JWT_MINT_TIMEOUT) {
                entryPoint.liveChatServiceFactory()
                    .createTokenProvider(credentials, redactor)
                    .provide()
            }
            WooAiSmokeApplicationPasswordStore.installRobolectricPreferences(
                context = application,
                applicationPasswordsStore = entryPoint.applicationPasswordsStore(),
            )

            val bootstrap = runPhase("selected_site_and_tool_preflight", BOOTSTRAP_TIMEOUT) {
                WooAiSmokeCredentialBootstrap(
                    siteStore = entryPoint.siteStore(),
                    selectedSite = entryPoint.selectedSite(),
                    applicationPasswordsStore = entryPoint.applicationPasswordsStore(),
                    toolRegistry = entryPoint.toolRegistry(),
                ).bootstrap(credentials)
            }

            outputDirectory.mkdirs()
            val preflightJson = entryPoint.json().encodeToString(bootstrap.preflight)
            val redactedPreflightJson = redactor.redact(preflightJson)
            File(outputDirectory, PREFLIGHT_FILE_NAME).writeText(redactedPreflightJson)

            require(entryPoint.toolRegistry() is WooCommerceToolRegistry) {
                "Expected WooCommerceToolRegistry"
            }
            val exit = runPhase("live_scenarios", LIVE_SCENARIOS_TIMEOUT) {
                WooAiSmokeRunner(
                    chatService = entryPoint.liveChatServiceFactory().create(credentials, redactor),
                    toolRegistry = entryPoint.toolRegistry(),
                    toolCatalogSelector = entryPoint.toolCatalogSelector(),
                    retryPolicy = entryPoint.retryPolicy(),
                    historyBudgeter = entryPoint.historyBudgeter(),
                    systemPromptProvider = entryPoint.systemPromptProvider(),
                    json = entryPoint.json(),
                    timeSource = entryPoint.timeSource(),
                    config = WooAiSmokeConfig(
                        baselineMode = credentials.mode,
                        scenarioResourceName = "live-scenarios.json",
                        baselineResourceName = "live-baseline.json",
                        approvedBaselineFileName = "approved-live-baseline.json",
                        usePerRunDirectory = true,
                    ),
                    selectedSiteId = bootstrap.site.siteId,
                    outputDirectory = outputDirectory,
                    jwtProviderClass = "WooAiSmokeDirectJwtTokenProvider",
                    storeLabel = credentials.storeLabel,
                    credentialSource = credentials.credentialSource,
                    redactor = redactor,
                ).run()
            }
            writePreflightArtifacts(
                exit = exit,
                preflightJson = redactedPreflightJson,
            )
            exit
        }.getOrElse { error ->
            WooAiSmokeRunExit(
                artifactsDirectory = outputDirectory,
                sourceArtifactsDirectory = outputDirectory,
                failureMessage = redactor.redact(error.message ?: error::class.java.simpleName),
            )
        }
    }

    internal fun writePreflightArtifacts(
        exit: WooAiSmokeRunExit,
        preflightJson: String,
    ) {
        exit.artifactDirectories().forEach { directory ->
            directory.mkdirs()
            File(directory, PREFLIGHT_FILE_NAME).writeText(preflightJson)
        }
    }

    private suspend fun <T> runPhase(
        phaseName: String,
        timeout: Duration,
        block: suspend () -> T,
    ): T = try {
        withTimeout(timeout) { block() }
    } catch (_: TimeoutCancellationException) {
        error("PHASE_TIMEOUT: $phaseName")
    }

    private val JWT_MINT_TIMEOUT = 30.seconds
    private val BOOTSTRAP_TIMEOUT = 3.minutes
    private val LIVE_SCENARIOS_TIMEOUT = 5.minutes
    private const val PREFLIGHT_FILE_NAME = "preflight.json"
}
