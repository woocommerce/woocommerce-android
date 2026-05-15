@file:Suppress("ImportOrdering")

package com.woocommerce.android.aiassistant.headless

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.WooWellSqlConfig
import com.woocommerce.android.di.FluxCModule
import com.yarolegovich.wellsql.WellSql
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.di.WCDatabaseModule
import org.wordpress.android.fluxc.module.MediaModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import java.io.File

@HiltAndroidTest
@UninstallModules(FluxCModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    application = HiltTestApplication::class,
    manifest = Config.NONE,
)
class WooAiSmokeLiveRobolectricApprovalTest {
    @Module(
        includes = [
            WooAiSmokeRobolectricNetworkModule::class,
            OkHttpClientModule::class,
            WCDatabaseModule::class,
            MediaModule::class,
        ],
    )
    @InstallIn(SingletonComponent::class)
    abstract class RobolectricFluxCModule

    private val liveEnvRule = WooAiSmokeLiveEnvRule(
        System.getenv() + ("WOO_AI_SMOKE_MODE" to "approve"),
        defaultOutputDirectory(),
    )
    private val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(liveEnvRule).around(hiltRule)

    @Test
    fun `when live approval runs, then approved baseline is written`() {
        runBlocking {
            val credentials = liveEnvRule.requireValidCredentials()
            hiltRule.inject()

            val application = ApplicationProvider.getApplicationContext<Application>()
            WellSql.init(WooWellSqlConfig(application))

            val exit = WooAiSmokeDebugBridge.runLive(
                application = application,
                credentials = credentials,
            )

            assertThat(exit.failureMessage).isNull()
            assertThat(File(exit.artifactsDirectory, "approved-live-baseline.json")).exists()
        }
    }

    private fun defaultOutputDirectory(): File =
        File(repoRoot(), "WooCommerce/build/outputs/woo-ai-smoke/live/latest")

    private fun repoRoot(): File {
        val workingDir = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        return generateSequence(workingDir) { it.parentFile }
            .first { File(it, "settings.gradle").isFile }
    }
}
