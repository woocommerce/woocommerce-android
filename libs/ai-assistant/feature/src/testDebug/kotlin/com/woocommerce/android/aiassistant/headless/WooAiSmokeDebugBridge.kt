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

object WooAiSmokeDebugBridge {
    @Suppress("LongMethod")
    suspend fun runLive(
        application: Application,
        credentials: WooAiSmokeCredentialConfig,
        mode: WooAiSmokeBaselineMode,
    ): WooAiSmokeRunExit {
        require(WooAiSmokeLiveRunGate.isEnabled()) {
            WooAiSmokeLiveRunGate.DISABLED_REASON
        }
        val entryPoint = EntryPoints.get(application, WooAiSmokeDebugEntryPoint::class.java)
        val redactor = WooAiSmokeRedactor(
            siteUrl = credentials.siteUrl,
            wpComUsername = credentials.wpComUsername,
            wpComPassword = credentials.wpComPassword,
        )
        val outputDirectory = credentials.outputDirectory
        return runCatching {
            validateLiveRequest(
                credentials = credentials,
                mode = mode,
            )
            runPhase("wpcom_auth", WPCOM_AUTH_TIMEOUT) {
                entryPoint.wpComAuthenticator().authenticate(credentials)
            }
            val resolvedSite = runPhase("wpcom_site_resolution", WPCOM_SITE_RESOLUTION_TIMEOUT) {
                entryPoint.wpComSiteResolver().resolve(credentials)
            }
            val bootstrap = runPhase("selected_site_and_tool_preflight", BOOTSTRAP_TIMEOUT) {
                WooAiSmokeCredentialBootstrap(
                    siteStore = entryPoint.siteStore(),
                    selectedSite = entryPoint.selectedSite(),
                    toolRegistry = entryPoint.toolRegistry(),
                ).bootstrap(
                    credentials = credentials,
                    resolvedSite = resolvedSite,
                    wpComAccessTokenPresent = entryPoint.accountStore().hasAccessToken(),
                )
            }

            outputDirectory.mkdirs()
            val preflightJson = entryPoint.json().encodeToString(bootstrap.preflight)
            val redactedPreflightJson = redactor.redact(preflightJson)
            File(outputDirectory, PREFLIGHT_FILE_NAME).writeText(redactedPreflightJson)

            require(entryPoint.toolRegistry() is WooCommerceToolRegistry) {
                "Expected WooCommerceToolRegistry"
            }
            val exit = runPhase("live_scenarios", LIVE_SCENARIOS_TIMEOUT * credentials.sampleCount) {
                WooAiSmokeRunner(
                    chatService = entryPoint.liveChatServiceFactory().create(),
                    toolRegistry = entryPoint.toolRegistry(),
                    toolCatalogSelector = entryPoint.toolCatalogSelector(),
                    retryPolicy = entryPoint.retryPolicy(),
                    historyBudgeter = entryPoint.historyBudgeter(),
                    systemPromptProvider = entryPoint.systemPromptProvider(),
                    json = entryPoint.json(),
                    timeSource = entryPoint.timeSource(),
                    config = WooAiSmokeConfig(
                        scenarioResourceName = "live-scenarios.json",
                        baseline = WooAiSmokeBaselineConfig(
                            mode = mode,
                            resourceName = "live-baseline.json",
                            approvedFileName = "approved-live-baseline.json",
                        ),
                        usePerRunDirectory = true,
                        sampleCount = credentials.sampleCount,
                        scenarioIds = credentials.scenarioIds,
                    ),
                    selectedSiteId = bootstrap.site.siteId,
                    outputDirectory = outputDirectory,
                    authProviderClass = "AccessTokenWpComOAuthTokenProvider",
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
            redactedFailureExit(credentials, error)
        }
    }

    internal fun validateLiveRequest(
        credentials: WooAiSmokeCredentialConfig,
        mode: WooAiSmokeBaselineMode,
    ) {
        require(mode != WooAiSmokeBaselineMode.APPROVE || credentials.scenarioIds.isEmpty()) {
            "WOO_AI_SMOKE_SCENARIO_ID cannot be used with approval; baseline approval must run the full suite."
        }
    }

    internal fun redactedFailureExit(
        credentials: WooAiSmokeCredentialConfig,
        error: Throwable,
    ): WooAiSmokeRunExit {
        val redactor = WooAiSmokeRedactor(
            siteUrl = credentials.siteUrl,
            wpComUsername = credentials.wpComUsername,
            wpComPassword = credentials.wpComPassword,
        )
        return WooAiSmokeRunExit(
            artifactsDirectory = credentials.outputDirectory,
            sourceArtifactsDirectory = credentials.outputDirectory,
            failureMessage = redactor.redact(error.message ?: error::class.java.simpleName),
        )
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

    private val BOOTSTRAP_TIMEOUT = 3.minutes
    private val WPCOM_AUTH_TIMEOUT = 1.minutes
    private val WPCOM_SITE_RESOLUTION_TIMEOUT = 1.minutes
    private val LIVE_SCENARIOS_TIMEOUT = 5.minutes
    private const val PREFLIGHT_FILE_NAME = "preflight.json"
}
