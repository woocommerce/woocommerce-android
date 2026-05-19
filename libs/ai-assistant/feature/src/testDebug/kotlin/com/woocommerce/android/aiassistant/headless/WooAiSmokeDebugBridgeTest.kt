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
}
