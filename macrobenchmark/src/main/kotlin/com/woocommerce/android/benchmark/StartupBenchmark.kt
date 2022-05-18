package com.woocommerce.android.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.quicklogin.QuickLoginHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.woocommerce.android"

private const val EMAIL = ""
private const val PASSWORD = ""
private const val WEB_SITE = ""

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    private val helper = QuickLoginHelper(PACKAGE_NAME)

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() {
        startup(CompilationMode.None())
    }

    @Test
    fun startupBaselineProfile() {
        startup(
            CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Require
            )
        )
    }

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "com.woocommerce.android",
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            setupBlock = {
                helper.loginWithWordpress(
                    email = EMAIL,
                    password = PASSWORD,
                    webSite = WEB_SITE,
                )
            }
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
