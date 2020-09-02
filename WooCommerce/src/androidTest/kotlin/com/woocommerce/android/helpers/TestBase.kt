package com.woocommerce.android.helpers

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.di.AndroidNotifier
import com.woocommerce.android.di.AppComponentTest
import com.woocommerce.android.di.AssetFileSource
import com.woocommerce.android.di.DaggerAppComponentTest
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
        appContext = getInstrumentation().targetContext.applicationContext as WooCommerce

        mockedAppComponent = DaggerAppComponentTest.builder()
                .application(appContext)
                .build()
    }

    @Rule @JvmField
    val wireMockRule = WireMockRule(options().port(wireMockPort)
        .fileSource(AssetFileSource(getInstrumentation().context.assets))
        .extensions(ResponseTemplateTransformer(true))
        .notifier(AndroidNotifier())
    )
}
