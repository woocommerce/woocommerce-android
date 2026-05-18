@file:Suppress("FunctionNaming", "ImportOrdering")

package com.woocommerce.android.aiassistant.headless

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import java.io.File

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(
    application = HiltTestApplication::class,
    manifest = Config.NONE,
)
class WooAiSmokeLiveRobolectricTest {
    private val liveEnvRule = WooAiSmokeLiveEnvRule(
        System.getenv(),
        defaultOutputDirectory(),
    )
    private val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(liveEnvRule).around(hiltRule)

    @Test
    fun `when live smoke runs, then artifacts are written`() {
        runBlocking {
            val credentials = liveEnvRule.requireValidCredentials()
            hiltRule.inject()

            val application = ApplicationProvider.getApplicationContext<Application>()
            WellSql.init(WellSqlConfig(application))

            val exit = WooAiSmokeDebugBridge.runLive(
                application = application,
                credentials = credentials,
            )

            assertThat(exit.failureMessage).isNull()
            assertThat(File(exit.artifactsDirectory, "preflight.json")).exists()
        }
    }

    private fun defaultOutputDirectory(): File =
        File(repoRoot(), "libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest")

    private fun repoRoot(): File {
        val workingDir = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        return generateSequence(workingDir) { it.parentFile }
            .first { File(it, "settings.gradle").isFile }
    }
}
