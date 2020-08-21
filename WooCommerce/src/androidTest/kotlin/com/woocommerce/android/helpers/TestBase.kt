package com.woocommerce.android.helpers

import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.di.AppComponentTest
import com.woocommerce.android.di.DaggerAppComponentTest
import org.junit.Before

open class TestBase {
    protected lateinit var appContext: WooCommerce
    protected lateinit var mockedAppComponent: AppComponentTest

    @Before
    open fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as WooCommerce

        mockedAppComponent = DaggerAppComponentTest.builder()
                .application(appContext)
                .build()
    }
}
