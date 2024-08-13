package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlin.reflect.KClass

sealed class WooPosAnalyticsEvent : IAnalyticsEvent {
    override val siteless: Boolean = false
    override val isPosEvent: Boolean = true

    private val _properties: MutableMap<String, String> = mutableMapOf()
    val properties: Map<String, String> get() = _properties.toMap()

    fun addProperties(additionalProperties: Map<String, String>) {
        _properties.putAll(additionalProperties)
    }

    sealed class Error : WooPosAnalyticsEvent() {
        abstract val errorContext: KClass<Any>
        abstract val errorType: String?
        abstract val errorDescription: String?

        data class Test(
            override val errorContext: KClass<Any>,
            override val errorType: String?,
            override val errorDescription: String?,
        ) : Error() {
            override val name: String = "WOO_POS_TEST_ERROR"
        }
    }

    sealed class Event : WooPosAnalyticsEvent() {
        data object Test : Event() {
            override val name: String = "WOO_POS_TEST_EVENT"
        }
    }
}

internal fun IAnalyticsEvent.addProperties(additionalProperties: Map<String, String>) {
    when (this) {
        is WooPosAnalyticsEvent -> addProperties(additionalProperties)
        else -> error("Cannot add properties to non-WooPosAnalytics event")
    }
}
