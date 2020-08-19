package com.woocommerce.android.helpers

import androidx.test.platform.app.InstrumentationRegistry
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.di.AppComponentTest
import com.woocommerce.android.di.DaggerAppComponentTest
import com.woocommerce.android.mocks.AndroidNotifier
import org.junit.Before
import org.junit.Rule

open class TestBase {
    protected lateinit var appContext: WooCommerce
    protected lateinit var mockedAppComponent: AppComponentTest

    companion object {
        val wireMockPort = 8080
    }

    @Before
    open fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as WooCommerce

        mockedAppComponent = DaggerAppComponentTest.builder()
                .application(appContext)
                .build()
    }

    @get:Rule
    var wireMockRule: WireMockRule = WireMockRule(options().port(wireMockPort)
        .usingFilesUnderDirectory("/WooCommerce/src/androidTest/kotlin/com/woocommerce/android/mocks")
        .notifier(AndroidNotifier()))
}
