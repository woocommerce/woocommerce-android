package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlin.reflect.KClass

sealed class WooPosAnalytics : IAnalyticsEvent {
    override val siteless: Boolean = false

    private val _properties: MutableMap<String, String> = mutableMapOf()
    val properties: Map<String, String> get() = _properties.toMap()

    fun addProperties(additionalProperties: Map<String, String>) {
        _properties.putAll(additionalProperties)
    }

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
