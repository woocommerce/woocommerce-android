package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlin.reflect.KClass

sealed class WooPosAnalytics : IAnalyticsEvent {
    override val siteless: Boolean = false

    val properties: Map<String, *> = emptyMap<String, String>()

    sealed class Error : WooPosAnalytics() {
        abstract val errorContext: KClass<Any>
        abstract val errorType: String?
        abstract val errorDescription: String?

        data class Test(
            override val name: String = "WOO_POS_TEST_ERROR",
            override val errorContext: KClass<Any>,
            override val errorType: String?,
            override val errorDescription: String?,
        ) : Error()
    }

    sealed class Event : WooPosAnalytics() {
        data class Test(
            override val name: String = "WOO_POS_TEST_EVENT",
        ) : Event()
    }
}
