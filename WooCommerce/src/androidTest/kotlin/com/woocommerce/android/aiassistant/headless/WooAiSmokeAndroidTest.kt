package com.woocommerce.android.aiassistant.headless

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.e2e.helpers.InitializationRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WooAiSmokeAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @Test
    fun runWooAiSmoke() = runBlocking {
        hiltRule.inject()
        val arguments = InstrumentationRegistry.getArguments().keySet().associateWith { key ->
            InstrumentationRegistry.getArguments().getString(key)
        }

        assumeTrue(WooAiSmokeConfig.fromInstrumentationArguments(arguments).enabled)

        val exit = WooAiSmokeDebugBridge.run(
            application = ApplicationProvider.getApplicationContext(),
            instrumentationArguments = arguments,
        )

        if (exit.failureMessage != null) {
            throw AssertionError(exit.failureMessage)
        }
    }
}
