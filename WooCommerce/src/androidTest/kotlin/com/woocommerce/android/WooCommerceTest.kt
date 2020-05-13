package com.woocommerce.android

import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import com.woocommerce.android.di.DaggerAppComponentTest

open class WooCommerceTest : WooCommerce() {
    override val component: AppComponent by lazy {
        // The mocks interfere with the screenshots recording, which right
        // now uses a live account. Use this parameter to disable them.
        val shouldDisableMocks = InstrumentationRegistry.getArguments().get("disableMocks") == "true"

        if (shouldDisableMocks) {
            DaggerAppComponent.builder()
                    .application(this)
                    .build()
        } else {
            DaggerAppComponentTest.builder()
                    .application(this)
                    .build()
        }
    }
}
