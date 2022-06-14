package com.woocommerce.android.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import com.woocommerce.android.quicklogin.QuickLoginHelper
import org.junit.Rule
import org.junit.Test

class LoggedInStartupBenchmark {
    private val helper = QuickLoginHelper(PACKAGE_NAME)

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupPartialWithBaselineProfiles() =
        startup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 10,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            setupBlock = {
                if (!helper.isLoggedIn()) {
                    helper.loginWithWordpress(
                        email = BuildConfig.QUICK_LOGIN_WP_EMAIL,
                        password = BuildConfig.QUICK_LOGIN_WP_PASSWORD,
                        webSite = BuildConfig.QUICK_LOGIN_WP_SITE,
                    )
                }
            }
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
