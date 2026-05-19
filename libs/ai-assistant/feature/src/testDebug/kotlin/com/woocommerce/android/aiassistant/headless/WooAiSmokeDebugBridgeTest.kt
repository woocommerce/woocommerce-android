@file:Suppress("FunctionNaming")

package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WooAiSmokeDebugBridgeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `given per run exit, when preflight is written, then latest and run directories both receive it`() {
        val latestDirectory = temporaryFolder.newFolder("live").resolve("latest")
        val runDirectory = requireNotNull(latestDirectory.parentFile).resolve("runs/20260516-120000-deadbeef")
        val exit = WooAiSmokeRunExit(
            artifactsDirectory = latestDirectory,
            sourceArtifactsDirectory = runDirectory,
            failureMessage = null,
        )

        WooAiSmokeDebugBridge.writePreflightArtifacts(
            exit = exit,
            preflightJson = """{"safeToolResults":[{"toolName":"orders_list","resultKind":"SUCCESS"}]}""",
        )

        assertThat(latestDirectory.resolve("preflight.json")).exists()
        assertThat(runDirectory.resolve("preflight.json")).exists()
        assertThat(runDirectory.resolve("preflight.json").readText())
            .isEqualTo(latestDirectory.resolve("preflight.json").readText())
    }

    @Test
    fun `given live bridge failure, when failure exit is built, then credentials are redacted`() {
        val outputDirectory = temporaryFolder.newFolder("live-output")
        val exit = WooAiSmokeDebugBridge.redactedFailureExit(
            credentials = WooAiSmokeCredentialConfig(
                siteUrl = "https://leaky-store.example",
                siteId = 2922L,
                username = "merchant@example.com",
                appPassword = "app password",
                storeLabel = "store",
                outputDirectory = outputDirectory,
                credentialSource = "test",
            ),
            error = IllegalStateException(
                "Failed for https://leaky-store.example, leaky-store.example, merchant@example.com, app password"
            ),
        )

        assertThat(exit.artifactsDirectory).isEqualTo(outputDirectory)
        assertThat(exit.sourceArtifactsDirectory).isEqualTo(outputDirectory)
        assertThat(exit.failureMessage)
            .doesNotContain("https://leaky-store.example")
            .doesNotContain("leaky-store.example")
            .doesNotContain("merchant@example.com")
            .doesNotContain("app password")
            .contains("[REDACTED]")
    }
}
